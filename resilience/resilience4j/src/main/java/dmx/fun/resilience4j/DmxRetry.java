package dmx.fun.resilience4j;

import dmx.fun.CheckedSupplier;
import dmx.fun.NonFatal;
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

    /**
     * Wraps an existing {@link Retry} instance.
     *
     * @param retry the Resilience4J retry policy to wrap
     * @return a new {@code DmxRetry} backed by the given retry policy
     */
    public static DmxRetry of(Retry retry) {
        return new DmxRetry(Objects.requireNonNull(retry, "retry"));
    }

    /**
     * Creates a new {@link Retry} from the given name and config, then wraps it.
     *
     * @param name   the retry policy name
     * @param config the retry configuration
     * @return a new {@code DmxRetry} backed by the created retry policy
     */
    public static DmxRetry of(String name, RetryConfig config) {
        return new DmxRetry(Retry.of(name, config));
    }

    /**
     * Executes the supplier through the retry policy.
     *
     * <p>Fatal throwables are never captured: if the call fails with one — per
     * {@link NonFatal}, including one reachable through the cause chain or suppressed
     * exceptions — it is rethrown via {@link NonFatal#rethrowIfFatal(Throwable)}, with the
     * interrupt flag restored when an {@link InterruptedException} is involved.
     *
     * @param <V>      the value type
     * @param supplier the operation to execute
     * @return {@code Success(value)} if the call eventually succeeds,
     *         {@code Failure(cause)} if all attempts are exhausted
     */
    public <V> Try<V> executeTry(CheckedSupplier<V> supplier) {
        try {
            return Try.success(retry.executeCheckedSupplier(supplier::get));
        } catch (Throwable t) {
            NonFatal.rethrowIfFatal(t);
            return Try.failure(t);
        }
    }

    /**
     * Executes the supplier through the retry policy.
     *
     * <p>Fatal throwables are rethrown rather than returned as {@code Err} — see
     * {@link #executeTry(CheckedSupplier)}.
     *
     * @param <V>      the value type
     * @param supplier the operation to execute
     * @return {@code Ok(value)} if the call eventually succeeds,
     *         {@code Err(cause)} if all attempts are exhausted
     */
    public <V> Result<V, Throwable> executeResult(CheckedSupplier<V> supplier) {
        return executeTry(supplier).toResult();
    }
}
