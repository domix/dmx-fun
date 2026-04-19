package dmx.fun.resilience4j;

import dmx.fun.Result;
import dmx.fun.Try;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmxRateLimiterTest {

    /** Rate limiter that allows 1 call per hour — any second call is immediately rejected. */
    private static DmxRateLimiter saturatedAfterOne() {
        RateLimiterConfig config = RateLimiterConfig.custom()
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
        Try<String> result = generous().executeTry(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeTry_callFailure_returnsFailure() {
        IOException boom = new IOException("boom");
        Try<String> result = generous().executeTry(() -> { throw boom; });

        assertThat(result).failsWith(IOException.class);
        assertThat(result.getCause()).isSameAs(boom);
    }

    @Test
    void executeTry_rateLimitExceeded_returnsRequestNotPermitted() {
        DmxRateLimiter rl = saturatedAfterOne();
        rl.executeTry(() -> "first");

        Try<String> result = rl.executeTry(() -> "second");

        assertThat(result).failsWith(RequestNotPermitted.class);
    }

    @Test
    void executeResult_success() {
        Result<String, Throwable> result = generous().executeResult(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_success() {
        Result<String, RequestNotPermitted> result = generous().executeResultTyped(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_rateLimitExceeded_returnsErr() {
        DmxRateLimiter rl = saturatedAfterOne();
        rl.executeResultTyped(() -> "first");

        Result<String, RequestNotPermitted> result = rl.executeResultTyped(() -> "second");

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
        RateLimiter rl = RateLimiter.ofDefaults("existing");
        DmxRateLimiter dmxRl = DmxRateLimiter.of(rl);

        assertThat(dmxRl.executeTry(() -> 42)).containsValue(42);
    }
}
