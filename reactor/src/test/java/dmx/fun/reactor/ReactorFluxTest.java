package dmx.fun.reactor;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactorFluxTest {

    private static final IllegalStateException BOOM = new IllegalStateException("boom");

    // ── sequence ───────────────────────────────────────────────────────────────

    @Test
    void sequence_allOk_isOkList() {
        Flux<Result<Integer, String>> src = Flux.just(Result.ok(1), Result.ok(2), Result.ok(3));
        StepVerifier.create(ReactorFlux.sequence(src))
            .assertNext(r -> assertThat(r).isOk().containsValue(List.of(1, 2, 3)))
            .verifyComplete();
    }

    @Test
    void sequence_empty_isOkEmptyList() {
        StepVerifier.create(ReactorFlux.sequence(Flux.<Result<Integer, String>>empty()))
            .assertNext(r -> assertThat(r).isOk().containsValue(List.of()))
            .verifyComplete();
    }

    @Test
    void sequence_firstErr_shortCircuitsAndCancelsUpstream() {
        AtomicInteger tailSeen = new AtomicInteger();
        Flux<Result<Integer, String>> src = Flux.concat(
            Flux.just(Result.ok(1), Result.ok(2), Result.err("bad")),
            Flux.just(Result.<Integer, String>ok(99)).doOnNext(x -> tailSeen.incrementAndGet()));

        StepVerifier.create(ReactorFlux.sequence(src))
            .assertNext(r -> assertThat(r).isErr().containsError("bad"))
            .verifyComplete();

        // Elements after the first Err are never requested.
        org.assertj.core.api.Assertions.assertThat(tailSeen).hasValue(0);
    }

    @Test
    void sequence_sourceError_propagates() {
        StepVerifier.create(ReactorFlux.sequence(Flux.error(BOOM)))
            .verifyError(IllegalStateException.class);
    }

    // ── collectResult ──────────────────────────────────────────────────────────

    @Test
    void collectResult_values_isOkList() {
        StepVerifier.create(ReactorFlux.collectResult(Flux.just("a", "b")))
            .assertNext(r -> assertThat(r).isOk().containsValue(List.of("a", "b")))
            .verifyComplete();
    }

    @Test
    void collectResult_empty_isOkEmptyList() {
        StepVerifier.create(ReactorFlux.collectResult(Flux.<String>empty()))
            .assertNext(r -> assertThat(r).isOk().containsValue(List.of()))
            .verifyComplete();
    }

    @Test
    void collectResult_sourceError_isErrWithCause() {
        StepVerifier.create(ReactorFlux.collectResult(Flux.<String>error(BOOM)))
            .assertNext(r -> assertThat(r).isErr().containsError(BOOM))
            .verifyComplete();
    }

    @Test
    void collectResult_withMapper_mapsError() {
        StepVerifier.create(ReactorFlux.collectResult(Flux.<String>error(BOOM), Throwable::getMessage))
            .assertNext(r -> assertThat(r).isErr().containsError("boom"))
            .verifyComplete();
    }

    // ── collectValidated ───────────────────────────────────────────────────────

    @Test
    void collectValidated_allOk_isValidList() {
        Flux<Result<String, String>> src = Flux.just(Result.ok("a"), Result.ok("b"));
        StepVerifier.create(ReactorFlux.collectValidated(src))
            .assertNext(v -> assertThat(v).isValid().containsValue(List.of("a", "b")))
            .verifyComplete();
    }

    @Test
    void collectValidated_mixed_accumulatesAllErrors() {
        Flux<Result<String, String>> src =
            Flux.just(Result.ok("a"), Result.err("e1"), Result.ok("b"), Result.err("e2"));
        StepVerifier.create(ReactorFlux.collectValidated(src))
            .assertNext(v -> {
                assertThat(v).isInvalid();
                org.assertj.core.api.Assertions.assertThat(
                        ((Validated.Invalid<NonEmptyList<String>, List<String>>) v).error().toList())
                    .containsExactly("e1", "e2");
            })
            .verifyComplete();
    }

    // ── flattenOption ──────────────────────────────────────────────────────────

    @Test
    void flattenOption_dropsNone_keepsOrder() {
        Flux<Option<Integer>> src =
            Flux.just(Option.some(1), Option.none(), Option.some(2), Option.none(), Option.some(3));
        StepVerifier.create(ReactorFlux.flattenOption(src))
            .expectNext(1, 2, 3)
            .verifyComplete();
    }

    // ── toFlux ─────────────────────────────────────────────────────────────────

    @Test
    void toFlux_nonEmptyList_emitsAllInOrder() {
        StepVerifier.create(ReactorFlux.toFlux(NonEmptyList.of(1, List.of(2, 3))))
            .expectNext(1, 2, 3)
            .verifyComplete();
    }

    @Test
    void toFlux_some_emitsOne() {
        StepVerifier.create(ReactorFlux.toFlux(Option.some("x")))
            .expectNext("x")
            .verifyComplete();
    }

    @Test
    void toFlux_none_isEmpty() {
        StepVerifier.create(ReactorFlux.toFlux(Option.<String>none()))
            .verifyComplete();
    }

    @Test
    void toFlux_success_emitsOne() {
        StepVerifier.create(ReactorFlux.toFlux(Try.success("x")))
            .expectNext("x")
            .verifyComplete();
    }

    @Test
    void toFlux_failure_errors() {
        StepVerifier.create(ReactorFlux.toFlux(Try.<String>failure(BOOM)))
            .verifyErrorMatches(t -> t == BOOM);
    }

    @Test
    void toFlux_okThrowableError_emitsOne() {
        StepVerifier.create(ReactorFlux.toFlux(Result.<String, IllegalStateException>ok("x")))
            .expectNext("x")
            .verifyComplete();
    }

    @Test
    void toFlux_errThrowableError_errors() {
        StepVerifier.create(ReactorFlux.toFlux(Result.<String, IllegalStateException>err(BOOM)))
            .verifyErrorMatches(t -> t == BOOM);
    }

    @Test
    void toFlux_errWithMapper_errorsWithMapped() {
        StepVerifier.create(ReactorFlux.toFlux(Result.<String, String>err("bad"), IllegalStateException::new))
            .verifyError(IllegalStateException.class);
    }

    @Test
    void toFlux_errMapperReturnsNull_flowsAsOnError() {
        StepVerifier.create(ReactorFlux.toFlux(Result.<String, String>err("bad"), e -> null))
            .verifyError(NullPointerException.class);
    }

    // ── backpressure ───────────────────────────────────────────────────────────

    @Test
    void toFlux_honorsBackpressure() {
        StepVerifier.create(ReactorFlux.toFlux(NonEmptyList.of(1, List.of(2, 3))), 0)
            .thenRequest(1).expectNext(1)
            .thenRequest(2).expectNext(2, 3)
            .verifyComplete();
    }

    // ── null guards ────────────────────────────────────────────────────────────

    @Test
    void sequence_null_throws() {
        assertThatThrownBy(() -> ReactorFlux.sequence(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("results");
    }

    @Test
    void toFlux_nullResult_throws() {
        assertThatThrownBy(() -> ReactorFlux.toFlux((Result<String, IllegalStateException>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("result");
    }
}
