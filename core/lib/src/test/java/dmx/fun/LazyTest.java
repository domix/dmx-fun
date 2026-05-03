package dmx.fun;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LazyTest {

    // ---------- construction ----------

    @Test
    void of_nullSupplier_throwsNullPointerException() {
        assertThatThrownBy(() -> Lazy.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void of_doesNotEvaluateSupplierEagerly() {
        var calls = new AtomicInteger(0);
        Lazy.of(() -> { calls.incrementAndGet(); return "x"; });
        assertThat(calls.get()).isEqualTo(0);
    }

    // ---------- get / memoization ----------

    @Test
    void get_evaluatesSupplierOnFirstCall() {
        var lazy = Lazy.of(() -> "hello");
        assertThat(lazy.get()).isEqualTo("hello");
    }

    @Test
    void get_supplierCalledExactlyOnce() {
        var calls = new AtomicInteger(0);
        var lazy = Lazy.of(() -> { calls.incrementAndGet(); return "v"; });
        lazy.get();
        lazy.get();
        lazy.get();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void get_nullReturningSupplier_throwsNullPointerException() {
        var lazy = Lazy.of(() -> null);
        assertThatThrownBy(lazy::get).isInstanceOf(NullPointerException.class);
    }

    @Test
    void get_throwingSupplier_supplierCalledExactlyOnce() {
        var calls = new AtomicInteger(0);
        var lazy = Lazy.<String>of(() -> { calls.incrementAndGet(); throw new IllegalStateException("boom"); });
        assertThatThrownBy(lazy::get).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(lazy::get).isInstanceOf(IllegalStateException.class);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void get_throwingSupplier_sameExceptionRethrownOnSubsequentCalls() {
        var ex = new IllegalStateException("boom");
        var lazy = Lazy.<String>of(() -> { throw ex; });
        assertThatThrownBy(lazy::get).isSameAs(ex);
        assertThatThrownBy(lazy::get).isSameAs(ex);
    }

    @Test
    void get_nullReturningSupplier_supplierCalledExactlyOnce() {
        var calls = new AtomicInteger(0);
        var lazy = Lazy.of(() -> { calls.incrementAndGet(); return null; });
        assertThatThrownBy(lazy::get).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(lazy::get).isInstanceOf(NullPointerException.class);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void get_throwingError_propagatesErrorWithoutWrapping() {
        var lazy = Lazy.<String>of(() -> { throw new OutOfMemoryError("oom"); });
        assertThatThrownBy(lazy::get).isInstanceOf(OutOfMemoryError.class);
    }

    // ---------- isEvaluated ----------

    @Test
    void isEvaluated_falseBeforeGet() {
        var lazy = Lazy.of(() -> 42);
        assertThat(lazy.isEvaluated()).isFalse();
    }

    @Test
    void isEvaluated_trueAfterGet() {
        var lazy = Lazy.of(() -> 42);
        lazy.get();
        assertThat(lazy.isEvaluated()).isTrue();
    }

    @Test
    void isEvaluated_trueAfterThrowingGet() {
        var lazy = Lazy.<String>of(() -> { throw new RuntimeException("x"); });
        assertThatThrownBy(lazy::get).isInstanceOf(RuntimeException.class);
        assertThat(lazy.isEvaluated()).isTrue();
    }

    // ---------- map ----------

    @Test
    void map_appliesFunctionToValue() {
        var lazy = Lazy.of(() -> "hello").map(String::toUpperCase);
        assertThat(lazy.get()).isEqualTo("HELLO");
    }

    @Test
    void map_isLazy_doesNotEvaluateUntilGet() {
        var calls = new AtomicInteger(0);
        var mapped = Lazy.of(() -> { calls.incrementAndGet(); return "x"; })
            .map(s -> s + "!");
        assertThat(calls.get()).isEqualTo(0);
        mapped.get();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void map_nullFunction_throwsNullPointerException() {
        assertThatThrownBy(() -> Lazy.of(() -> "x").map(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void map_nullReturningFunction_throwsNullPointerException() {
        var lazy = Lazy.of(() -> "x").map(s -> null);
        assertThatThrownBy(lazy::get).isInstanceOf(NullPointerException.class);
    }

    // ---------- flatMap ----------

    @Test
    void flatMap_chainsLazily() {
        var lazy = Lazy.of(() -> 3)
            .flatMap(n -> Lazy.of(() -> n * 10));
        assertThat(lazy.get()).isEqualTo(30);
    }

    @Test
    void flatMap_isLazy_doesNotEvaluateUntilGet() {
        var calls = new AtomicInteger(0);
        var chained = Lazy.of(() -> { calls.incrementAndGet(); return 1; })
            .flatMap(n -> Lazy.of(() -> n + 1));
        assertThat(calls.get()).isEqualTo(0);
        chained.get();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void flatMap_nullFunction_throwsNullPointerException() {
        assertThatThrownBy(() -> Lazy.of(() -> "x").flatMap(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void flatMap_nullReturningFunction_throwsNullPointerException() {
        var lazy = Lazy.of(() -> "x").flatMap(s -> null);
        assertThatThrownBy(lazy::get).isInstanceOf(NullPointerException.class);
    }

    // ---------- toOption ----------

    @Test
    void toOption_returnsSomeWithValue() {
        var opt = Lazy.of(() -> "value").toOption();
        assertThat(opt.isDefined()).isTrue();
        assertThat(opt.get()).isEqualTo("value");
    }

    // ---------- toTry ----------

    @Test
    void toTry_successfulSupplier_returnsSuccess() {
        var t = Lazy.of(() -> 42).toTry();
        assertThat(t.isSuccess()).isTrue();
        assertThat(t.get()).isEqualTo(42);
    }

    @Test
    void toTry_throwingSupplier_returnsFailure() {
        var lazy = Lazy.of(() -> { throw new IllegalStateException("boom"); });
        var t = lazy.toTry();
        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause().getMessage()).isEqualTo("boom");
    }

    @Test
    void toTry_calledMultipleTimes_returnsSameCachedInstance() {
        var lazy = Lazy.of(() -> "v");
        assertThat(lazy.toTry()).isSameAs(lazy.toTry());
    }

    @Test
    void toTry_throwingSupplier_calledMultipleTimes_supplierCalledExactlyOnce() {
        var calls = new AtomicInteger(0);
        var lazy = Lazy.<String>of(() -> { calls.incrementAndGet(); throw new RuntimeException("x"); });
        lazy.toTry();
        lazy.toTry();
        assertThat(calls.get()).isEqualTo(1);
    }

    // ---------- toResult ----------

    @Test
    void toResult_successfulSupplier_returnsOk() {
        var r = Lazy.of(() -> 42).toResult();
        assertThat(r.isOk()).isTrue();
        assertThat(r.get()).isEqualTo(42);
    }

    @Test
    void toResult_throwingSupplier_returnsErr() {
        var lazy = Lazy.<Integer>of(() -> { throw new IllegalStateException("boom"); });
        var r = lazy.toResult();
        assertThat(r.isError()).isTrue();
        assertThat(r.getError().getMessage()).isEqualTo("boom");
    }

    @Test
    void toResult_withMapper_successfulSupplier_returnsOk() {
        var r = Lazy.of(() -> 7).toResult(Throwable::getMessage);
        assertThat(r.isOk()).isTrue();
        assertThat(r.get()).isEqualTo(7);
    }

    @Test
    void toResult_withMapper_throwingSupplier_appliesMapper() {
        var lazy = Lazy.<Integer>of(() -> { throw new IllegalArgumentException("bad"); });
        var r = lazy.toResult(Throwable::getMessage);
        assertThat(r.isError()).isTrue();
        assertThat(r.getError()).isEqualTo("bad");
    }

    @Test
    void toResult_withMapper_nullMapper_throwsNullPointerException() {
        assertThatThrownBy(() -> Lazy.of(() -> "x").toResult(null))
            .isInstanceOf(NullPointerException.class);
    }

    // ---------- fromFuture ----------

    @Test
    void fromFuture_completedFuture_defersUntilGet() {
        var calls = new AtomicInteger(0);
        var future = CompletableFuture.supplyAsync(() -> { calls.incrementAndGet(); return 42; });
        var lazy = Lazy.fromFuture(future);
        // fromFuture itself does not block or evaluate
        assertThat(lazy.get()).isEqualTo(42);
        assertThat(lazy.isEvaluated()).isTrue();
    }

    @Test
    void fromFuture_nullFuture_throwsNullPointerException() {
        assertThatThrownBy(() -> Lazy.fromFuture(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void fromFuture_failedFuture_getOrThrowRethrowsCause() {
        var future = CompletableFuture.<String>failedFuture(new IllegalStateException("fail"));
        var lazy = Lazy.fromFuture(future);
        assertThatThrownBy(lazy::get).isInstanceOf(RuntimeException.class);
    }

    @Test
    void fromFuture_failedFuture_runtimeExceptionPreservedAsIs() {
        var cause = new IllegalStateException("fail");
        var lazy = Lazy.fromFuture(CompletableFuture.<String>failedFuture(cause));
        assertThatThrownBy(lazy::get).isSameAs(cause);
    }

    @Test
    void fromFuture_failedFuture_sameExceptionRethrownOnSubsequentCalls() {
        var cause = new IllegalStateException("fail");
        var lazy = Lazy.fromFuture(CompletableFuture.<String>failedFuture(cause));
        assertThatThrownBy(lazy::get).isSameAs(cause);
        assertThatThrownBy(lazy::get).isSameAs(cause);
    }

    @Test
    void fromFuture_failedFutureWithError_propagatesErrorWithoutWrapping() {
        var error = new OutOfMemoryError("oom");
        var lazy = Lazy.fromFuture(CompletableFuture.<String>failedFuture(error));
        assertThatThrownBy(lazy::get).isInstanceOf(OutOfMemoryError.class);
    }

    @Test
    void fromFuture_failedFutureWithCheckedException_wrapsInRuntimeException() {
        var cause = new Exception("checked");
        var future = new CompletableFuture<String>();
        future.completeExceptionally(cause);
        var lazy = Lazy.fromFuture(future);
        assertThatThrownBy(lazy::get)
            .isInstanceOf(RuntimeException.class)
            .hasCause(cause);
    }

    @Test
    void fromFuture_cancelledFuture_throwsCancellationException() {
        var future = new CompletableFuture<String>();
        future.cancel(true);
        var lazy = Lazy.fromFuture(future);
        assertThatThrownBy(lazy::get).isInstanceOf(CancellationException.class);
    }

    @Test
    void fromFuture_failedFuture_isEvaluatedTrueAfterGet() {
        var lazy = Lazy.fromFuture(CompletableFuture.<String>failedFuture(new RuntimeException("x")));
        assertThatThrownBy(lazy::get).isInstanceOf(RuntimeException.class);
        assertThat(lazy.isEvaluated()).isTrue();
    }

    // ---------- toFuture ----------

    @Test
    void toFuture_notYetEvaluated_returnsAsyncFuture() throws Exception {
        var lazy = Lazy.of(() -> "async");
        var future = lazy.toFuture();
        assertThat(future.get()).isEqualTo("async");
        assertThat(lazy.isEvaluated()).isTrue();
    }

    @Test
    void toFuture_alreadyEvaluated_returnsAlreadyCompletedFuture() throws Exception {
        var lazy = Lazy.of(() -> "cached");
        lazy.get(); // force evaluation
        var future = lazy.toFuture();
        assertThat(future.isDone()).isTrue();
        assertThat(future.get()).isEqualTo("cached");
    }

    @Test
    void toFuture_alreadyEvaluatedFailure_returnsFailedFuture() {
        var lazy = Lazy.<String>of(() -> { throw new RuntimeException("x"); });
        assertThatThrownBy(lazy::get).isInstanceOf(RuntimeException.class);
        assertThat(lazy.toFuture().isCompletedExceptionally()).isTrue();
    }

    @Test
    void toFuture_supplierCalledOnceAcrossGetAndToFuture() throws Exception {
        var calls = new AtomicInteger(0);
        var lazy = Lazy.of(() -> { calls.incrementAndGet(); return "x"; });
        lazy.get();
        lazy.toFuture().get();
        assertThat(calls.get()).isEqualTo(1);
    }

    // ---------- toString ----------

    @Test
    void toString_beforeEvaluation_returnsPlaceholder() {
        assertThat(Lazy.of(() -> "x").toString()).isEqualTo("Lazy[?]");
    }

    @Test
    void toString_afterEvaluation_containsValue() {
        var lazy = Lazy.of(() -> "hello");
        lazy.get();
        assertThat(lazy.toString()).isEqualTo("Lazy[hello]");
    }

    @Test
    void toString_afterFailure_returnsFailurePlaceholder() {
        var lazy = Lazy.<String>of(() -> { throw new RuntimeException("x"); });
        assertThatThrownBy(lazy::get).isInstanceOf(RuntimeException.class);
        assertThat(lazy.toString()).isEqualTo("Lazy[!]");
    }
}
