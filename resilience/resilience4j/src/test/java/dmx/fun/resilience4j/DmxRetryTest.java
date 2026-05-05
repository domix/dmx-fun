package dmx.fun.resilience4j;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DmxRetryTest {

    private static DmxRetry noRetry() {
        return DmxRetry.of("test", RetryConfig.custom().maxAttempts(1).build());
    }

    private static DmxRetry withRetries(int maxAttempts) {
        return DmxRetry.of("test", RetryConfig.custom().maxAttempts(maxAttempts).build());
    }

    @Test
    void executeTry_success() {
        var result = noRetry().executeTry(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeTry_failure_returnsFailure() {
        var boom = new IOException("boom");
        var result = noRetry().executeTry(() -> { throw boom; });

        assertThat(result).failsWith(IOException.class);
        assertThat(result.getCause()).isSameAs(boom);
    }

    @Test
    void executeTry_retriesOnFailure() {
        var attempts = new AtomicInteger(0);
        var result = withRetries(3).executeTry(() -> {
            if (attempts.incrementAndGet() < 3) throw new IOException("not yet");
            return "done";
        });

        assertThat(result).containsValue("done");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void executeTry_allAttemptsExhausted_returnsLastFailure() {
        var result = withRetries(2).executeTry(() -> { throw new IOException("always"); });

        assertThat(result).failsWith(IOException.class);
    }

    @Test
    void executeResult_success() {
        var result = noRetry().executeResult(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResult_failure_returnsErr() {
        var result = noRetry().executeResult(() -> { throw new IOException("boom"); });

        assertThat(result).isErr();
        assertThat(result.getError()).isInstanceOf(IOException.class);
    }

    @Test
    void of_wrapsExistingRetry() {
        var retry = Retry.ofDefaults("existing");
        var dmxRetry = DmxRetry.of(retry);

        assertThat(dmxRetry.executeTry(() -> 42)).containsValue(42);
    }

    // ── null contracts ────────────────────────────────────────────────────────────

    @Test
    void of_nullRetry_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DmxRetry.of(null));
    }
}
