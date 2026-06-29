package dmx.fun.reactor;

import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import java.io.IOException;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactorFunTest {

    private static final IllegalStateException BOOM = new IllegalStateException("boom");

    // ── toMonoTry ──────────────────────────────────────────────────────────────

    @Test
    void toMonoTry_value_isSuccess() {
        StepVerifier.create(ReactorFun.toMonoTry(Mono.just("x")))
            .assertNext(t -> assertThat(t).isSuccess().containsValue("x"))
            .verifyComplete();
    }

    @Test
    void toMonoTry_empty_isFailureWithNoSuchElement() {
        StepVerifier.create(ReactorFun.toMonoTry(Mono.empty()))
            .assertNext(t -> assertThat(t).isFailure().failsWith(NoSuchElementException.class))
            .verifyComplete();
    }

    @Test
    void toMonoTry_error_isFailureWithCause() {
        StepVerifier.create(ReactorFun.toMonoTry(Mono.error(BOOM)))
            .assertNext(t -> assertThat(t).isFailure().failsWith(IllegalStateException.class))
            .verifyComplete();
    }

    // ── toMonoResult ───────────────────────────────────────────────────────────

    @Test
    void toMonoResult_value_isOk() {
        StepVerifier.create(ReactorFun.toMonoResult(Mono.just("x")))
            .assertNext(r -> assertThat(r).isOk().containsValue("x"))
            .verifyComplete();
    }

    @Test
    void toMonoResult_empty_isErrWithNoSuchElement() {
        StepVerifier.create(ReactorFun.toMonoResult(Mono.empty()))
            .assertNext(r -> assertThat(r).isErr())
            .verifyComplete();
    }

    @Test
    void toMonoResult_error_isErrWithCause() {
        StepVerifier.create(ReactorFun.toMonoResult(Mono.error(BOOM)))
            .assertNext(r -> assertThat(r).isErr().containsError(BOOM))
            .verifyComplete();
    }

    @Test
    void toMonoResult_withMapper_mapsErrorChannel() {
        StepVerifier.create(ReactorFun.toMonoResult(Mono.<String>error(BOOM), Throwable::getMessage))
            .assertNext(r -> assertThat(r).isErr().containsError("boom"))
            .verifyComplete();
    }

    @Test
    void toMonoResult_withMapper_mapsEmptyThroughNoSuchElement() {
        StepVerifier.create(ReactorFun.toMonoResult(Mono.<String>empty(), Throwable::getClass))
            .assertNext(r -> assertThat(r).isErr().containsError(NoSuchElementException.class))
            .verifyComplete();
    }

    @Test
    void toMonoResult_withCustomEmptyMapper_usesOnEmpty() {
        StepVerifier.create(
                ReactorFun.toMonoResult(Mono.<String>empty(), Throwable::getMessage, () -> "was-empty"))
            .assertNext(r -> assertThat(r).isErr().containsError("was-empty"))
            .verifyComplete();
    }

    @Test
    void toMonoResult_withCustomEmptyMapper_stillMapsError() {
        StepVerifier.create(
                ReactorFun.toMonoResult(Mono.<String>error(BOOM), Throwable::getMessage, () -> "was-empty"))
            .assertNext(r -> assertThat(r).isErr().containsError("boom"))
            .verifyComplete();
    }

    @Test
    void toMonoTry_checkedException_isFailureWithCause() {
        IOException io = new IOException("disk");
        StepVerifier.create(ReactorFun.toMonoTry(Mono.error(io)))
            .assertNext(t -> assertThat(t).isFailure().failsWith(IOException.class))
            .verifyComplete();
    }

    @Test
    void toMonoResult_checkedException_isErrWithCause() {
        IOException io = new IOException("disk");
        StepVerifier.create(ReactorFun.toMonoResult(Mono.error(io)))
            .assertNext(r -> assertThat(r).isErr().containsError(io))
            .verifyComplete();
    }

    // ── toMonoOption ───────────────────────────────────────────────────────────

    @Test
    void toMonoOption_value_isSome() {
        StepVerifier.create(ReactorFun.toMonoOption(Mono.just("x")))
            .assertNext(o -> assertThat(o).isSome().containsValue("x"))
            .verifyComplete();
    }

    @Test
    void toMonoOption_empty_isNone() {
        StepVerifier.create(ReactorFun.toMonoOption(Mono.empty()))
            .assertNext(o -> assertThat(o).isNone())
            .verifyComplete();
    }

    @Test
    void toMonoOption_error_propagates() {
        StepVerifier.create(ReactorFun.toMonoOption(Mono.error(BOOM)))
            .verifyError(IllegalStateException.class);
    }

    // ── cancellation ───────────────────────────────────────────────────────────

    @Test
    void toMonoTry_honorsCancellation() {
        StepVerifier.create(ReactorFun.toMonoTry(Mono.never()))
            .expectSubscription()
            .thenCancel()
            .verify();
    }

    // ── blocking extractors ────────────────────────────────────────────────────

    @Test
    void toTry_value_isSuccess() {
        assertThat(ReactorFun.toTry(Mono.just("x"))).isSuccess().containsValue("x");
    }

    @Test
    void toResult_empty_isErr() {
        assertThat(ReactorFun.toResult(Mono.<String>empty())).isErr();
    }

    @Test
    void toResult_withMapper_mapsError() {
        assertThat(ReactorFun.toResult(Mono.<String>error(BOOM), Throwable::getMessage))
            .isErr().containsError("boom");
    }

    @Test
    void toOption_value_isSome() {
        assertThat(ReactorFun.toOption(Mono.just("x"))).isSome().containsValue("x");
    }

    @Test
    void toOption_empty_isNone() {
        assertThat(ReactorFun.toOption(Mono.<String>empty())).isNone();
    }

    @Test
    void toOption_error_rethrows() {
        assertThatThrownBy(() -> ReactorFun.toOption(Mono.<String>error(BOOM)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");
    }

    // ── dmx-fun -> Mono ────────────────────────────────────────────────────────

    @Test
    void toMono_some_emitsValue() {
        StepVerifier.create(ReactorFun.toMono(Option.some("x")))
            .expectNext("x")
            .verifyComplete();
    }

    @Test
    void toMono_none_isEmpty() {
        StepVerifier.create(ReactorFun.toMono(Option.<String>none()))
            .verifyComplete();
    }

    @Test
    void toMono_success_emitsValue() {
        StepVerifier.create(ReactorFun.toMono(Try.success("x")))
            .expectNext("x")
            .verifyComplete();
    }

    @Test
    void toMono_failure_errors() {
        StepVerifier.create(ReactorFun.toMono(Try.<String>failure(BOOM)))
            .verifyErrorMatches(t -> t == BOOM);
    }

    @Test
    void toMono_ok_emitsValue() {
        StepVerifier.create(ReactorFun.toMono(Result.<String, String>ok("x"), IllegalStateException::new))
            .expectNext("x")
            .verifyComplete();
    }

    @Test
    void toMono_okThrowableError_emitsValue() {
        StepVerifier.create(ReactorFun.toMono(Result.<String, IllegalStateException>ok("x")))
            .expectNext("x")
            .verifyComplete();
    }

    @Test
    void toMono_errThrowableError_errorsWithError() {
        StepVerifier.create(ReactorFun.toMono(Result.<String, IllegalStateException>err(BOOM)))
            .verifyErrorMatches(t -> t == BOOM);
    }

    @Test
    void toMono_err_errorsWithMappedThrowable() {
        StepVerifier.create(ReactorFun.toMono(Result.<String, String>err("bad"), IllegalStateException::new))
            .verifyError(IllegalStateException.class);
    }

    // ── null guards ────────────────────────────────────────────────────────────

    @Test
    void toMonoTry_null_throws() {
        assertThatThrownBy(() -> ReactorFun.toMonoTry(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mono");
    }

    @Test
    void toMono_nullOption_throws() {
        assertThatThrownBy(() -> ReactorFun.toMono((Option<String>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("option");
    }
}
