package codes.domix.fun;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class TryTest {

    @Test
    void of_shouldReturnSuccessWhenComputationDoesNotThrow() {
        Try<Integer> t = Try.of(() -> 42);

        assertTrue(t.isSuccess());
        assertFalse(t.isFailure());
        assertEquals(42, t.get());
    }

    @Test
    void of_shouldReturnFailureWhenComputationThrows() {
        Try<Integer> t = Try.of(() -> {
            throw new IOException("boom");
        });

        assertTrue(t.isFailure());
        assertFalse(t.isSuccess());
        Throwable cause = t.getCause();
        assertInstanceOf(IOException.class, cause);
        assertEquals("boom", cause.getMessage());
    }

    @Test
    void run_shouldReturnSuccessOnVoidSideEffect() {
        AtomicBoolean executed = new AtomicBoolean(false);

        Try<Void> t = Try.run(() -> executed.set(true));

        assertTrue(t.isSuccess());
        assertTrue(executed.get());
    }

    @Test
    void run_shouldReturnFailureWhenSideEffectThrows() {
        Try<Void> t = Try.run(() -> {
            throw new IllegalStateException("side-effect failed");
        });

        assertTrue(t.isFailure());
        assertInstanceOf(IllegalStateException.class, t.getCause());
    }

    @Test
    void map_shouldTransformValueOnSuccess() {
        Try<Integer> t = Try.of(() -> 10);

        Try<String> mapped = t.map(v -> "value:" + v);

        assertTrue(mapped.isSuccess());
        assertEquals("value:10", mapped.get());
    }

    @Test
    void map_shouldConvertToFailureWhenMapperThrows() {
        Try<Integer> t = Try.of(() -> 10);

        Try<String> mapped = t.map(v -> {
            throw new RuntimeException("mapper failed");
        });

        assertTrue(mapped.isFailure());
        assertEquals("mapper failed", mapped.getCause().getMessage());
    }

    @Test
    void flatMap_shouldChainSuccessValues() {
        Try<Integer> t = Try.of(() -> 2);

        Try<String> result = t
            .flatMap(v -> Try.of(() -> v * 10))
            .flatMap(v -> Try.success("result:" + v));

        assertTrue(result.isSuccess());
        assertEquals("result:20", result.get());
    }

    @Test
    void flatMap_shouldPropagateFailure() {
        Try<Integer> t = Try.of(() -> 2);

        Try<String> result = t.flatMap(v -> Try.failure(new IllegalArgumentException("bad")));

        assertTrue(result.isFailure());
        assertInstanceOf(IllegalArgumentException.class, result.getCause());
        assertEquals("bad", result.getCause().getMessage());
    }

    @Test
    void onSuccess_shouldRunActionOnlyOnSuccess() {
        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicBoolean failureCalled = new AtomicBoolean(false);

        Try<Integer> success = Try.success(42);
        Try<Integer> failure = Try.failure(new RuntimeException("err"));

        success
            .onSuccess(v -> successCalled.set(true))
            .onFailure(cause -> failureCalled.set(true));

        assertTrue(successCalled.get());
        assertFalse(failureCalled.get());
    }

    @Test
    void onFailure_shouldRunActionOnlyOnFailure() {
        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicBoolean failureCalled = new AtomicBoolean(false);

        Try<Integer> failure = Try.failure(new RuntimeException("err"));

        failure
            .onSuccess(v -> successCalled.set(true))
            .onFailure(cause -> failureCalled.set(true));

        assertFalse(successCalled.get());
        assertTrue(failureCalled.get());
    }

    @Test
    void recover_shouldReturnSameSuccess() {
        Try<Integer> success = Try.success(10);

        Try<Integer> recovered = success.recover(throwable -> 99);

        assertSame(success, recovered); // misma instancia
        assertEquals(10, recovered.get());
    }

    @Test
    void recover_shouldProduceNewSuccessFromFailure() {
        Try<Integer> failure = Try.failure(new RuntimeException("boom"));

        Try<Integer> recovered = failure.recover(throwable -> 99);

        assertTrue(recovered.isSuccess());
        assertEquals(99, recovered.get());
    }

    @Test
    void recoverWith_shouldReturnSameSuccess() {
        Try<Integer> success = Try.success(10);

        Try<Integer> recovered = success.recoverWith(t -> Try.success(99));

        assertSame(success, recovered);
        assertEquals(10, recovered.get());
    }

    @Test
    void recoverWith_shouldUseAlternativeTryOnFailure() {
        Try<Integer> failure = Try.failure(new RuntimeException("boom"));

        Try<Integer> recovered = failure.recoverWith(t -> Try.success(123));

        assertTrue(recovered.isSuccess());
        assertEquals(123, recovered.get());
    }

    @Test
    void getOrElse_shouldReturnValueOnSuccessOrFallbackOnFailure() {
        Try<Integer> success = Try.success(5);
        Try<Integer> failure = Try.failure(new RuntimeException("err"));

        assertEquals(5, success.getOrElse(99));
        assertEquals(99, failure.getOrElse(99));
    }

    @Test
    void getOrElseGet_shouldUseSupplierOnlyOnFailure() {
        Try<Integer> success = Try.success(5);
        Try<Integer> failure = Try.failure(new RuntimeException("err"));

        AtomicBoolean supplierCalled = new AtomicBoolean(false);

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
        Try<Integer> success = Try.success(5);
        Try<Integer> failure = Try.failure(new RuntimeException("err"));

        assertEquals(5, success.getOrNull());
        assertNull(failure.getOrNull());
    }

    @Test
    void getOrThrow_withoutMapper_shouldThrowOriginalExceptionIfException() {
        Try<Integer> failure = Try.of(() -> {
            throw new IOException("io boom");
        });

        IOException ex = assertThrows(IOException.class, failure::getOrThrow);
        assertEquals("io boom", ex.getMessage());
    }

    @Test
    void getOrThrow_withMapper_shouldThrowMappedRuntimeException() {
        Try<Integer> failure = Try.failure(new IllegalStateException("illegal"));

        RuntimeException ex = assertThrows(
            RuntimeException.class,
            () -> failure.getOrThrow(cause -> new RuntimeException("wrapped: " + cause.getMessage()))
        );

        assertEquals("wrapped: illegal", ex.getMessage());
    }

    @Test
    void fold_shouldApplyOnSuccessOrOnFailure() {
        Try<Integer> success = Try.success(10);
        Try<Integer> failure = Try.failure(new RuntimeException("err"));

        String foldedSuccess = success.fold(
            v -> "value:" + v,
            t -> "error:" + t.getMessage()
        );

        String foldedFailure = failure.fold(
            v -> "value:" + v,
            t -> "error:" + t.getMessage()
        );

        assertEquals("value:10", foldedSuccess);
        assertEquals("error:err", foldedFailure);
    }

    // --- Leyes monádicas básicas para Try ---

    @Test
    void monad_leftIdentity() {
        Integer a = 42;
        Function<Integer, Try<String>> f = x -> Try.success("value:" + x);

        Try<String> left = Try.of(() -> a).flatMap(f);
        Try<String> right = f.apply(a);

        assertEquals(right, left);
    }

    @Test
    void monad_rightIdentity() {
        Try<Integer> m = Try.success(7);

        Try<Integer> left = m.flatMap(Try::success);
        Try<Integer> right = m;

        assertEquals(right, left);
    }

    @Test
    void monad_associativity() {
        Try<Integer> m = Try.success(3);

        Function<Integer, Try<Integer>> f = x -> Try.success(x + 1);
        Function<Integer, Try<Integer>> g = x -> Try.success(x * 2);

        Try<Integer> left = m.flatMap(f).flatMap(g);
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
}
