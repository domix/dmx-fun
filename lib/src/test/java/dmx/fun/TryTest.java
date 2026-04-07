package dmx.fun;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TryTest {

    /**
     * Verifies success case returns value and has no cause
     */
    @Test
    void of_shouldReturnSuccessWhenComputationDoesNotThrow() {
        var t = Try.of(() -> 42);

        assertThat(t.isSuccess()).isTrue();
        assertThat(t.isFailure()).isFalse();
        assertThat(t.get()).isEqualTo(42);
        assertThatThrownBy(t::getCause).isInstanceOf(NoSuchElementException.class);
    }

    /**
     * Verifies failure case returns exception and metadata
     */
    @Test
    void of_shouldReturnFailureWhenComputationThrows() {
        var t = Try.of(() -> {
            throw new IOException("boom");
        });

        assertThat(t.isFailure()).isTrue();
        assertThat(t.isSuccess()).isFalse();
        var cause = t.getCause();
        assertThat(cause).isInstanceOf(IOException.class);
        assertThat(cause.getMessage()).isEqualTo("boom");
        assertThatThrownBy(t::get).isInstanceOf(NoSuchElementException.class);
    }

    /**
     * Verifies side effect executes and returns success
     */
    @Test
    void run_shouldReturnSuccessOnVoidSideEffect() {
        var executed = new AtomicBoolean(false);

        var t = Try.run(() -> executed.set(true));

        assertThat(t.isSuccess()).isTrue();
        assertThat(executed.get()).isTrue();
    }

    @Test
    void run_shouldReturnFailureWhenSideEffectThrows() {
        var t = Try.run(() -> {
            throw new IllegalStateException("side-effect failed");
        });

        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isInstanceOf(IllegalStateException.class);
    }

    /**
     * Maps successful result to transformed value
     */
    @Test
    void map_shouldTransformValueOnSuccess() {
        var t = Try.of(() -> 10);

        var mapped = t.map(v -> "value:" + v);

        assertThat(mapped.isSuccess()).isTrue();
        assertThat(mapped.get()).isEqualTo("value:10");
    }

    /**
     * Maps failure case; preserves original exception
     */
    @Test
    void map_shouldMapFailure() {
        var t = Try.failure(new RuntimeException("boom"));
        var mapped = t.map(v -> "value:" + v);

        assertThat(mapped.isFailure()).isTrue();
        assertThat(mapped.getCause())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");
    }

    /**
     * Maps success to failure when mapper throws an exception
     */
    @Test
    void map_shouldConvertToFailureWhenMapperThrows() {
        var t = Try.of(() -> 10);

        var mapped = t.map(_ -> {
            throw new RuntimeException("mapper failed");
        });

        assertThat(mapped.isFailure()).isTrue();
        assertThat(mapped.getCause().getMessage()).isEqualTo("mapper failed");
    }

    /**
     * Chains successful values using flatMap
     */
    @Test
    void flatMap_shouldChainSuccessValues() {
        var t = Try.of(() -> 2);

        var result = t
            .flatMap(v -> Try.of(() -> v * 10))
            .flatMap(v -> Try.success("result:" + v));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("result:20");
    }

    /**
     * Verifies failure propagates through flatMap
     */
    @Test
    void flatMap_shouldPropagateFailure() {
        var t = Try.of(() -> 2);

        var result = t.flatMap(_ -> Try.failure(new IllegalArgumentException("bad")));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(result.getCause().getMessage()).isEqualTo("bad");
    }

    /**
     * `flatMap` ignores mapped failure; propagates initial failure
     */
    @Test
    void flatMap_shouldIgnoreMappedFailure() {
        var t = Try.failure(new IllegalArgumentException("bad"));

        var result = t.flatMap(_ -> Try.failure(new IllegalArgumentException("BAD")));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("bad");
    }

    /**
     * `onSuccess` runs action only on success; no failure action
     */
    @Test
    void onSuccess_shouldRunActionOnlyOnSuccess() {
        var successCalled = new AtomicBoolean(false);
        var failureCalled = new AtomicBoolean(false);

        var success = Try.success(42);

        success
            .onSuccess(_ -> successCalled.set(true))
            .onFailure(_ -> failureCalled.set(true));

        assertThat(successCalled.get()).isTrue();
        assertThat(failureCalled.get()).isFalse();
    }

    /**
     * Tests action execution only on failure
     */
    @Test
    void onFailure_shouldRunActionOnlyOnFailure() {
        var successCalled = new AtomicBoolean(false);
        var failureCalled = new AtomicBoolean(false);

        var failure = Try.failure(new RuntimeException("err"));

        failure
            .onSuccess(_ -> successCalled.set(true))
            .onFailure(_ -> failureCalled.set(true));

        assertThat(successCalled.get()).isFalse();
        assertThat(failureCalled.get()).isTrue();
    }

    /**
     * `recoverWith` uses alternative `Try` on failure
     */
    @Test
    void recover_shouldReturnSameSuccess() {
        var success = Try.success(10);

        var recovered = success.recover(_ -> 99);

        assertThat(recovered).isSameAs(success);
        assertThat(recovered.get()).isEqualTo(10);
    }

    /**
     * `recover` produces success from failure via recovery function
     */
    @Test
    void recover_shouldProduceNewSuccessFromFailure() {
        var failure = Try.<Integer>failure(new RuntimeException("boom"));

        var recovered = failure.recover(_ -> 99);

        assertThat(recovered.isSuccess()).isTrue();
        assertThat(recovered.get()).isEqualTo(99);
    }

    @Test
    void recoverWith_shouldReturnSameSuccess() {
        var success = Try.success(10);

        var recovered = success.recoverWith(_ -> Try.success(99));

        assertThat(recovered).isSameAs(success);
        assertThat(recovered.get()).isEqualTo(10);
    }

    /**
     * `recoverWith` uses alternative `Try` on failure
     */
    @Test
    void recoverWith_shouldUseAlternativeTryOnFailure() {
        var failure = Try.<Integer>failure(new RuntimeException("boom"));

        var recovered = failure.recoverWith(_ -> Try.success(123));

        assertThat(recovered.isSuccess()).isTrue();
        assertThat(recovered.get()).isEqualTo(123);
    }

    /**
     * Demonstrates `getOrElse` returns value or fallback
     */
    @Test
    void getOrElse_shouldReturnValueOnSuccessOrFallbackOnFailure() {
        var success = Try.success(5);
        var failure = Try.failure(new RuntimeException("err"));

        assertThat(success.getOrElse(99)).isEqualTo(5);
        assertThat(failure.getOrElse(99)).isEqualTo(99);
    }

    /**
     * Verifies supplier is used only on failure
     */
    @Test
    void getOrElseGet_shouldUseSupplierOnlyOnFailure() {
        var success = Try.success(5);
        var failure = Try.<Integer>failure(new RuntimeException("err"));

        var supplierCalled = new AtomicBoolean(false);

        int value1 = success.getOrElseGet(() -> {
            supplierCalled.set(true);
            return 99;
        });
        assertThat(supplierCalled.get()).as("Supplier should not be called on success").isFalse();
        int value2 = failure.getOrElseGet(() -> {
            supplierCalled.set(true);
            return 100;
        });

        assertThat(value1).isEqualTo(5);
        assertThat(value2).isEqualTo(100);
        assertThat(supplierCalled.get()).isTrue();
    }

    /**
     * Verifies null result for failure case
     */
    @Test
    void getOrNull_shouldReturnNullOnFailure() {
        var success = Try.success(5);
        var failure = Try.failure(new RuntimeException("err"));

        assertThat(success.getOrNull()).isEqualTo(5);
        assertThat(failure.getOrNull()).isNull();
    }

    @Test
    void getOrThrow_withoutMapper_shouldThrowOriginalExceptionIfException() {
        var failure = Try.of(() -> {
            throw new IOException("io boom");
        });

        assertThatThrownBy(failure::getOrThrow)
            .isInstanceOf(IOException.class)
            .hasMessage("io boom");
    }

    /**
     * Verifies exception mapping during `getOrThrow` call
     */
    @Test
    void getOrThrow_withMapper_shouldThrowMappedRuntimeException() {
        var failure = Try.<Integer>failure(new IllegalStateException("illegal"));

        assertThatThrownBy(() -> failure.getOrThrow(cause -> new RuntimeException("wrapped: " + cause.getMessage())))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("wrapped: illegal");
    }

    /**
     * Demonstrates `Try` fold operation on success and failure
     */
    @Test
    void fold_shouldApplyOnSuccessOrOnFailure() {
        var success = Try.success(10);
        var failure = Try.failure(new RuntimeException("err"));

        var foldedSuccess = success.fold(
            v -> "value:" + v,
            t -> "error:" + t.getMessage()
        );

        var foldedFailure = failure.fold(
            v -> "value:" + v,
            t -> "error:" + t.getMessage()
        );

        assertThat(foldedSuccess).isEqualTo("value:10");
        assertThat(foldedFailure).isEqualTo("error:err");
    }

    // --- Basic monadic laws for Try ---

    @Test
    void monad_leftIdentity() {
        var a = 42;
        Function<Integer, Try<String>> f = x -> Try.success("value:" + x);

        var left = Try.of(() -> a).flatMap(f);
        var right = f.apply(a);

        assertThat(left).isEqualTo(right);
    }

    @Test
    void monad_rightIdentity() {
        var m = Try.success(7);

        var left = m.flatMap(Try::success);
        var right = m;

        assertThat(left).isEqualTo(right);
    }

    /**
     * Verifies monad associativity law holds for Try
     */
    @Test
    void monad_associativity() {
        var m = Try.success(3);

        Function<Integer, Try<Integer>> f = x -> Try.success(x + 1);
        Function<Integer, Try<Integer>> g = x -> Try.success(x * 2);

        var left = m.flatMap(f).flatMap(g);
        Try<Integer> right = m.flatMap(x -> f.apply(x).flatMap(g));

        assertThat(left).isEqualTo(right);
    }

    // --- Integration with Result<Value, Throwable> ---

    @Test
    void toResult_shouldConvertSuccessToOk() {
        Try<String> t = Try.success("ok");

        Result<String, Throwable> result = t.toResult();

        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo("ok");
    }

    /**
     * Converts failure to error with a message preserved
     */
    @Test
    void toResult_shouldConvertFailureToErr() {
        Try<String> t = Try.failure(new RuntimeException("err"));

        Result<String, Throwable> result = t.toResult();

        assertThat(result.isError()).isTrue();
        assertThat(result.getError().getMessage()).isEqualTo("err");
    }

    @Test
    void fromResult_shouldConvertOkToSuccess() {
        Result<String, Throwable> result = Result.ok("hello");

        Try<String> t = Try.fromResult(result);

        assertThat(t.isSuccess()).isTrue();
        assertThat(t.get()).isEqualTo("hello");
    }

    /**
     * Converts a failure result to a failed try
     */
    @Test
    void fromResult_shouldConvertErrToFailure() {
        RuntimeException ex = new RuntimeException("boom");
        Result<String, Throwable> result = Result.err(ex);

        Try<String> t = Try.fromResult(result);

        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isSameAs(ex);
    }

    // --------------------------------------------------------
    //           weird cases / additional edge cases
    // --------------------------------------------------------

    @Test
    void of_shouldCaptureErrorNotOnlyException() {
        AssertionError error = new AssertionError("hard error");

        Try<Integer> t = Try.of(() -> {
            throw error;
        });

        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isSameAs(error);
    }

    @Test
    void run_shouldCaptureErrorNotOnlyException() {
        AssertionError error = new AssertionError("boom");

        Try<Void> t = Try.run(() -> {
            throw error;
        });

        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isSameAs(error);
    }

    /**
     * Verifies `getOrThrow` wraps error in `RuntimeException`
     */
    @Test
    void getOrThrow_withoutMapper_shouldWrapErrorInRuntimeException() {
        AssertionError error = new AssertionError("assert failed");
        Try<Integer> t = Try.failure(error);

        assertThatThrownBy(t::getOrThrow)
            .isInstanceOf(RuntimeException.class)
            .hasCause(error);
    }

    /**
     * Verifies `getOrThrow` returns success value
     */
    @Test
    void getOrThrow_shouldReturnTheSuccessValue() throws Exception {
        var aTry = Try.success(1);
        var result = aTry
            .getOrThrow();
        assertThat(result).isEqualTo(1);

        var result2 = aTry
            .getOrThrow(cause -> new RuntimeException("wrapped: " + cause.getMessage()));

        assertThat(result2).isEqualTo(1);
    }

    /**
     * Verifies `map` turns to failure when mapper throws
     */
    @Test
    void map_shouldTurnToFailureWhenMapperThrowsCheckedException() {
        Try<Integer> t = Try.success(10);

        Try<String> mapped = t.map(v -> {
            try {
                throw new IOException("checked");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(mapped.isFailure()).isTrue();
        assertThat(mapped.getCause()).isInstanceOf(RuntimeException.class);
        assertThat(mapped.getCause().getCause()).isInstanceOf(IOException.class);
        assertThat(mapped.getCause().getMessage()).contains("checked");
    }

    /**
     * Maps success; asserts mapper throws error
     */
    @Test
    void map_shouldTurnToFailureWhenMapperThrowsError() {
        Try<Integer> t = Try.success(10);
        AssertionError error = new AssertionError("mapper error");

        Try<String> mapped = t.map(v -> {
            throw error;
        });

        assertThat(mapped.isFailure()).isTrue();
        assertThat(mapped.getCause()).isSameAs(error);
    }

    /**
     * Flatmaps success; asserts mapper throws checked exception
     */
    @Test
    void flatMap_shouldTurnToFailureWhenMapperThrowsCheckedException() {
        Try<Integer> t = Try.success(10);

        Try<String> mapped = t.flatMap(v -> {
            try {
                throw new IOException("flat checked");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(mapped.isFailure()).isTrue();
        assertThat(mapped.getCause())
            .isInstanceOf(RuntimeException.class);
        assertThat(mapped.getCause().getCause())
            .isInstanceOf(IOException.class);
        assertThat(mapped.getCause().getMessage()).contains("flat checked");
    }

    /**
     * Verifies flatMap yields failure on mapper error
     */
    @Test
    void flatMap_shouldTurnToFailureWhenMapperThrowsError() {
        Try<Integer> t = Try.success(10);
        AssertionError error = new AssertionError("flat error");

        Try<String> mapped = t.flatMap(v -> {
            throw error;
        });

        assertThat(mapped.isFailure()).isTrue();
        assertThat(mapped.getCause()).isSameAs(error);
    }

    /**
     * Verifies recover yields failure when function throws
     */
    @Test
    void recover_shouldReturnFailureIfRecoverFunctionThrows() {
        Try<Integer> failure = Try.failure(new RuntimeException("original"));

        Try<Integer> recovered = failure.recover(cause -> {
            try {
                throw new IOException("recover failed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(recovered.isFailure()).isTrue();
        assertThat(recovered.getCause())
            .isInstanceOf(RuntimeException.class);
        assertThat(recovered.getCause().getCause())
            .isInstanceOf(IOException.class);
        assertThat(recovered.getCause().getMessage())
            .contains("recover failed");
    }

    /**
     * Verifies `recoverWith` returns failure if function throws
     */
    @Test
    void recoverWith_shouldReturnFailureIfRecoverFunctionThrows() {
        Try<Integer> failure = Try.failure(new RuntimeException("original"));

        Try<Integer> recovered = failure.recoverWith(cause -> {
            try {
                throw new IOException("recoverWith failed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(recovered.isFailure()).isTrue();
        assertThat(recovered.getCause())
            .isInstanceOf(RuntimeException.class);
        assertThat(recovered.getCause().getCause())
            .isInstanceOf(IOException.class);
        assertThat(recovered.getCause().getMessage())
            .contains("recoverWith failed");
    }

    /**
     * Verifies recoverWith throws error when recover function throws
     */
    @Test
    void recoverWith_shouldReturnFailureWithNPE_ifRecoverFnReturnsNull() {
        Try<Integer> t = Try.<Integer>failure(new RuntimeException("original"))
            .recoverWith(_ -> null);

        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void recoverWith_shouldReturnFailureIfRecoverFunctionThrowsError() {
        Try<Integer> failure = Try.failure(new RuntimeException("original"));
        AssertionError error = new AssertionError("recoverWith error");

        Try<Integer> recovered = failure.recoverWith(cause -> {
            throw error;
        });

        assertThat(recovered.isFailure()).isTrue();
        assertThat(recovered.getCause()).isSameAs(error);
    }

    /**
     * Verifies `toResult` preserves exact throwable instance
     */
    @Test
    void toResult_withErrorShouldKeepExactThrowableInstance() {
        AssertionError error = new AssertionError("critical");
        Try<Integer> t = Try.failure(error);

        Result<Integer, Throwable> result = t.toResult();

        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).isSameAs(error);
    }

    /**
     * Creates failure from a result with the same throwable instance
     */
    @Test
    void fromResult_withNonExceptionThrowableShouldCreateFailureWithSameInstance() {
        AssertionError error = new AssertionError("assert!");
        Result<Integer, Throwable> result = Result.err(error);

        Try<Integer> t = Try.fromResult(result);

        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isSameAs(error);
    }

    // ---------- null-contract validations ----------

    @Test
    void recover_shouldThrowNPE_ifRecoverFnIsNull() {
        assertThatThrownBy(() -> Try.success(1).recover(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void recoverWith_shouldThrowNPE_ifRecoverFnIsNull() {
        assertThatThrownBy(() -> Try.success(1).recoverWith(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getOrElseGet_shouldThrowNPE_ifFallbackSupplierIsNull() {
        assertThatThrownBy(() -> Try.success(1).getOrElseGet(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fromResult_shouldThrowNPE_ifResultIsNull() {
        assertThatThrownBy(() -> Try.fromResult(null))
            .isInstanceOf(NullPointerException.class);
    }

    // ---------- toResult(Function) ----------

    @Test
    void toResult_withMapper_shouldConvertSuccessToOk() {
        Try<Integer> t = Try.success(42);
        Result<Integer, String> result = t.toResult(Throwable::getMessage);

        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo(42);
    }

    @Test
    void toResult_withMapper_shouldConvertFailureToErrUsingMapper() {
        Try<Integer> t = Try.failure(new RuntimeException("boom"));
        Result<Integer, String> result = t.toResult(Throwable::getMessage);

        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).isEqualTo("boom");
    }

    @Test
    void toResult_withMapper_shouldThrowNPE_ifMapperIsNull() {
        assertThatThrownBy(() -> Try.success(1).toResult(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toResult_withMapper_shouldThrowNPE_ifMapperReturnsNull() {
        Try<Integer> t = Try.failure(new RuntimeException("boom"));
        assertThatThrownBy(() -> t.toResult(_ -> null))
            .isInstanceOf(NullPointerException.class);
    }

    // ---------- mapFailure ----------

    @Test
    void mapFailure_shouldTransformCause_onFailure() {
        RuntimeException original = new RuntimeException("low-level");
        Try<Integer> t = Try.<Integer>failure(original)
            .mapFailure(e -> new IllegalStateException("domain error", e));

        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(t.getCause().getMessage()).isEqualTo("domain error");
        assertThat(t.getCause().getCause()).isSameAs(original);
    }

    @Test
    void mapFailure_shouldNotAffectSuccess() {
        Try<Integer> success = Try.success(10);
        Try<Integer> result = success.mapFailure(e -> new RuntimeException("should not run"));

        assertThat(result).isSameAs(success);
        assertThat(result.get()).isEqualTo(10);
    }

    @Test
    void mapFailure_shouldThrowNPE_ifMapperIsNull() {
        assertThatThrownBy(() -> Try.success(1).mapFailure(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void mapFailure_shouldReturnFailureWithNPE_ifMapperReturnsNull() {
        // mapFailure wraps exceptions from the mapper (consistent with map() semantics)
        Try<Integer> t = Try.<Integer>failure(new RuntimeException()).mapFailure(_ -> null);
        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void mapFailure_shouldWrapException_ifMapperThrows() {
        RuntimeException mapperBoom = new RuntimeException("mapper exploded");
        Try<Integer> t = Try.<Integer>failure(new RuntimeException("original"))
            .mapFailure(_ -> { throw mapperBoom; });

        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isSameAs(mapperBoom);
    }

    // ---------- stream ----------

    @Test
    void stream_shouldReturnSingleElementStream_forSuccess() {
        List<Integer> values = Try.success(42).stream().toList();
        assertThat(values).containsExactly(42);
    }

    @Test
    void stream_shouldReturnEmptyStream_forFailure() {
        List<Integer> values = Try.<Integer>failure(new RuntimeException()).stream().toList();
        assertThat(values).isEmpty();
    }

    @Test
    void stream_shouldReturnSingleElementStreamContainingNull_forSuccessFromRun() {
        // Try.run() produces Success(null); stream() must return exactly one element (null),
        // not an empty stream — distinguishing it from Failure
        Try<Void> t = Try.run(() -> {});
        assertThat(t.stream().count()).isEqualTo(1L);
    }

    @Test
    void stream_canBeUsedToFlattenStreamOfTries() {
        List<Integer> result = Stream.<Try<Integer>>of(
            Try.success(1),
            Try.failure(new RuntimeException()),
            Try.success(3)
        ).flatMap(Try::stream).toList();

        assertThat(result).containsExactly(1, 3);
    }

    // ---------- filter(Predicate) — default overload ----------

    @Test
    void filter_default_shouldReturnFailureWithIAE_whenPredicateFails() {
        Try<String> result = Try.success("hello").filter(s -> s.length() >= 10);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void filter_default_shouldReturnSuccess_whenPredicatePasses() {
        Try<String> result = Try.success("hello").filter(s -> s.equals("hello"));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
    }

    @Test
    void filter_default_shouldNotAffectFailure() {
        RuntimeException original = new RuntimeException("Boom!");
        Try<String> result = Try.<String>failure(original).filter(s -> true);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(original);
    }

    @Test
    void filter_default_shouldCapturePredicateException_asFailure() {
        Try<String> result = Try.success("hello").filter(s -> {
            throw new RuntimeException("predicate failed");
        });
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("predicate failed");
    }

    // ---------- filter(Predicate, Supplier) ----------

    @Test
    void filter_withSupplier_shouldReturnFailure_withCustomException_whenPredicateFails() {
        Try<String> result = Try.success("hello")
            .filter(s -> s.length() >= 10, () -> new RuntimeException("boom!"));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom!");
    }

    @Test
    void filter_withSupplier_shouldReturnSuccess_andNotInvokeSupplier_whenPredicatePasses() {
        AtomicBoolean called = new AtomicBoolean(false);
        Try<String> result = Try.success("hello")
            .filter(s -> s.equals("hello"), () -> {
                called.set(true);
                return new RuntimeException("should not run");
            });
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
        assertThat(called.get()).as("exception supplier must not be called when predicate passes").isFalse();
    }

    // ---------- filter(Predicate, Function) ----------

    @Test
    void filter_withFunction_shouldReturnSuccess_whenPredicateHolds() {
        Try<Integer> t = Try.success(10)
            .filter(n -> n > 0, n -> new IllegalArgumentException("non-positive: " + n));

        assertThat(t.isSuccess()).isTrue();
        assertThat(t.get()).isEqualTo(10);
    }

    @Test
    void filter_withFunction_shouldReturnFailure_withContextualMessage_whenPredicateFails() {
        Try<Integer> t = Try.success(-5)
            .filter(n -> n > 0, n -> new IllegalArgumentException("non-positive: " + n));

        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(t.getCause().getMessage()).isEqualTo("non-positive: -5");
    }

    @Test
    void filter_withFunction_shouldNotAffectFailure() {
        RuntimeException original = new RuntimeException("original");
        Try<Integer> t = Try.<Integer>failure(original)
            .filter(n -> n > 0, n -> new IllegalArgumentException("should not run"));

        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isSameAs(original);
    }

    @Test
    void filter_withFunction_shouldThrowNPE_ifPredicateIsNull() {
        assertThatThrownBy(() -> Try.success(1).filter(null, n -> new RuntimeException()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void filter_withFunction_shouldThrowNPE_ifErrorFnIsNull() {
        assertThatThrownBy(() -> Try.success(1).filter(n -> true, (Function<Integer, Throwable>) null))
            .isInstanceOf(NullPointerException.class);
    }

    // ---------- flatMapError ----------

    @Test
    void flatMapError_shouldChainFailureCauses() {
        RuntimeException recovered = new RuntimeException("recovered");
        Try<String> result = Try.<String>failure(new RuntimeException("original"))
            .flatMapError(ex -> Try.failure(recovered));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(recovered);
    }

    @Test
    void flatMapError_shouldAllowConvertingFailureToSuccess() {
        Try<String> result = Try.<String>failure(new RuntimeException("oops"))
            .flatMapError(ex -> Try.success("fallback"));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("fallback");
    }

    @Test
    void flatMapError_shouldLeaveSuccessUntouched() {
        AtomicBoolean called = new AtomicBoolean(false);
        Try<String> result = Try.success("hello")
            .flatMapError(ex -> {
                called.set(true);
                return Try.failure(ex);
            });
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
        assertThat(called.get()).as("mapper must not be called for Success").isFalse();
    }

    @Test
    void flatMapError_shouldThrowNPE_ifMapperIsNull() {
        assertThatThrownBy(() -> Try.<String>failure(new RuntimeException()).flatMapError(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void flatMapError_shouldReturnFailureWithNPE_ifMapperReturnsNull() {
        Try<String> result = Try.<String>failure(new RuntimeException()).flatMapError(ex -> null);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper returned null");
    }

    @Test
    void flatMapError_shouldReturnFailure_ifMapperThrows() {
        RuntimeException thrown = new RuntimeException("mapper blew up");
        Try<String> result = Try.<String>failure(new RuntimeException("original"))
            .flatMapError(ex -> { throw thrown; });
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(thrown);
    }
}
