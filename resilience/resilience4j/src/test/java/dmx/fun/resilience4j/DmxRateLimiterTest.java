package dmx.fun.resilience4j;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DmxRateLimiterTest {

    /** Rate limiter that allows 1 call per hour — any second call is immediately rejected. */
    private static DmxRateLimiter saturatedAfterOne() {
        var config = RateLimiterConfig.custom()
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofHours(1))
            .timeoutDuration(Duration.ZERO)
            .build();
        return DmxRateLimiter.of("test", config);
    }

    private static DmxRateLimiter generous() {
        return DmxRateLimiter.of("test", RateLimiterConfig.ofDefaults());
    }

    @Test
    void executeTry_success() {
        var result = generous().executeTry(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeTry_callFailure_returnsFailure() {
        var boom = new IOException("boom");
        var result = generous().executeTry(() -> { throw boom; });

        assertThat(result).failsWith(IOException.class);
        assertThat(result.getCause()).isSameAs(boom);
    }

    @Test
    void executeTry_rateLimitExceeded_returnsRequestNotPermitted() {
        var rl = saturatedAfterOne();
        assertThat(rl.executeTry(() -> "first")).containsValue("first");

        var result = rl.executeTry(() -> "second");

        assertThat(result).failsWith(RequestNotPermitted.class);
    }

    @Test
    void executeResult_success() {
        var result = generous().executeResult(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_success() {
        var result = generous().executeResultTyped(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_rateLimitExceeded_returnsErr() {
        var rl = saturatedAfterOne();
        assertThat(rl.executeResultTyped(() -> "first")).containsValue("first");

        var result = rl.executeResultTyped(() -> "second");

        assertThat(result).isErr();
        assertThat(result.getError()).isInstanceOf(RequestNotPermitted.class);
    }

    @Test
    void executeResultTyped_callFailure_propagatesAsUnchecked() {
        assertThatThrownBy(() -> generous().executeResultTyped(() -> { throw new IOException("boom"); }))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void of_wrapsExistingRateLimiter() {
        var rl = RateLimiter.ofDefaults("existing");
        var dmxRl = DmxRateLimiter.of(rl);

        assertThat(dmxRl.executeTry(() -> 42)).containsValue(42);
    }

    // ── null contracts ────────────────────────────────────────────────────────────

    @Test
    void of_nullRateLimiter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DmxRateLimiter.of(null));
    }

    // ── fatal throwables ───────────────────────────────────────────────────────

    @Test
    void executeTry_interruptedException_isRethrownWithInterruptFlagSet() {
        var interruption = new InterruptedException("stop");
        try {
            assertThatThrownBy(() -> generous().executeTry(() -> {
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

        assertThatThrownBy(() -> generous().executeTry(() -> {
            throw overflow;
        })).isSameAs(overflow);
    }

    @Test
    void executeResult_fatalError_isRethrown() {
        var overflow = new StackOverflowError("deep");

        assertThatThrownBy(() -> generous().executeResult(() -> {
            throw overflow;
        })).isSameAs(overflow);
    }

    @Test
    void executeTry_nonFatalError_staysFailure() {
        var failed = new AssertionError("not fatal");
        var result = generous().executeTry(() -> {
            throw failed;
        });

        assertThat(result).failsWith(AssertionError.class);
        assertThat(result.getCause()).isSameAs(failed);
    }

    @Test
    void executeResultTyped_interruptedException_wrapsAndRestoresInterruptFlag() {
        var interruption = new InterruptedException("stop");
        try {
            assertThatThrownBy(() -> generous().executeResultTyped(() -> {
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
            assertThatThrownBy(() -> generous().executeResultTyped(() -> {
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
        assertThatThrownBy(() -> generous().executeResultTyped(() -> {
            throw new IOException("boom");
        })).isInstanceOf(RuntimeException.class);

        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }
}
