package dmx.fun;

import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FutureAdaptersTest {

    // ── Try.fromFuture ────────────────────────────────────────────────────────

    @Test
    void tryFromFuture_succeededFuture_returnsSuccess() {
        var future = CompletableFuture.completedFuture("hello");
        var result = Try.fromFuture(future);
        assertTrue(result.isSuccess());
        assertEquals("hello", result.get());
    }

    @Test
    void tryFromFuture_failedFuture_returnsFailureWithUnwrappedCause() {
        var cause = new IllegalStateException("boom");
        var future = CompletableFuture.<String>failedFuture(cause);
        var result = Try.fromFuture(future);
        assertTrue(result.isFailure());
        assertInstanceOf(IllegalStateException.class, result.getCause());
        assertEquals("boom", result.getCause().getMessage());
    }

    @Test
    void tryFromFuture_completionExceptionWithNullCause_preservesCompletionException() {
        var future = new CompletableFuture<String>();
        future.completeExceptionally(new CompletionException(null));
        var result = Try.fromFuture(future);
        assertTrue(result.isFailure());
        assertInstanceOf(CompletionException.class, result.getCause());
    }

    @Test
    void tryFromFuture_cancelledFuture_returnsFailureWithCancellationException() {
        var future = new CompletableFuture<String>();
        future.cancel(true);
        var result = Try.fromFuture(future);
        assertTrue(result.isFailure());
        assertInstanceOf(CancellationException.class, result.getCause());
    }

    @Test
    void tryFromFuture_nullFuture_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Try.fromFuture(null));
    }

    // ── Try.toFuture ──────────────────────────────────────────────────────────

    @Test
    void tryToFuture_success_returnsCompletedFuture() throws Exception {
        var future = Try.success(42).toFuture();
        assertTrue(future.isDone());
        assertEquals(42, future.get());
    }

    @Test
    void tryToFuture_failure_returnsExceptionallyCompletedFuture() {
        var cause = new RuntimeException("fail");
        var future = Try.<Integer>failure(cause).toFuture();
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void tryToFuture_roundtrip_successPreservesValue() {
        var original = Try.success("round-trip");
        var recovered = Try.fromFuture(original.toFuture());
        assertEquals(original, recovered);
    }

    @Test
    void tryToFuture_roundtrip_failurePreservesCause() {
        var cause = new IllegalArgumentException("cause");
        var original = Try.<String>failure(cause);
        var recovered = Try.fromFuture(original.toFuture());
        assertTrue(recovered.isFailure());
        assertEquals(cause, recovered.getCause());
    }

    // ── Result.fromFuture ─────────────────────────────────────────────────────

    @Test
    void resultFromFuture_succeededFuture_returnsOk() {
        var future = CompletableFuture.completedFuture(99);
        var result = Result.fromFuture(future);
        assertTrue(result.isOk());
        assertEquals(99, result.get());
    }

    @Test
    void resultFromFuture_failedFuture_returnsErrWithUnwrappedCause() {
        var cause = new IllegalStateException("err");
        var future = CompletableFuture.<Integer>failedFuture(cause);
        var result = Result.fromFuture(future);
        assertTrue(result.isError());
        assertInstanceOf(IllegalStateException.class, result.getError());
        assertEquals("err", result.getError().getMessage());
    }

    @Test
    void resultFromFuture_cancelledFuture_returnsErrWithCancellationException() {
        var future = new CompletableFuture<Integer>();
        future.cancel(true);
        var result = Result.fromFuture(future);
        assertTrue(result.isError());
        assertInstanceOf(CancellationException.class, result.getError());
    }

    @Test
    void resultFromFuture_nullFuture_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Result.fromFuture(null));
    }

    // ── Result.toFuture ───────────────────────────────────────────────────────

    @Test
    void resultToFuture_ok_returnsCompletedFuture() throws Exception {
        var future = Result.ok("value").toFuture();
        assertTrue(future.isDone());
        assertEquals("value", future.get());
    }

    @Test
    void resultToFuture_err_returnsExceptionallyCompletedFutureWithNoSuchElementException() {
        var future = Result.<String, String>err("something went wrong").toFuture();
        assertTrue(future.isCompletedExceptionally());
        var ex = assertThrows(Exception.class, future::get);
        assertInstanceOf(NoSuchElementException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("something went wrong"));
    }

    @Test
    void resultToFuture_withMapper_ok_returnsCompletedFuture() throws Exception {
        var future = Result.<Integer, String>ok(42)
            .toFuture(msg -> new IllegalArgumentException(msg));
        assertEquals(42, future.get());
    }

    @Test
    void resultToFuture_withMapper_err_usesMapperException() {
        var future = Result.<Integer, String>err("bad input")
            .toFuture(IllegalArgumentException::new);
        assertTrue(future.isCompletedExceptionally());
        var ex = assertThrows(Exception.class, future::get);
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertEquals("bad input", ex.getCause().getMessage());
    }

    @Test
    void resultToFuture_withMapper_nullMapper_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Result.ok("x").toFuture(null));
    }

    @Test
    void resultToFuture_roundtrip_okPreservesValue() {
        var original = Result.<String, String>ok("round-trip");
        var recovered = Result.fromFuture(original.toFuture());
        assertTrue(recovered.isOk());
        assertEquals("round-trip", recovered.get());
    }
}
