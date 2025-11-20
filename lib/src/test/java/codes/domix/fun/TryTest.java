package codes.domix.fun;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TryTest {

    @Test
    void of_shouldReturnSuccessWhenComputationDoesNotThrow() {
        var t = Try.of(() -> 42);

        assertTrue(t.isSuccess());
        assertFalse(t.isFailure());
        assertEquals(42, t.get());
    }

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
    }

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

    @Test
    void map_shouldTransformValueOnSuccess() {
        var t = Try.of(() -> 10);

        var mapped = t.map(v -> "value:" + v);

        assertTrue(mapped.isSuccess());
        assertEquals("value:10", mapped.get());
    }

    @Test
    void map_shouldConvertToFailureWhenMapperThrows() {
        var t = Try.of(() -> 10);

        var mapped = t.map(_ -> {
            throw new RuntimeException("mapper failed");
        });

        assertTrue(mapped.isFailure());
        assertEquals("mapper failed", mapped.getCause().getMessage());
    }

    @Test
    void flatMap_shouldChainSuccessValues() {
        var t = Try.of(() -> 2);

        var result = t
            .flatMap(v -> Try.of(() -> v * 10))
            .flatMap(v -> Try.success("result:" + v));

        assertTrue(result.isSuccess());
        assertEquals("result:20", result.get());
    }

    @Test
    void flatMap_shouldPropagateFailure() {
        var t = Try.of(() -> 2);

        var result = t.flatMap(_ -> Try.failure(new IllegalArgumentException("bad")));

        assertTrue(result.isFailure());
        assertInstanceOf(IllegalArgumentException.class, result.getCause());
        assertEquals("bad", result.getCause().getMessage());
    }

    @Test
    void onSuccess_shouldRunActionOnlyOnSuccess() {
        var successCalled = new AtomicBoolean(false);
        var failureCalled = new AtomicBoolean(false);

        var success = Try.success(42);
        var failure = Try.failure(new RuntimeException("err"));

        success
            .onSuccess(_ -> successCalled.set(true))
            .onFailure(_ -> failureCalled.set(true));

        assertTrue(successCalled.get());
        assertFalse(failureCalled.get());
    }

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

    @Test
    void recover_shouldReturnSameSuccess() {
        var success = Try.success(10);

        var recovered = success.recover(_ -> 99);

        assertSame(success, recovered); // misma instancia
        assertEquals(10, recovered.get());
    }

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

    @Test
    void recoverWith_shouldUseAlternativeTryOnFailure() {
        var failure = Try.<Integer>failure(new RuntimeException("boom"));

        var recovered = failure.recoverWith(_ -> Try.success(123));

        assertTrue(recovered.isSuccess());
        assertEquals(123, recovered.get());
    }

    @Test
    void getOrElse_shouldReturnValueOnSuccessOrFallbackOnFailure() {
        var success = Try.success(5);
        var failure = Try.failure(new RuntimeException("err"));

        assertEquals(5, success.getOrElse(99));
        assertEquals(99, failure.getOrElse(99));
    }

    @Test
    void getOrElseGet_shouldUseSupplierOnlyOnFailure() {
        var success = Try.success(5);
        var failure = Try.<Integer>failure(new RuntimeException("err"));

        var supplierCalled = new AtomicBoolean(false);

        int value1 = success.getOrElseGet(() -> {
            supplierCalled.set(true);
            return 99;
        });
        int value2 = failure.getOrElseGet(() -> {
            supplierCalled.set(true);
            return 100;
        });

        assertEquals(5, value1);
        assertEquals(100, value2);
        assertTrue(supplierCalled.get());
    }

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

    @Test
    void getOrThrow_withMapper_shouldThrowMappedRuntimeException() {
        var failure = Try.<Integer>failure(new IllegalStateException("illegal"));

        var ex = assertThrows(
            RuntimeException.class,
            () -> failure.getOrThrow(cause -> new RuntimeException("wrapped: " + cause.getMessage()))
        );

        assertEquals("wrapped: illegal", ex.getMessage());
    }

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

    // --- Leyes monádicas básicas para Try ---

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

    @Test
    void monad_associativity() {
        var m = Try.success(3);

        Function<Integer, Try<Integer>> f = x -> Try.success(x + 1);
        Function<Integer, Try<Integer>> g = x -> Try.success(x * 2);

        var left = m.flatMap(f).flatMap(g);
        Try<Integer> right = m.flatMap(x -> f.apply(x).flatMap(g));

        assertEquals(right, left);
    }

    // --- Integración con Result<Value, Throwable> ---

    @Test
    void toResult_shouldConvertSuccessToOk() {
        Try<String> t = Try.success("ok");

        Result<String, Throwable> result = t.toResult();

        assertTrue(result.isOk());
        assertEquals("ok", result.get());
    }

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

    @Test
    void getOrThrow_withoutMapper_shouldWrapErrorInRuntimeException() {
        AssertionError error = new AssertionError("assert failed");
        Try<Integer> t = Try.failure(error);

        // este es el getOrThrow() sin mapper
        RuntimeException ex = assertThrows(RuntimeException.class, t::getOrThrow);

        assertSame(error, ex.getCause());
        assertEquals("assert failed", ex.getCause().getMessage());
    }

    @Test
    @Disabled("need to handle the CheckedException")
    void map_shouldTurnToFailureWhenMapperThrowsCheckedException() {
        Try<Integer> t = Try.success(10);

        Try<String> mapped = t.map(v -> {
            try {
                //TODO: handle CheckedException
                throw new IOException("checked");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(mapped.isFailure());
        assertInstanceOf(IOException.class, mapped.getCause());
        assertEquals("checked", mapped.getCause().getMessage());
    }

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

    @Test
    @Disabled("need to handle the CheckedException")
    void flatMap_shouldTurnToFailureWhenMapperThrowsCheckedException() {
        Try<Integer> t = Try.success(10);

        Try<String> mapped = t.flatMap(v -> {
            try {
                //TODO: handle CheckedException
                throw new IOException("flat checked");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(mapped.isFailure());
        assertInstanceOf(IOException.class, mapped.getCause());
        assertEquals("flat checked", mapped.getCause().getMessage());
    }

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

    @Test
    @Disabled("need to handle the CheckedException")
    void recover_shouldReturnFailureIfRecoverFunctionThrows() {
        Try<Integer> failure = Try.failure(new RuntimeException("original"));

        Try<Integer> recovered = failure.recover(cause -> {
            try {
                //TODO: handle CheckedException
                throw new IOException("recover failed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(recovered.isFailure());
        assertInstanceOf(IOException.class, recovered.getCause());
        assertEquals("recover failed", recovered.getCause().getMessage());
    }

    @Test
    @Disabled("need to handle the CheckedException")
    void recoverWith_shouldReturnFailureIfRecoverFunctionThrows() {
        Try<Integer> failure = Try.failure(new RuntimeException("original"));

        Try<Integer> recovered = failure.recoverWith(cause -> {
            try {
                //TODO: handle CheckedException
                throw new IOException("recoverWith failed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(recovered.isFailure());
        assertInstanceOf(IOException.class, recovered.getCause());
        assertEquals("recoverWith failed", recovered.getCause().getMessage());
    }

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

    @Test
    void toResult_withErrorShouldKeepExactThrowableInstance() {
        AssertionError error = new AssertionError("critical");
        Try<Integer> t = Try.failure(error);

        Result<Integer, Throwable> result = t.toResult();

        assertTrue(result.isError());
        assertSame(error, result.getError());
    }

    @Test
    void fromResult_withNonExceptionThrowableShouldCreateFailureWithSameInstance() {
        AssertionError error = new AssertionError("assert!");
        Result<Integer, Throwable> result = Result.err(error);

        Try<Integer> t = Try.fromResult(result);

        assertTrue(t.isFailure());
        assertSame(error, t.getCause());
    }
}
