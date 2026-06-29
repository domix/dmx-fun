package dmx.fun.reactor;

import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
        StepVerifier.create(ReactorFun.toMonoResult(Mono.<String>empty()))
            .assertNext(r -> {
                assertThat(r).isErr();
                org.assertj.core.api.Assertions
                    .assertThat(((Result.Err<String, Throwable>) r).error())
                    .isInstanceOf(NoSuchElementException.class);
            })
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
        AtomicBoolean upstreamCancelled = new AtomicBoolean(false);
        StepVerifier.create(
                ReactorFun.toMonoTry(Mono.never().doOnCancel(() -> upstreamCancelled.set(true))))
            .expectSubscription()
            .thenCancel()
            .verify();
        org.assertj.core.api.Assertions.assertThat(upstreamCancelled).isTrue();
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

    @Test
    void toMono_err_mapperThrows_flowsAsOnError() {
        // The mapper runs at subscription, so its exception is an onError signal,
        // not something thrown during assembly of the Mono.
        Mono<String> mono = ReactorFun.toMono(
            Result.<String, String>err("bad"),
            e -> { throw new IllegalStateException("mapper-boom"); });
        StepVerifier.create(mono)
            .verifyErrorMatches(t -> t instanceof IllegalStateException
                && "mapper-boom".equals(t.getMessage()));
    }

    @Test
    void toMono_err_mapperReturnsNull_flowsAsOnError() {
        Mono<String> mono = ReactorFun.toMono(Result.<String, String>err("bad"), e -> null);
        StepVerifier.create(mono).verifyError(NullPointerException.class);
    }

    // ── null guards ────────────────────────────────────────────────────────────

    static Stream<Arguments> nullGuards() {
        Mono<String> ok = Mono.just("x");
        Function<Throwable, String> errorMapper = Throwable::getMessage;
        Function<String, Throwable> errorToThrowable = IllegalStateException::new;
        Supplier<String> onEmpty = () -> "empty";
        return Stream.of(
            arguments("toMonoTry(null)", (Executable) () -> ReactorFun.toMonoTry(null), "mono"),
            arguments("toMonoResult(null)", (Executable) () -> ReactorFun.toMonoResult(null), "mono"),
            arguments("toMonoResult(null, errorMapper)", (Executable) () -> ReactorFun.toMonoResult(null, errorMapper), "mono"),
            arguments("toMonoResult(ok, null)", (Executable) () -> ReactorFun.toMonoResult(ok, null), "errorMapper"),
            arguments("toMonoResult(null, errorMapper, onEmpty)", (Executable) () -> ReactorFun.toMonoResult(null, errorMapper, onEmpty), "mono"),
            arguments("toMonoResult(ok, null, onEmpty)", (Executable) () -> ReactorFun.toMonoResult(ok, null, onEmpty), "errorMapper"),
            arguments("toMonoResult(ok, errorMapper, null)", (Executable) () -> ReactorFun.toMonoResult(ok, errorMapper, null), "onEmpty"),
            arguments("toMonoOption(null)", (Executable) () -> ReactorFun.toMonoOption(null), "mono"),
            arguments("toTry(null)", (Executable) () -> ReactorFun.toTry(null), "mono"),
            arguments("toResult(null)", (Executable) () -> ReactorFun.toResult(null), "mono"),
            arguments("toResult(ok, null)", (Executable) () -> ReactorFun.toResult(ok, null), "errorMapper"),
            arguments("toResult(ok, errorMapper, null)", (Executable) () -> ReactorFun.toResult(ok, errorMapper, null), "onEmpty"),
            arguments("toOption(null)", (Executable) () -> ReactorFun.toOption(null), "mono"),
            arguments("toMono((Option) null)", (Executable) () -> ReactorFun.toMono((Option<String>) null), "option"),
            arguments("toMono((Try) null)", (Executable) () -> ReactorFun.toMono((Try<String>) null), "aTry"),
            arguments("toMono((Result) null)", (Executable) () -> ReactorFun.toMono((Result<String, IllegalStateException>) null), "result"),
            arguments("toMono((Result) null, errorMapper)", (Executable) () -> ReactorFun.toMono((Result<String, String>) null, errorToThrowable), "result"),
            arguments("toMono(ok-result, null)", (Executable) () -> ReactorFun.toMono(Result.<String, String>ok("x"), null), "errorMapper")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullGuards")
    void publicAdapters_rejectNull(String name, Executable call, String parameter) {
        assertThatThrownBy(call::execute)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining(parameter);
    }
}
