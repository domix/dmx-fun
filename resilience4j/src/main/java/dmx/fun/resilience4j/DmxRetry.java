package dmx.fun.resilience4j;

import dmx.fun.CheckedSupplier;
import dmx.fun.Result;
import dmx.fun.Try;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * dmx-fun adapter for Resilience4J {@link Retry}.
 *
 * <p>Executes a supplier through the configured retry policy and returns a dmx-fun type
 * instead of throwing. Configure the retry using the native Resilience4J API:
 *
 * <pre>{@code
 * RetryConfig config = RetryConfig.custom()
 *     .maxAttempts(3)
 *     .waitDuration(Duration.ofMillis(200))
 *     .retryOnException(IOException.class::isInstance)
 *     .build();
 *
 * DmxRetry retry = DmxRetry.of("my-retry", config);
 *
 * Try<Response>               r1 = retry.executeTry(() -> httpClient.get(url));
 * Result<Response, Throwable> r2 = retry.executeResult(() -> httpClient.get(url));
 * }</pre>
 */
@NullMarked
public final class DmxRetry {

    private final Retry retry;

    private DmxRetry(Retry retry) {
        this.retry = retry;
    }

    /** Wraps an existing {@link Retry} instance. */
    public static DmxRetry of(Retry retry) {
        return new DmxRetry(Objects.requireNonNull(retry, "retry"));
    }

    /** Creates a new {@link Retry} from the given name and config, then wraps it. */
    public static DmxRetry of(String name, RetryConfig config) {
        return new DmxRetry(Retry.of(name, config));
    }

    /**
     * Executes the supplier through the retry policy.
     *
     * @return {@code Success(value)} if the call eventually succeeds,
     *         {@code Failure(cause)} if all attempts are exhausted
     */
    public <V> Try<V> executeTry(CheckedSupplier<V> supplier) {
        try {
            return Try.success(retry.executeCheckedSupplier(supplier::get));
        } catch (Throwable t) {
            return Try.failure(t);
        }
    }

    /**
     * Executes the supplier through the retry policy.
     *
     * @return {@code Ok(value)} if the call eventually succeeds,
     *         {@code Err(cause)} if all attempts are exhausted
     */
    public <V> Result<V, Throwable> executeResult(CheckedSupplier<V> supplier) {
        return executeTry(supplier).toResult();
    }
}
