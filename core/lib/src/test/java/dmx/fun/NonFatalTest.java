package dmx.fun;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NonFatalTest {

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted(); // never leak the flag into other tests
    }

    @Test
    void check_shouldClassifyOrdinaryExceptionsAsNonFatal() {
        assertThat(NonFatal.check(new RuntimeException())).isTrue();
        assertThat(NonFatal.check(new IOException())).isTrue();
        assertThat(NonFatal.check(new AssertionError())).isTrue();
    }

    @Test
    void check_shouldClassifyFatalThrowables() {
        assertThat(NonFatal.check(new OutOfMemoryError())).isFalse();
        assertThat(NonFatal.check(new StackOverflowError())).isFalse();
        assertThat(NonFatal.check(new NoClassDefFoundError())).isFalse();
        assertThat(NonFatal.check(new InterruptedException())).isFalse();
    }

    @Test
    void check_shouldNotInspectCauseChain() {
        assertThat(NonFatal.check(new RuntimeException(new OutOfMemoryError()))).isTrue();
    }

    @Test
    void check_shouldThrowNPEOnNull() {
        assertThatThrownBy(() -> NonFatal.check(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("throwable");
    }

    @Test
    void rethrowIfFatal_shouldReturnNormallyOnNonFatal() {
        assertThatCode(() -> NonFatal.rethrowIfFatal(new IOException("boom")))
            .doesNotThrowAnyException();
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    void rethrowIfFatal_shouldRethrowFatalErrorDirectly() {
        var oom = new OutOfMemoryError("boom");

        assertThatThrownBy(() -> NonFatal.rethrowIfFatal(oom)).isSameAs(oom);
    }

    @Test
    void rethrowIfFatal_shouldInspectSuppressedExceptions() {
        var oom = new OutOfMemoryError("boom");
        var body = new IOException("body failed");
        body.addSuppressed(oom);

        assertThatThrownBy(() -> NonFatal.rethrowIfFatal(body)).isSameAs(oom);
    }

    @Test
    void rethrowIfFatal_shouldWrapInterruptionAndSetInterruptFlag() {
        var ie = new InterruptedException("cancelled");

        assertThatThrownBy(() -> NonFatal.rethrowIfFatal(ie))
            .isInstanceOf(CompletionException.class)
            .cause().isSameAs(ie);
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void rethrowIfFatal_shouldThrowNPEOnNull() {
        assertThatThrownBy(() -> NonFatal.rethrowIfFatal(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("throwable");
    }

    @Test
    void rethrowIfFatal_shouldSetInterruptFlagWhenInterruptionIsSuppressedOnFatalError() {
        var oom = new OutOfMemoryError("boom");
        oom.addSuppressed(new InterruptedException("release interrupted"));

        assertThatThrownBy(() -> NonFatal.rethrowIfFatal(oom)).isSameAs(oom);
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void rethrowIfFatal_shouldDetectInterruptionOnlyReachableViaSuppressed() {
        var body = new IOException("body failed");
        body.addSuppressed(new InterruptedException("release interrupted"));

        assertThatThrownBy(() -> NonFatal.rethrowIfFatal(body))
            .isInstanceOf(CompletionException.class)
            .cause().isSameAs(body);
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void rethrowIfFatal_shouldInspectGraphOfSuppressedException() {
        var oom = new OutOfMemoryError("boom");
        var suppressed = new IOException("release failed", oom);
        var body = new IOException("body failed");
        body.addSuppressed(suppressed);

        assertThatThrownBy(() -> NonFatal.rethrowIfFatal(body)).isSameAs(oom);
    }

    @Test
    void rethrowIfFatal_shouldNotDoubleWrapCompletionException() {
        var ce = new CompletionException(new InterruptedException("cancelled"));

        assertThatThrownBy(() -> NonFatal.rethrowIfFatal(ce)).isSameAs(ce);
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void rethrowIfFatal_shouldPreserveExecutionExceptionInWrappedGraph() {
        var ee = new ExecutionException(new InterruptedException("cancelled"));

        assertThatThrownBy(() -> NonFatal.rethrowIfFatal(ee))
            .isInstanceOf(CompletionException.class)
            .cause().isSameAs(ee);
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void rethrowIfFatal_shouldFindFatalWithinTraversalBound() {
        var oom = new OutOfMemoryError("boom");
        Throwable chain = oom;
        for (var i = 0; i < 999; i++) { // oom is the 1000th node — last one inside the cap
            chain = new RuntimeException("wrapper " + i, chain);
        }
        var visible = chain;

        assertThatThrownBy(() -> NonFatal.rethrowIfFatal(visible)).isSameAs(oom);
    }

    @Test
    void rethrowIfFatal_shouldNotSeeFatalBeyondTraversalBound() {
        Throwable chain = new OutOfMemoryError("boom");
        for (var i = 0; i < 1000; i++) { // pushes the fatal to node 1001 — past the cap
            chain = new RuntimeException("wrapper " + i, chain);
        }
        var hidden = chain;

        assertThatCode(() -> NonFatal.rethrowIfFatal(hidden)).doesNotThrowAnyException();
    }

    @Test
    void rethrowIfFatal_shouldNotSeeFatalSuppressedBeyondTraversalBound() {
        var root = new RuntimeException("root");
        for (var i = 0; i < 999; i++) { // root + 999 suppressed exhaust the cap
            root.addSuppressed(new IOException("suppressed " + i));
        }
        root.addSuppressed(new OutOfMemoryError("boom")); // node 1001 — past the cap

        assertThatCode(() -> NonFatal.rethrowIfFatal(root)).doesNotThrowAnyException();
    }

    @Test
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void rethrowIfFatal_shouldNotLoopOnCauseCycle() {
        var a = new RuntimeException("a");
        var b = new RuntimeException("b", a);
        a.initCause(b);

        assertThatCode(() -> NonFatal.rethrowIfFatal(a)).doesNotThrowAnyException();
    }

    @Test
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void rethrowIfFatal_shouldFindFatalSuppressedOnCauseCycle() {
        var oom = new OutOfMemoryError("boom");
        var a = new RuntimeException("a");
        var b = new RuntimeException("b", a);
        a.initCause(b); // cause cycle must not starve the suppressed graph
        a.addSuppressed(new IOException("release failed", oom));

        assertThatThrownBy(() -> NonFatal.rethrowIfFatal(a)).isSameAs(oom);
    }
}
