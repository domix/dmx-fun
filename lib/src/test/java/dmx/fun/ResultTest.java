package dmx.fun;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultTest {

    // ---------- factories ----------

    @Test
    void ok_shouldThrowNPE_ifValueIsNull() {
        assertThatThrownBy(() -> Result.ok(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("null");
    }

    @Test
    void err_shouldThrowNPE_ifErrorIsNull() {
        assertThatThrownBy(() -> Result.err(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("null");
    }

    @Test
    void of_shouldInferRightTypes() {
        Result<String, RuntimeException> boom = Result.err(new RuntimeException("boom"));
        Result<BigDecimal, String> ok = Result.ok(BigDecimal.ONE);

        assertThat(boom.isError()).isTrue();
        assertThat(boom.isOk()).isFalse();
        assertThat(ok.isOk()).isTrue();
        assertThat(ok.isError()).isFalse();
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
    void getOrElseGet_shouldThrowNPE_ifSupplierIsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").getOrElseGet((Supplier<String>) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getOrElseGet_shouldThrowNPE_ifSupplierReturnsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").getOrElseGet(() -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("supplier returned null");
    }

    @Test
    void getOrElseGet_shouldBeLazyOnOk() {
        AtomicBoolean called = new AtomicBoolean(false);
        String value = Result.ok("real").getOrElseGet(() -> {
            called.set(true);
            return "fallback";
        });
        assertThat(value).isEqualTo("real");
        assertThat(called.get()).as("supplier must not be invoked for Ok").isFalse();
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
        assertThat(secondCalled.get()).as("mapper must not be called for Err").isFalse();
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
    void filter_withValue_shouldReturnOk_whenPredicatePasses() {
        Result<String, String> filtered = Result.<String, String>ok("hello world!")
            .filter(s -> s.length() > 10, "too short");
        assertThat(filtered.get()).isEqualTo("hello world!");
    }

    @Test
    void filter_withFunction_shouldReturnOk_whenPredicatePasses() {
        Result<String, String> filtered = Result.<String, String>ok("hello world!")
            .filter(s -> s.length() > 10, s -> "value '" + s + "' is too short");
        assertThat(filtered.get()).isEqualTo("hello world!");
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
        assertThat(okTouched.get()).isTrue();
        assertThat(errTouched.get()).isFalse();

        okTouched.set(false);
        errTouched.set(false);

        Result.err("e").match(_ -> okTouched.set(true), _ -> errTouched.set(true));
        assertThat(okTouched.get()).isFalse();
        assertThat(errTouched.get()).isTrue();
    }

    // ---------- peek / peekError ----------

    @Test
    void peek_shouldRunOnOkOnly_andReturnSelf() {
        AtomicBoolean touched = new AtomicBoolean(false);
        Result<String, String> ok = Result.ok("v");
        Result<String, String> returned = ok.peek(_ -> touched.set(true));

        assertThat(touched.get()).isTrue();
        assertThat(returned).isSameAs(ok);

        touched.set(false);
        Result.<String, String>err("e").peek(_ -> touched.set(true));
        assertThat(touched.get()).isFalse();
    }

    @Test
    void peekError_shouldRunOnErrOnly_andReturnSelf() {
        AtomicBoolean touched = new AtomicBoolean(false);
        Result<String, String> err = Result.err("e");
        Result<String, String> returned = err.peekError(_ -> touched.set(true));

        assertThat(touched.get()).isTrue();
        assertThat(returned).isSameAs(err);

        touched.set(false);
        Result.<String, String>ok("v").peekError(_ -> touched.set(true));
        assertThat(touched.get()).isFalse();
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

    // ---------- recover ----------

    @Test
    void recover_shouldConvertErrToOk_usingErrorValue() {
        Result<String, String> recovered = Result.<String, String>err("oops")
            .recover(e -> "recovered:" + e);
        assertThat(recovered.isOk()).isTrue();
        assertThat(recovered.get()).isEqualTo("recovered:oops");
    }

    @Test
    void recover_shouldLeaveOkUntouched() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result<String, String> ok = Result.ok("value");
        Result<String, String> result = ok.recover(e -> {
            called.set(true);
            return "should-not-appear";
        });
        assertThat(result).isSameAs(ok);
        assertThat(called.get()).as("rescue must not be called for Ok").isFalse();
    }

    @Test
    void recover_shouldThrowNPE_ifRescueIsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").recover(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void recover_shouldThrowNPE_ifRescueReturnsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").recover(e -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("rescue returned null");
    }

    // ---------- recoverWith ----------

    @Test
    void recoverWith_shouldConvertErrToOk_viaMappedResult() {
        Result<String, Integer> result = Result.<String, String>err("missing")
            .recoverWith(e -> Result.ok("default"));
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo("default");
    }

    @Test
    void recoverWith_shouldAllowErrToRemainErr_withNewErrorType() {
        Result<String, Integer> result = Result.<String, String>err("oops")
            .recoverWith(e -> Result.err(e.length()));
        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).isEqualTo(4);
    }

    @Test
    void recoverWith_shouldLeaveOkUntouched_andChangeErrorType() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result<String, Integer> result = Result.<String, String>ok("hello")
            .recoverWith(e -> {
                called.set(true);
                return Result.err(0);
            });
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
        assertThat(called.get()).as("rescue must not be called for Ok").isFalse();
    }

    @Test
    void recoverWith_shouldThrowNPE_ifRescueIsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").recoverWith(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void recoverWith_shouldThrowNPE_ifRescueReturnsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").recoverWith(e -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("rescue returned null");
    }

    // ---------- orElse ----------

    @Test
    void orElse_eager_shouldReturnSelf_whenOk() {
        Result<String, String> ok = Result.ok("primary");
        Result<String, String> result = ok.orElse(Result.ok("fallback"));
        assertThat(result).isSameAs(ok);
    }

    @Test
    void orElse_eager_shouldReturnAlternative_whenErr() {
        Result<String, String> result = Result.<String, String>err("e")
            .orElse(Result.ok("fallback"));
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo("fallback");
    }

    @Test
    void orElse_eager_shouldChainMultipleAlternatives() {
        Result<String, String> result = Result.<String, String>err("e1")
            .orElse(Result.err("e2"))
            .orElse(Result.ok("found"));
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo("found");
    }

    @Test
    void orElse_eager_shouldThrowNPE_ifAlternativeIsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").orElse((Result<String, String>) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void orElse_lazy_shouldReturnSelf_whenOk_andNotCallSupplier() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result<String, String> ok = Result.ok("primary");
        Result<String, String> result = ok.orElse(() -> {
            called.set(true);
            return Result.ok("fallback");
        });
        assertThat(result).isSameAs(ok);
        assertThat(called.get()).as("supplier must not be called for Ok").isFalse();
    }

    @Test
    void orElse_lazy_shouldReturnAlternative_whenErr() {
        Result<String, String> result = Result.<String, String>err("e")
            .orElse(() -> Result.ok("fallback"));
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo("fallback");
    }

    @Test
    void orElse_lazy_shouldThrowNPE_ifSupplierIsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").orElse((Supplier<Result<String, String>>) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void orElse_lazy_shouldThrowNPE_ifSupplierReturnsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").orElse(() -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("alternative returned null");
    }

    // ---------- or ----------

    @Test
    void or_shouldReturnSelf_whenOk() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result<String, String> ok = Result.ok("primary");
        Result<String, String> result = ok.or(() -> {
            called.set(true);
            return Result.ok("fallback");
        });
        assertThat(result).isSameAs(ok);
        assertThat(called.get()).as("fallback supplier must not be called for Ok").isFalse();
    }

    @Test
    void or_shouldReturnFallback_whenErr() {
        Result<String, String> result = Result.<String, String>err("e")
            .or(() -> Result.ok("fallback"));
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo("fallback");
    }

    @Test
    void or_shouldChain_multipleAlternatives() {
        Result<String, String> result = Result.<String, String>err("e1")
            .or(() -> Result.err("e2"))
            .or(() -> Result.ok("found"));
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo("found");
    }

    @Test
    void or_shouldThrowNPE_ifFallbackIsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").or(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void or_shouldThrowNPE_ifFallbackReturnsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").or(() -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("fallback returned null");
    }

    // ---------- flatMapError ----------

    @Test
    void flatMapError_shouldChainErrValues() {
        Result<String, Integer> result = Result.<String, String>err("oops")
            .flatMapError(e -> Result.err(e.length()));
        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).isEqualTo(4);
    }

    @Test
    void flatMapError_shouldAllowConvertingErrToOk() {
        Result<String, Integer> result = Result.<String, String>err("fallback-value")
            .flatMapError(e -> Result.ok(e.toUpperCase()));
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo("FALLBACK-VALUE");
    }

    @Test
    void flatMapError_shouldLeaveOkUntouched_andChangeErrorType() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result<String, Integer> result = Result.<String, String>ok("hello")
            .flatMapError(e -> {
                called.set(true);
                return Result.err(0);
            });
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
        assertThat(called.get()).as("mapper must not be called for Ok").isFalse();
    }

    @Test
    void flatMapError_shouldThrowNPE_ifMapperIsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").flatMapError(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void flatMapError_shouldThrowNPE_ifMapperReturnsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").flatMapError(e -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper returned null");
    }

    // ---------- swap ----------

    @Test
    void swap_shouldFlipOkToErr() {
        // Ok<String, Integer> → swap → Err<Integer, String>
        Result<Integer, String> swapped = Result.<String, Integer>ok("hello").swap();
        assertThat(swapped.isError()).isTrue();
        assertThat(swapped.getError()).isEqualTo("hello");
    }

    @Test
    void swap_shouldFlipErrToOk() {
        // Err<Integer, String> → swap → Ok<String, Integer>
        Result<String, Integer> swapped = Result.<Integer, String>err("error").swap();
        assertThat(swapped.isOk()).isTrue();
        assertThat(swapped.get()).isEqualTo("error");
    }

    @Test
    void swap_appliedTwice_shouldReturnEquivalentResult() {
        Result<String, Integer> original = Result.ok("hello");
        Result<String, Integer> roundTrip = original.swap().swap();
        assertThat(roundTrip.isOk()).isTrue();
        assertThat(roundTrip.get()).isEqualTo("hello");
    }

    // ---------- getOrElseGetWithError ----------

    @Test
    void getOrElseGetWithError_shouldReturnValueOnOk() {
        AtomicBoolean called = new AtomicBoolean(false);
        String value = Result.<String, String>ok("real").getOrElseGetWithError(e -> {
            called.set(true);
            return "from-error:" + e;
        });
        assertThat(value).isEqualTo("real");
        assertThat(called.get()).as("mapper must not be invoked for Ok").isFalse();
    }

    @Test
    void getOrElseGetWithError_shouldMapErrorOnErr() {
        String value = Result.<String, String>err("boom").getOrElseGetWithError(e -> "from-error:" + e);
        assertThat(value).isEqualTo("from-error:boom");
    }

    @Test
    void getOrElseGetWithError_shouldThrowNPE_ifMapperIsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").getOrElseGetWithError(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getOrElseGetWithError_shouldThrowNPE_ifMapperReturnsNull() {
        assertThatThrownBy(() -> Result.<String, String>err("e").getOrElseGetWithError(e -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("errorMapper returned null");
    }

    // ---------- stream() ----------

    @Test
    void stream_shouldReturnSingleElementStream_forOk() {
        assertThat(Result.<Integer, String>ok(42).stream()).containsExactly(42);
    }

    @Test
    void stream_shouldReturnEmptyStream_forErr() {
        assertThat(Result.<Integer, String>err("oops").stream()).isEmpty();
    }

    @Test
    void stream_canFlatMapToKeepOnlyOkValues() {
        List<Integer> values = Stream.of(Result.ok(1), Result.<Integer, String>err("x"), Result.ok(3))
            .flatMap(Result::stream)
            .toList();
        assertThat(values).containsExactly(1, 3);
    }

    // ---------- toList() ----------

    @Test
    void toList_allOk_returnsOkList() {
        Result<List<Integer>, String> r =
            Stream.<Result<Integer, String>>of(Result.ok(1), Result.ok(2), Result.ok(3))
                  .collect(Result.toList());
        assertThat(r.isOk()).isTrue();
        assertThat(r.get()).containsExactly(1, 2, 3);
    }

    @Test
    void toList_firstErrIsReturned() {
        // A Collector cannot short-circuit: all stream elements are fed to the
        // accumulator before the finisher runs. The counter confirms all three
        // elements are consumed even though only the first Err matters.
        AtomicInteger counter = new AtomicInteger(0);
        Result<List<Integer>, String> r =
            Stream.<Result<Integer, String>>of(Result.ok(1), Result.err("boom"), Result.ok(3))
                  .peek(__ -> counter.incrementAndGet())
                  .collect(Result.toList());
        assertThat(r.isError()).isTrue();
        assertThat(r.getError()).isEqualTo("boom");
        assertThat(counter.get()).isEqualTo(3); // all elements processed; no stream short-circuit
    }

    @Test
    void toList_emptyStream_returnsOkEmptyList() {
        Result<List<Integer>, String> r =
            Stream.<Result<Integer, String>>of().collect(Result.toList());
        assertThat(r.isOk()).isTrue();
        assertThat(r.get()).isEmpty();
    }

    @Test
    void toList_resultListIsUnmodifiable() {
        Result<List<Integer>, String> r =
            Stream.<Result<Integer, String>>of(Result.ok(1)).collect(Result.toList());
        assertThatThrownBy(() -> r.get().add(99))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // ---------- partitioningBy() ----------

    @Test
    void partitioningBy_separatesOksAndErrors() {
        Result.Partition<Integer, String> p =
            Stream.<Result<Integer, String>>of(Result.ok(1), Result.err("a"), Result.ok(3), Result.err("b"))
                  .collect(Result.partitioningBy());
        assertThat(p.oks()).containsExactly(1, 3);
        assertThat(p.errors()).containsExactly("a", "b");
    }

    @Test
    void partitioningBy_allOk_emptyErrors() {
        Result.Partition<Integer, String> p =
            Stream.<Result<Integer, String>>of(Result.ok(1), Result.ok(2))
                  .collect(Result.partitioningBy());
        assertThat(p.oks()).containsExactly(1, 2);
        assertThat(p.errors()).isEmpty();
    }

    @Test
    void partitioningBy_allErr_emptyOks() {
        Result.Partition<Integer, String> p =
            Stream.<Result<Integer, String>>of(Result.err("x"), Result.err("y"))
                  .collect(Result.partitioningBy());
        assertThat(p.oks()).isEmpty();
        assertThat(p.errors()).containsExactly("x", "y");
    }

    @Test
    void partitioningBy_listsAreUnmodifiable() {
        Result.Partition<Integer, String> p =
            Stream.<Result<Integer, String>>of(Result.ok(1), Result.err("e"))
                  .collect(Result.partitioningBy());
        assertThatThrownBy(() -> p.oks().add(99))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> p.errors().add("x"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void partition_constructor_defensivelyCopiesAndEnforcesImmutability() {
        ArrayList<Integer> srcOks = new ArrayList<>(List.of(1, 2));
        ArrayList<String> srcErrors = new ArrayList<>(List.of("a"));

        Result.Partition<Integer, String> p = new Result.Partition<>(srcOks, srcErrors);

        // mutating the source lists must not affect the partition
        srcOks.add(99);
        srcErrors.add("z");
        assertThat(p.oks()).containsExactly(1, 2);
        assertThat(p.errors()).containsExactly("a");

        // the lists exposed by the partition must be unmodifiable
        assertThatThrownBy(() -> p.oks().add(99))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> p.errors().add("z"))
            .isInstanceOf(UnsupportedOperationException.class);

        // null arguments must be rejected
        assertThatThrownBy(() -> new Result.Partition<>(null, srcErrors))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Result.Partition<>(srcOks, null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // groupingBy
    // -------------------------------------------------------------------------

    @Test
    void groupingBy_shouldGroupElementsByKey() {
        Map<Integer, NonEmptyList<String>> result = Stream.of("a", "bb", "cc", "ddd")
            .collect(Result.groupingBy(String::length));
        assertThat(result).containsOnlyKeys(1, 2, 3);
        assertThat(result.get(1).toList()).containsExactly("a");
        assertThat(result.get(2).toList()).containsExactly("bb", "cc");
        assertThat(result.get(3).toList()).containsExactly("ddd");
    }

    @Test
    void groupingBy_shouldReturnEmptyMap_forEmptyStream() {
        Map<Integer, NonEmptyList<String>> result = Stream.<String>empty()
            .collect(Result.groupingBy(String::length));
        assertThat(result).isEmpty();
    }

    @Test
    void groupingBy_resultShouldBeUnmodifiable() {
        Map<Integer, NonEmptyList<String>> result = Stream.of("a")
            .collect(Result.groupingBy(String::length));
        assertThatThrownBy(() -> result.put(99, NonEmptyList.singleton("x")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void groupingBy_shouldThrowNPE_whenClassifierIsNull() {
        assertThatThrownBy(() -> Stream.of("a").collect(Result.groupingBy(null)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("classifier");
    }

    @Test
    void groupingBy_shouldThrowNPE_whenStreamElementIsNull() {
        Stream<String> s = Stream.of("a", null);
        assertThatThrownBy(() -> s.collect(Result.groupingBy(String::length)))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void groupingBy_downstream_shouldApplyFunctionToEachGroup() {
        Map<Integer, Long> result = Stream.of("a", "bb", "cc", "ddd")
            .collect(Result.groupingBy(String::length, nel -> (long) nel.size()));
        assertThat(result).containsEntry(1, 1L).containsEntry(2, 2L).containsEntry(3, 1L);
    }

    @Test
    void groupingBy_downstream_shouldThrowNPE_whenDownstreamIsNull() {
        assertThatThrownBy(() -> Stream.of("a").collect(Result.groupingBy(String::length, null)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("downstream");
    }
}
