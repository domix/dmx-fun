package codes.domix.fun;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    // ---------- factories ----------

    @Test
    void of_shouldInferRightTypes() {
        Result<String, RuntimeException> boom = Result.err(new RuntimeException("boom"));
        Result<BigDecimal, String> ok = Result.ok(BigDecimal.ONE);

        assertTrue(boom.isError());
        assertFalse(boom.isOk());
        assertTrue(ok.isOk());
        assertFalse(ok.isError());
    }

    // ---------- get / getError ----------

    @Test
    void get_onOk_shouldReturnValue() {
        assertThat(Result.ok("hello").get()).isEqualTo("hello");
    }

    @Test
    void get_onErr_shouldThrow() {
        assertThatThrownBy(() -> Result.err("oops").get())
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Error");
    }

    @Test
    void getError_onErr_shouldReturnError() {
        assertThat(Result.<String, String>err("oops").getError()).isEqualTo("oops");
    }

    @Test
    void getError_onOk_shouldThrow() {
        assertThatThrownBy(() -> Result.ok("hello").getError())
            .isInstanceOf(NoSuchElementException.class);
    }

    // ---------- getOrElse / getOrElseGet / getOrNull / getOrThrow ----------

    @Test
    void getOrElse_shouldReturnValueOnOk_andFallbackOnErr() {
        assertThat(Result.ok("real").getOrElse("fallback")).isEqualTo("real");
        assertThat(Result.<String, String>err("e").getOrElse("fallback")).isEqualTo("fallback");
    }

    @Test
    void getOrElseGet_shouldBeLazyOnOk() {
        AtomicBoolean called = new AtomicBoolean(false);
        String value = Result.ok("real").getOrElseGet(() -> {
            called.set(true);
            return "fallback";
        });
        assertThat(value).isEqualTo("real");
        assertFalse(called.get(), "supplier must not be invoked for Ok");
    }

    @Test
    void getOrNull_shouldReturnNullOnErr() {
        assertThat(Result.err("e").getOrNull()).isNull();
        assertThat(Result.ok("v").getOrNull()).isEqualTo("v");
    }

    @Test
    void getOrThrow_shouldReturnValueOnOk_andThrowMappedExceptionOnErr() {
        assertThat(Result.ok("v").getOrThrow(e -> new IllegalStateException(e.toString())))
            .isEqualTo("v");
        assertThatThrownBy(() ->
            Result.<String, String>err("boom").getOrThrow(IllegalStateException::new))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");
    }

    // ---------- map / mapError ----------

    @Test
    void map_shouldTransformOkValue_andPropagateErr() {
        assertThat(Result.ok("hello").map(String::length).get()).isEqualTo(5);
        assertThat(Result.<String, String>err("e").map(String::length).isError()).isTrue();
    }

    @Test
    void mapError_shouldTransformErrValue_andPropagateOk() {
        assertThat(Result.<String, String>err("oops").mapError(String::length).getError()).isEqualTo(4);
        Result<String, String> ok = Result.ok("v");
        assertThat(ok.mapError(String::length).get()).isEqualTo("v");
    }

    // ---------- flatMap ----------

    @Test
    void flatMap_shouldChainOkValues() {
        Result<Integer, String> result = Result.<String, String>ok("hello")
            .flatMap(s -> Result.ok(s.length()))
            .flatMap(n -> Result.ok(n * 2));
        assertThat(result.get()).isEqualTo(10);
    }

    @Test
    void flatMap_shouldShortCircuitOnErr() {
        AtomicBoolean secondCalled = new AtomicBoolean(false);
        Result<Integer, String> result = Result.<String, String>err("stop")
            .flatMap(s -> {
                secondCalled.set(true);
                return Result.ok(s.length());
            });
        assertThat(result.isError()).isTrue();
        assertFalse(secondCalled.get(), "mapper must not be called for Err");
    }

    // ---------- filter ----------

    @Test
    void filter_withValue_shouldReturnErrWhenPredicateFails() {
        Result<String, String> filtered = Result.<String, String>ok("hi")
            .filter(s -> s.length() > 10, "too short");
        assertThat(filtered.getError()).isEqualTo("too short");
    }

    @Test
    void filter_withFunction_shouldPassValueToErrorMapper() {
        Result<String, String> filtered = Result.<String, String>ok("hi")
            .filter(s -> s.length() > 10, s -> "value '" + s + "' is too short");
        assertThat(filtered.getError()).isEqualTo("value 'hi' is too short");
    }

    @Test
    void filter_shouldLeaveErrUntouched() {
        Result<String, String> err = Result.err("original");
        assertThat(err.filter(s -> false, "new error")).isSameAs(err);
    }

    // ---------- fold / match ----------

    @Test
    void fold_shouldApplyCorrectBranch() {
        String fromOk  = Result.<String, String>ok("v").fold(s -> "ok:" + s, e -> "err:" + e);
        String fromErr = Result.<String, String>err("e").fold(s -> "ok:" + s, e -> "err:" + e);
        assertThat(fromOk).isEqualTo("ok:v");
        assertThat(fromErr).isEqualTo("err:e");
    }

    @Test
    void match_shouldExecuteCorrectConsumer() {
        AtomicBoolean okTouched  = new AtomicBoolean(false);
        AtomicBoolean errTouched = new AtomicBoolean(false);

        Result.ok("v").match(_ -> okTouched.set(true), _ -> errTouched.set(true));
        assertTrue(okTouched.get());
        assertFalse(errTouched.get());

        okTouched.set(false);
        errTouched.set(false);

        Result.err("e").match(_ -> okTouched.set(true), _ -> errTouched.set(true));
        assertFalse(okTouched.get());
        assertTrue(errTouched.get());
    }

    // ---------- peek / peekError ----------

    @Test
    void peek_shouldRunOnOkOnly_andReturnSelf() {
        AtomicBoolean touched = new AtomicBoolean(false);
        Result<String, String> ok = Result.ok("v");
        Result<String, String> returned = ok.peek(_ -> touched.set(true));

        assertTrue(touched.get());
        assertThat(returned).isSameAs(ok);

        touched.set(false);
        Result.<String, String>err("e").peek(_ -> touched.set(true));
        assertFalse(touched.get());
    }

    @Test
    void peekError_shouldRunOnErrOnly_andReturnSelf() {
        AtomicBoolean touched = new AtomicBoolean(false);
        Result<String, String> err = Result.err("e");
        Result<String, String> returned = err.peekError(_ -> touched.set(true));

        assertTrue(touched.get());
        assertThat(returned).isSameAs(err);

        touched.set(false);
        Result.<String, String>ok("v").peekError(_ -> touched.set(true));
        assertFalse(touched.get());
    }

    // ---------- interop: toOption / toTry ----------

    @Test
    void toOption_shouldBeSome_forOk_andNone_forErr() {
        assertThat(Result.ok("v").toOption().isDefined()).isTrue();
        assertThat(Result.err("e").toOption().isEmpty()).isTrue();
    }

    @Test
    void toTry_shouldConvertOkToSuccess_andErrToFailure() {
        Try<String> success = Result.<String, String>ok("v")
            .toTry(IllegalArgumentException::new);
        assertThat(success.isSuccess()).isTrue();
        assertThat(success.get()).isEqualTo("v");

        Try<String> failure = Result.<String, String>err("boom")
            .toTry(IllegalArgumentException::new);
        assertThat(failure.isFailure()).isTrue();
        assertThat(failure.getCause()).isInstanceOf(IllegalArgumentException.class)
                                      .hasMessage("boom");
    }

    // ---------- interop: fromOption / fromTry ----------

    @Test
    void fromOption_shouldWrapSome_andUseErrorForNone() {
        assertThat(Result.fromOption(Option.some("v"), "missing").get()).isEqualTo("v");
        assertThat(Result.fromOption(Option.none(), "missing").getError()).isEqualTo("missing");
    }

    @Test
    void fromTry_shouldConvertSuccessToOk_andFailureToErr() {
        Result<String, Throwable> ok = Result.fromTry(Try.success("v"));
        assertThat(ok.isOk()).isTrue();
        assertThat(ok.get()).isEqualTo("v");

        RuntimeException ex = new RuntimeException("boom");
        Result<String, Throwable> err = Result.fromTry(Try.failure(ex));
        assertThat(err.isError()).isTrue();
        assertThat(err.getError()).isSameAs(ex);
    }
}
