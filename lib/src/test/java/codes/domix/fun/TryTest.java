package codes.domix.fun;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TryTest {

    /**
     * Verifies success case returns value and has no cause
     */
    @Test
    void of_shouldReturnSuccessWhenComputationDoesNotThrow() {
        var t = Try.of(() -> 42);

        assertTrue(t.isSuccess());
        assertFalse(t.isFailure());
        assertEquals(42, t.get());
        assertThrows(NoSuchElementException.class, t::getCause);
    }

    /**
     * Verifies failure case returns exception and metadata
     */
    @Test
    void of_shouldReturnFailureWhenComputationThrows() {
        var t = Try.of(() -> {
            throw new IOException("boom");
        });

        assertTrue(t.isFailure());
        assertFalse(t.isSuccess());
        var cause = t.getCause();
        assertInstanceOf(IOException.class, cause);
        assertEquals("boom", cause.getMessage());
        assertThrows(NoSuchElementException.class, t::get);
    }

    /**
     * Verifies side effect executes and returns success
     */
    @Test
    void run_shouldReturnSuccessOnVoidSideEffect() {
        var executed = new AtomicBoolean(false);

        var t = Try.run(() -> executed.set(true));

        assertTrue(t.isSuccess());
        assertTrue(executed.get());
    }

    @Test
    void run_shouldReturnFailureWhenSideEffectThrows() {
        var t = Try.run(() -> {
            throw new IllegalStateException("side-effect failed");
        });

        assertTrue(t.isFailure());
        assertInstanceOf(IllegalStateException.class, t.getCause());
    }

    /**
     * Maps successful result to transformed value
     */
    @Test
    void map_shouldTransformValueOnSuccess() {
        var t = Try.of(() -> 10);

        var mapped = t.map(v -> "value:" + v);

        assertTrue(mapped.isSuccess());
        assertEquals("value:10", mapped.get());
    }

    /**
     * Maps failure case; preserves original exception
     */
    @Test
    void map_shouldMapFailure() {
        var t = Try.failure(new RuntimeException("boom"));
        var mapped = t.map(v -> "value:" + v);

        assertTrue(mapped.isFailure());
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

        assertTrue(mapped.isFailure());
        assertEquals("mapper failed", mapped.getCause().getMessage());
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

        assertTrue(result.isSuccess());
        assertEquals("result:20", result.get());
    }

    /**
     * Verifies failure propagates through flatMap
     */
    @Test
    void flatMap_shouldPropagateFailure() {
        var t = Try.of(() -> 2);

        var result = t.flatMap(_ -> Try.failure(new IllegalArgumentException("bad")));

        assertTrue(result.isFailure());
        assertInstanceOf(IllegalArgumentException.class, result.getCause());
        assertEquals("bad", result.getCause().getMessage());
    }

    /**
     * `flatMap` ignores mapped failure; propagates initial failure
     */
    @Test
    void flatMap_shouldIgnoreMappedFailure() {
        var t = Try.failure(new IllegalArgumentException("bad"));

        var result = t.flatMap(_ -> Try.failure(new IllegalArgumentException("BAD")));
        assertThat(result.isFailure());
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

        assertTrue(successCalled.get());
        assertFalse(failureCalled.get());
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

        assertFalse(successCalled.get());
        assertTrue(failureCalled.get());
    }

    /**
     * `recoverWith` uses alternative `Try` on failure
     */
    @Test
    void recover_shouldReturnSameSuccess() {
        var success = Try.success(10);

        var recovered = success.recover(_ -> 99);

        assertSame(success, recovered); // same instance
        assertEquals(10, recovered.get());
    }

    /**
     * `recover` produces success from failure via recovery function
     */
    @Test
    void recover_shouldProduceNewSuccessFromFailure() {
        var failure = Try.<Integer>failure(new RuntimeException("boom"));

        var recovered = failure.recover(_ -> 99);

        assertTrue(recovered.isSuccess());
        assertEquals(99, recovered.get());
    }

    @Test
    void recoverWith_shouldReturnSameSuccess() {
        var success = Try.success(10);

        var recovered = success.recoverWith(_ -> Try.success(99));

        assertSame(success, recovered);
        assertEquals(10, recovered.get());
    }

    /**
     * `recoverWith` uses alternative `Try` on failure
     */
    @Test
    void recoverWith_shouldUseAlternativeTryOnFailure() {
        var failure = Try.<Integer>failure(new RuntimeException("boom"));

        var recovered = failure.recoverWith(_ -> Try.success(123));

        assertTrue(recovered.isSuccess());
        assertEquals(123, recovered.get());
    }

    /**
     * Demonstrates `getOrElse` returns value or fallback
     */
    @Test
    void getOrElse_shouldReturnValueOnSuccessOrFallbackOnFailure() {
        var success = Try.success(5);
        var failure = Try.failure(new RuntimeException("err"));

        assertEquals(5, success.getOrElse(99));
        assertEquals(99, failure.getOrElse(99));
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
        assertFalse(supplierCalled.get(), "Supplier should not be called on success");
        int value2 = failure.getOrElseGet(() -> {
            supplierCalled.set(true);
            return 100;
        });

        assertEquals(5, value1);
        assertEquals(100, value2);
        assertTrue(supplierCalled.get());
    }

    /**
     * Verifies null result for failure case
     */
    @Test
    void getOrNull_shouldReturnNullOnFailure() {
        var success = Try.success(5);
        var failure = Try.failure(new RuntimeException("err"));

        assertEquals(5, success.getOrNull());
        assertNull(failure.getOrNull());
    }

    @Test
    void getOrThrow_withoutMapper_shouldThrowOriginalExceptionIfException() throws Exception {
        var failure = Try.of(() -> {
            throw new IOException("io boom");
        });

        var ex = assertThrows(IOException.class, failure::getOrThrow);
        assertEquals("io boom", ex.getMessage());
    }

    /**
     * Verifies exception mapping during `getOrThrow` call
     */
    @Test
    void getOrThrow_withMapper_shouldThrowMappedRuntimeException() {
        var failure = Try.<Integer>failure(new IllegalStateException("illegal"));

        var ex = assertThrows(
            RuntimeException.class,
            () -> failure.getOrThrow(cause -> new RuntimeException("wrapped: " + cause.getMessage()))
        );

        assertEquals("wrapped: illegal", ex.getMessage());
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

        assertEquals("value:10", foldedSuccess);
        assertEquals("error:err", foldedFailure);
    }

    // --- Basic monadic laws for Try ---

    @Test
    void monad_leftIdentity() {
        var a = 42;
        Function<Integer, Try<String>> f = x -> Try.success("value:" + x);

        var left = Try.of(() -> a).flatMap(f);
        var right = f.apply(a);

        assertEquals(right, left);
    }

    @Test
    void monad_rightIdentity() {
        var m = Try.success(7);

        var left = m.flatMap(Try::success);
        var right = m;

        assertEquals(right, left);
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

        assertEquals(right, left);
    }

    // --- Integration with Result<Value, Throwable> ---

    @Test
    void toResult_shouldConvertSuccessToOk() {
        Try<String> t = Try.success("ok");

        Result<String, Throwable> result = t.toResult();

        assertTrue(result.isOk());
        assertEquals("ok", result.get());
    }

    /**
     * Converts failure to error with a message preserved
     */
    @Test
    void toResult_shouldConvertFailureToErr() {
        Try<String> t = Try.failure(new RuntimeException("err"));

        Result<String, Throwable> result = t.toResult();

        assertTrue(result.isError());
        assertEquals("err", result.getError().getMessage());
    }

    @Test
    void fromResult_shouldConvertOkToSuccess() {
        Result<String, Throwable> result = Result.ok("hello");

        Try<String> t = Try.fromResult(result);

        assertTrue(t.isSuccess());
        assertEquals("hello", t.get());
    }

    /**
     * Converts a failure result to a failed try
     */
    @Test
    void fromResult_shouldConvertErrToFailure() {
        RuntimeException ex = new RuntimeException("boom");
        Result<String, Throwable> result = Result.err(ex);

        Try<String> t = Try.fromResult(result);

        assertTrue(t.isFailure());
        assertSame(ex, t.getCause());
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

        assertTrue(t.isFailure());
        assertSame(error, t.getCause());
    }

    @Test
    void run_shouldCaptureErrorNotOnlyException() {
        AssertionError error = new AssertionError("boom");

        Try<Void> t = Try.run(() -> {
            throw error;
        });

        assertTrue(t.isFailure());
        assertSame(error, t.getCause());
    }

    /**
     * Verifies `getOrThrow` wraps error in `RuntimeException`
     */
    @Test
    void getOrThrow_withoutMapper_shouldWrapErrorInRuntimeException() {
        AssertionError error = new AssertionError("assert failed");
        Try<Integer> t = Try.failure(error);

        // no mapper
        RuntimeException ex = assertThrows(RuntimeException.class, t::getOrThrow);

        assertSame(error, ex.getCause());
        assertEquals("assert failed", ex.getCause().getMessage());
    }

    /**
     * Verifies `getOrThrow` returns success value
     */
    @Test
    void getOrThrow_shouldReturnTheSuccessValue() throws Exception {
        var aTry = Try.success(1);
        var result = aTry
            .getOrThrow();
        assertEquals(1, result);

        var result2 = aTry
            .getOrThrow(cause -> new RuntimeException("wrapped: " + cause.getMessage()));

        assertEquals(1, result2);
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
        assertInstanceOf(RuntimeException.class, mapped.getCause());
        assertInstanceOf(IOException.class, mapped.getCause().getCause());
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

        assertTrue(mapped.isFailure());
        assertSame(error, mapped.getCause());
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

        assertTrue(mapped.isFailure());
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

        assertTrue(mapped.isFailure());
        assertSame(error, mapped.getCause());
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

        assertTrue(recovered.isFailure());
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

        assertTrue(recovered.isFailure());
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
    void recoverWith_shouldReturnFailureIfRecoverFunctionThrowsError() {
        Try<Integer> failure = Try.failure(new RuntimeException("original"));
        AssertionError error = new AssertionError("recoverWith error");

        Try<Integer> recovered = failure.recoverWith(cause -> {
            throw error;
        });

        assertTrue(recovered.isFailure());
        assertSame(error, recovered.getCause());
    }

    /**
     * Verifies `toResult` preserves exact throwable instance
     */
    @Test
    void toResult_withErrorShouldKeepExactThrowableInstance() {
        AssertionError error = new AssertionError("critical");
        Try<Integer> t = Try.failure(error);

        Result<Integer, Throwable> result = t.toResult();

        assertTrue(result.isError());
        assertSame(error, result.getError());
    }

    /**
     * Creates failure from a result with the same throwable instance
     */
    @Test
    void fromResult_withNonExceptionThrowableShouldCreateFailureWithSameInstance() {
        AssertionError error = new AssertionError("assert!");
        Result<Integer, Throwable> result = Result.err(error);

        Try<Integer> t = Try.fromResult(result);

        assertTrue(t.isFailure());
        assertSame(error, t.getCause());
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

        assertTrue(result.isOk());
        assertEquals(42, result.get());
    }

    @Test
    void toResult_withMapper_shouldConvertFailureToErrUsingMapper() {
        Try<Integer> t = Try.failure(new RuntimeException("boom"));
        Result<Integer, String> result = t.toResult(Throwable::getMessage);

        assertTrue(result.isError());
        assertEquals("boom", result.getError());
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

        assertTrue(t.isFailure());
        assertInstanceOf(IllegalStateException.class, t.getCause());
        assertEquals("domain error", t.getCause().getMessage());
        assertSame(original, t.getCause().getCause());
    }

    @Test
    void mapFailure_shouldNotAffectSuccess() {
        Try<Integer> success = Try.success(10);
        Try<Integer> result = success.mapFailure(e -> new RuntimeException("should not run"));

        assertSame(success, result);
        assertEquals(10, result.get());
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
        assertTrue(t.isFailure());
        assertInstanceOf(NullPointerException.class, t.getCause());
    }

    @Test
    void mapFailure_shouldWrapException_ifMapperThrows() {
        RuntimeException mapperBoom = new RuntimeException("mapper exploded");
        Try<Integer> t = Try.<Integer>failure(new RuntimeException("original"))
            .mapFailure(_ -> { throw mapperBoom; });

        assertTrue(t.isFailure());
        assertSame(mapperBoom, t.getCause());
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
    void stream_canBeUsedToFlattenStreamOfTries() {
        List<Integer> result = Stream.<Try<Integer>>of(
            Try.success(1),
            Try.failure(new RuntimeException()),
            Try.success(3)
        ).flatMap(Try::stream).toList();

        assertThat(result).containsExactly(1, 3);
    }

    // ---------- filter(Predicate, Function) ----------

    @Test
    void filter_withFunction_shouldReturnSuccess_whenPredicateHolds() {
        Try<Integer> t = Try.success(10)
            .filter(n -> n > 0, n -> new IllegalArgumentException("non-positive: " + n));

        assertTrue(t.isSuccess());
        assertEquals(10, t.get());
    }

    @Test
    void filter_withFunction_shouldReturnFailure_withContextualMessage_whenPredicateFails() {
        Try<Integer> t = Try.success(-5)
            .filter(n -> n > 0, n -> new IllegalArgumentException("non-positive: " + n));

        assertTrue(t.isFailure());
        assertInstanceOf(IllegalArgumentException.class, t.getCause());
        assertEquals("non-positive: -5", t.getCause().getMessage());
    }

    @Test
    void filter_withFunction_shouldNotAffectFailure() {
        RuntimeException original = new RuntimeException("original");
        Try<Integer> t = Try.<Integer>failure(original)
            .filter(n -> n > 0, n -> new IllegalArgumentException("should not run"));

        assertTrue(t.isFailure());
        assertSame(original, t.getCause());
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
}
