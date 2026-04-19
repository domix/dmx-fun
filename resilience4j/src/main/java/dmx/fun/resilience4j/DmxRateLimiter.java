package dmx.fun.resilience4j;

import dmx.fun.CheckedSupplier;
import dmx.fun.Result;
import dmx.fun.Try;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * dmx-fun adapter for Resilience4J {@link RateLimiter}.
 *
 * <p>Executes a supplier through the rate limiter and returns a dmx-fun type instead of
 * throwing. Configure the rate limiter using the native Resilience4J API:
 *
 * <pre>{@code
 * DmxRateLimiter rl = DmxRateLimiter.of("payments", RateLimiterConfig.ofDefaults());
 *
 * Try<Receipt>                         r1 = rl.executeTry(() -> gateway.charge(amount));
 * Result<Receipt, Throwable>           r2 = rl.executeResult(() -> gateway.charge(amount));
 * Result<Receipt, RequestNotPermitted> r3 = rl.executeResultTyped(() -> gateway.charge(amount));
 * }</pre>
 */
@NullMarked
public final class DmxRateLimiter {

    private final RateLimiter rateLimiter;

    private DmxRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /** Wraps an existing {@link RateLimiter} instance. */
    public static DmxRateLimiter of(RateLimiter rateLimiter) {
        return new DmxRateLimiter(Objects.requireNonNull(rateLimiter, "rateLimiter"));
    }

    /** Creates a new {@link RateLimiter} from the given name and config, then wraps it. */
    public static DmxRateLimiter of(String name, RateLimiterConfig config) {
        return new DmxRateLimiter(RateLimiter.of(name, config));
    }

    /**
     * Executes the supplier through the rate limiter.
     *
     * @return {@code Success(value)} on success,
     *         {@code Failure(RequestNotPermitted)} when the rate limit is exceeded,
     *         or {@code Failure(cause)} when the call itself fails
     */
    public <V> Try<V> executeTry(CheckedSupplier<V> supplier) {
        try {
            return Try.success(rateLimiter.executeCheckedSupplier(supplier::get));
        } catch (Throwable t) {
            return Try.failure(t);
        }
    }

    /**
     * Executes the supplier through the rate limiter.
     *
     * @return {@code Ok(value)} on success, {@code Err(cause)} on any failure
     */
    public <V> Result<V, Throwable> executeResult(CheckedSupplier<V> supplier) {
        return executeTry(supplier).toResult();
    }

    /**
     * Executes the supplier through the rate limiter, surfacing rate-limit rejections
     * as a typed error.
     *
     * @return {@code Ok(value)} on success,
     *         {@code Err(RequestNotPermitted)} when the rate limit is exceeded;
     *         other exceptions from the call propagate as unchecked
     */
    public <V> Result<V, RequestNotPermitted> executeResultTyped(CheckedSupplier<V> supplier) {
        try {
            return Result.ok(rateLimiter.executeCheckedSupplier(supplier::get));
        } catch (RequestNotPermitted e) {
            return Result.err(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
