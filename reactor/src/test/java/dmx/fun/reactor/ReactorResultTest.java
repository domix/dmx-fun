package dmx.fun.reactor;

import dmx.fun.Result;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactorResultTest {

    private static Mono<Result<Integer, String>> ok(int v) {
        return Mono.just(Result.ok(v));
    }

    private static Mono<Result<Integer, String>> err(String e) {
        return Mono.just(Result.err(e));
    }

    // ── mapOk ──────────────────────────────────────────────────────────────────

    @Test
    void mapOk_onOk_transformsValue() {
        StepVerifier.create(ReactorResult.mapOk(ok(2), v -> v * 10))
            .assertNext(r -> assertThat(r).isOk().containsValue(20))
            .verifyComplete();
    }

    @Test
    void mapOk_onErr_passesThrough() {
        StepVerifier.create(ReactorResult.mapOk(err("bad"), v -> v * 10))
            .assertNext(r -> assertThat(r).isErr().containsError("bad"))
            .verifyComplete();
    }

    // ── flatMapOk ──────────────────────────────────────────────────────────────

    @Test
    void flatMapOk_onOk_chainsNextOk() {
        StepVerifier.create(ReactorResult.flatMapOk(ok(2), v -> Mono.just(Result.ok(v + 1))))
            .assertNext(r -> assertThat(r).isOk().containsValue(3))
            .verifyComplete();
    }

    @Test
    void flatMapOk_onOk_chainsNextErr() {
        StepVerifier.create(ReactorResult.flatMapOk(ok(2), v -> Mono.just(Result.err("downstream"))))
            .assertNext(r -> assertThat(r).isErr().containsError("downstream"))
            .verifyComplete();
    }

    @Test
    void flatMapOk_onErr_passesThroughWithoutInvokingMapper() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        StepVerifier.create(ReactorResult.flatMapOk(err("bad"), v -> {
                invoked.set(true);
                return Mono.just(Result.ok(v));
            }))
            .assertNext(r -> assertThat(r).isErr().containsError("bad"))
            .verifyComplete();
        org.assertj.core.api.Assertions.assertThat(invoked).isFalse();
    }

    // ── mapErr ─────────────────────────────────────────────────────────────────

    @Test
    void mapErr_onErr_transformsError() {
        StepVerifier.create(ReactorResult.mapErr(err("bad"), String::length))
            .assertNext(r -> assertThat(r).isErr().containsError(3))
            .verifyComplete();
    }

    @Test
    void mapErr_onOk_passesThrough() {
        StepVerifier.create(ReactorResult.mapErr(ok(7), String::length))
            .assertNext(r -> assertThat(r).isOk().containsValue(7))
            .verifyComplete();
    }

    // ── recover ────────────────────────────────────────────────────────────────

    @Test
    void recover_onErr_becomesOk() {
        StepVerifier.create(ReactorResult.recover(err("bad"), e -> e.length()))
            .assertNext(r -> assertThat(r).isOk().containsValue(3))
            .verifyComplete();
    }

    @Test
    void recover_onOk_unchanged() {
        StepVerifier.create(ReactorResult.recover(ok(7), e -> -1))
            .assertNext(r -> assertThat(r).isOk().containsValue(7))
            .verifyComplete();
    }

    // ── composition ────────────────────────────────────────────────────────────

    @Test
    void chain_composesOnTheResultTrack() {
        Mono<Result<Integer, String>> pipeline =
            ReactorResult.mapErr(
                ReactorResult.flatMapOk(
                    ReactorResult.mapOk(ok(2), v -> v + 1),       // Ok(3)
                    v -> Mono.just(Result.ok(v * 10))),           // Ok(30)
                e -> "mapped:" + e);                              // no-op on Ok
        StepVerifier.create(pipeline)
            .assertNext(r -> assertThat(r).isOk().containsValue(30))
            .verifyComplete();
    }

    // ── null guards ────────────────────────────────────────────────────────────

    @Test
    void mapOk_nullMono_throws() {
        assertThatThrownBy(() -> ReactorResult.mapOk(null, v -> v))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mono");
    }

    @Test
    void flatMapOk_nullMapper_throws() {
        assertThatThrownBy(() -> ReactorResult.flatMapOk(ok(1), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }
}
