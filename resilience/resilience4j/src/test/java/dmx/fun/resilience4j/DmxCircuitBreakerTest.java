package dmx.fun.resilience4j;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DmxCircuitBreakerTest {

    private static DmxCircuitBreaker openCircuit() {
        var cb = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
        cb.transitionToOpenState();
        return DmxCircuitBreaker.of(cb);
    }

    private static DmxCircuitBreaker closedCircuit() {
        return DmxCircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
    }

    @Test
    void executeTry_success() {
        var result = closedCircuit().executeTry(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeTry_callFailure_returnsFailure() {
        var boom = new IOException("boom");
        var result = closedCircuit().executeTry(() -> { throw boom; });

        assertThat(result).failsWith(IOException.class);
        assertThat(result.getCause()).isSameAs(boom);
    }

    @Test
    void executeTry_circuitOpen_returnsCallNotPermittedException() {
        var result = openCircuit().executeTry(() -> "ok");

        assertThat(result).failsWith(CallNotPermittedException.class);
    }

    @Test
    void executeResult_success() {
        var result = closedCircuit().executeResult(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_success() {
        var result = closedCircuit().executeResultTyped(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_circuitOpen_returnsErr() {
        var result = openCircuit().executeResultTyped(() -> "ok");

        assertThat(result).isErr();
        assertThat(result.getError()).isInstanceOf(CallNotPermittedException.class);
    }

    @Test
    void executeResultTyped_callFailure_propagatesAsUnchecked() {
        assertThatThrownBy(() -> closedCircuit().executeResultTyped(() -> { throw new IOException("boom"); }))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void of_wrapsExistingCircuitBreaker() {
        var cb = CircuitBreaker.ofDefaults("existing");
        var dmxCb = DmxCircuitBreaker.of(cb);

        assertThat(dmxCb.executeTry(() -> 42)).containsValue(42);
    }

    // ── null contracts ────────────────────────────────────────────────────────────

    @Test
    void of_nullCircuitBreaker_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DmxCircuitBreaker.of(null));
    }

    // ── fatal throwables ───────────────────────────────────────────────────────

    @Test
    void executeTry_interruptedException_isRethrownWithInterruptFlagSet() {
        var interruption = new InterruptedException("stop");
        try {
            assertThatThrownBy(() -> closedCircuit().executeTry(() -> {
                throw interruption;
            }))
                .isInstanceOf(CompletionException.class)
                .hasCause(interruption);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted(); // do not leak the flag into other tests
        }
    }

    @Test
    void executeTry_fatalError_isRethrown() {
        var overflow = new StackOverflowError("deep");

        assertThatThrownBy(() -> closedCircuit().executeTry(() -> {
            throw overflow;
        })).isSameAs(overflow);
    }

    @Test
    void executeResult_fatalError_isRethrown() {
        var overflow = new StackOverflowError("deep");

        assertThatThrownBy(() -> closedCircuit().executeResult(() -> {
            throw overflow;
        })).isSameAs(overflow);
    }

    @Test
    void executeTry_nonFatalError_staysFailure() {
        var failed = new AssertionError("not fatal");
        var result = closedCircuit().executeTry(() -> {
            throw failed;
        });

        assertThat(result).failsWith(AssertionError.class);
        assertThat(result.getCause()).isSameAs(failed);
    }

    @Test
    void executeResultTyped_interruptedException_wrapsAndRestoresInterruptFlag() {
        var interruption = new InterruptedException("stop");
        try {
            assertThatThrownBy(() -> closedCircuit().executeResultTyped(() -> {
                throw interruption;
            }))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCause(interruption);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void executeResultTyped_interruptionAsCause_restoresInterruptFlag() {
        var wrapped = new ExecutionException(new InterruptedException("stop"));
        try {
            assertThatThrownBy(() -> closedCircuit().executeResultTyped(() -> {
                throw wrapped;
            }))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCause(wrapped);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void executeResultTyped_checkedExceptionWithoutInterruption_leavesInterruptFlagClear() {
        assertThatThrownBy(() -> closedCircuit().executeResultTyped(() -> {
            throw new IOException("boom");
        })).isInstanceOf(RuntimeException.class);

        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }
}
