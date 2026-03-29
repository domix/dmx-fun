package codes.domix.fun;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LazyTest {

    // ── construction ──────────────────────────────────────────────────────────

    @Test
    void of_nullSupplier_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Lazy.of(null));
    }

    @Test
    void of_doesNotEvaluateSupplierEagerly() {
        var calls = new AtomicInteger(0);
        Lazy.of(() -> { calls.incrementAndGet(); return "x"; });
        assertEquals(0, calls.get());
    }

    // ── get / memoization ─────────────────────────────────────────────────────

    @Test
    void get_evaluatesSupplierOnFirstCall() {
        var lazy = Lazy.of(() -> "hello");
        assertEquals("hello", lazy.get());
    }

    @Test
    void get_supplierCalledExactlyOnce() {
        var calls = new AtomicInteger(0);
        var lazy = Lazy.of(() -> { calls.incrementAndGet(); return "v"; });
        lazy.get();
        lazy.get();
        lazy.get();
        assertEquals(1, calls.get());
    }

    @Test
    void get_nullReturningSupplier_throwsNullPointerException() {
        var lazy = Lazy.of(() -> null);
        assertThrows(NullPointerException.class, lazy::get);
    }

    // ── isEvaluated ───────────────────────────────────────────────────────────

    @Test
    void isEvaluated_falseBeforeGet() {
        var lazy = Lazy.of(() -> 42);
        assertFalse(lazy.isEvaluated());
    }

    @Test
    void isEvaluated_trueAfterGet() {
        var lazy = Lazy.of(() -> 42);
        lazy.get();
        assertTrue(lazy.isEvaluated());
    }

    // ── map ───────────────────────────────────────────────────────────────────

    @Test
    void map_appliesFunctionToValue() {
        var lazy = Lazy.of(() -> "hello").map(String::toUpperCase);
        assertEquals("HELLO", lazy.get());
    }

    @Test
    void map_isLazy_doesNotEvaluateUntilGet() {
        var calls = new AtomicInteger(0);
        var mapped = Lazy.of(() -> { calls.incrementAndGet(); return "x"; })
            .map(s -> s + "!");
        assertEquals(0, calls.get());
        mapped.get();
        assertEquals(1, calls.get());
    }

    @Test
    void map_nullFunction_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Lazy.of(() -> "x").map(null));
    }

    @Test
    void map_nullReturningFunction_throwsNullPointerException() {
        var lazy = Lazy.of(() -> "x").map(s -> null);
        assertThrows(NullPointerException.class, lazy::get);
    }

    // ── flatMap ───────────────────────────────────────────────────────────────

    @Test
    void flatMap_chainsLazily() {
        var lazy = Lazy.of(() -> 3)
            .flatMap(n -> Lazy.of(() -> n * 10));
        assertEquals(30, lazy.get());
    }

    @Test
    void flatMap_isLazy_doesNotEvaluateUntilGet() {
        var calls = new AtomicInteger(0);
        var chained = Lazy.of(() -> { calls.incrementAndGet(); return 1; })
            .flatMap(n -> Lazy.of(() -> n + 1));
        assertEquals(0, calls.get());
        chained.get();
        assertEquals(1, calls.get());
    }

    @Test
    void flatMap_nullFunction_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Lazy.of(() -> "x").flatMap(null));
    }

    // ── toOption ──────────────────────────────────────────────────────────────

    @Test
    void toOption_returnsSomeWithValue() {
        var opt = Lazy.of(() -> "value").toOption();
        assertTrue(opt.isDefined());
        assertEquals("value", opt.get());
    }

    // ── toTry ─────────────────────────────────────────────────────────────────

    @Test
    void toTry_successfulSupplier_returnsSuccess() {
        var t = Lazy.of(() -> 42).toTry();
        assertTrue(t.isSuccess());
        assertEquals(42, t.get());
    }

    @Test
    void toTry_throwingSupplier_returnsFailure() {
        var lazy = Lazy.of(() -> { throw new IllegalStateException("boom"); });
        var t = lazy.toTry();
        assertTrue(t.isFailure());
        assertEquals("boom", t.getCause().getMessage());
    }

    // ── toResult ──────────────────────────────────────────────────────────────

    @Test
    void toResult_successfulSupplier_returnsOk() {
        var r = Lazy.of(() -> 42).toResult();
        assertTrue(r.isOk());
        assertEquals(42, r.get());
    }

    @Test
    void toResult_throwingSupplier_returnsErr() {
        var lazy = Lazy.<Integer>of(() -> { throw new IllegalStateException("boom"); });
        var r = lazy.toResult();
        assertTrue(r.isError());
        assertEquals("boom", r.getError().getMessage());
    }

    @Test
    void toResult_withMapper_successfulSupplier_returnsOk() {
        var r = Lazy.of(() -> 7).toResult(Throwable::getMessage);
        assertTrue(r.isOk());
        assertEquals(7, r.get());
    }

    @Test
    void toResult_withMapper_throwingSupplier_appliesMapper() {
        var lazy = Lazy.<Integer>of(() -> { throw new IllegalArgumentException("bad"); });
        var r = lazy.toResult(Throwable::getMessage);
        assertTrue(r.isError());
        assertEquals("bad", r.getError());
    }

    @Test
    void toResult_withMapper_nullMapper_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Lazy.of(() -> "x").toResult(null));
    }

    // ── fromFuture ────────────────────────────────────────────────────────────

    @Test
    void fromFuture_completedFuture_defersUntilGet() {
        var calls = new AtomicInteger(0);
        var future = CompletableFuture.supplyAsync(() -> { calls.incrementAndGet(); return 42; });
        var lazy = Lazy.fromFuture(future);
        // fromFuture itself does not block or evaluate
        assertEquals(42, lazy.get());
        assertTrue(lazy.isEvaluated());
    }

    @Test
    void fromFuture_nullFuture_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Lazy.fromFuture(null));
    }

    @Test
    void fromFuture_failedFuture_getOrThrowRethrowsCause() {
        var future = CompletableFuture.<String>failedFuture(new IllegalStateException("fail"));
        var lazy = Lazy.fromFuture(future);
        assertThrows(RuntimeException.class, lazy::get);
    }

    // ── toFuture ──────────────────────────────────────────────────────────────

    @Test
    void toFuture_notYetEvaluated_returnsAsyncFuture() throws Exception {
        var lazy = Lazy.of(() -> "async");
        var future = lazy.toFuture();
        assertEquals("async", future.get());
        assertTrue(lazy.isEvaluated());
    }

    @Test
    void toFuture_alreadyEvaluated_returnsAlreadyCompletedFuture() throws Exception {
        var lazy = Lazy.of(() -> "cached");
        lazy.get(); // force evaluation
        var future = lazy.toFuture();
        assertTrue(future.isDone());
        assertEquals("cached", future.get());
    }

    @Test
    void toFuture_supplierCalledOnceAcrossGetAndToFuture() throws Exception {
        var calls = new AtomicInteger(0);
        var lazy = Lazy.of(() -> { calls.incrementAndGet(); return "x"; });
        lazy.get();
        lazy.toFuture().get();
        assertEquals(1, calls.get());
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    void toString_beforeEvaluation_returnsPlaceholder() {
        assertEquals("Lazy[?]", Lazy.of(() -> "x").toString());
    }

    @Test
    void toString_afterEvaluation_containsValue() {
        var lazy = Lazy.of(() -> "hello");
        lazy.get();
        assertEquals("Lazy[hello]", lazy.toString());
    }
}
