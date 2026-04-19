package dmx.fun.resilience4j;

import dmx.fun.CheckedSupplier;
import dmx.fun.Result;
import dmx.fun.Try;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * dmx-fun adapter for Resilience4J {@link CircuitBreaker}.
 *
 * <p>Executes a supplier through the circuit breaker and returns a dmx-fun type instead of
 * throwing. Configure the circuit breaker using the native Resilience4J API:
 *
 * <pre>{@code
 * DmxCircuitBreaker cb = DmxCircuitBreaker.of("orders", CircuitBreakerConfig.ofDefaults());
 *
 * Try<Response>                               r1 = cb.executeTry(() -> orderService.place(cmd));
 * Result<Response, Throwable>                 r2 = cb.executeResult(() -> orderService.place(cmd));
 * Result<Response, CallNotPermittedException> r3 = cb.executeResultTyped(() -> orderService.place(cmd));
 * }</pre>
 */
@NullMarked
public final class DmxCircuitBreaker {

    private final CircuitBreaker circuitBreaker;

    private DmxCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    /** Wraps an existing {@link CircuitBreaker} instance. */
    public static DmxCircuitBreaker of(CircuitBreaker circuitBreaker) {
        return new DmxCircuitBreaker(Objects.requireNonNull(circuitBreaker, "circuitBreaker"));
    }

    /** Creates a new {@link CircuitBreaker} from the given name and config, then wraps it. */
    public static DmxCircuitBreaker of(String name, CircuitBreakerConfig config) {
        return new DmxCircuitBreaker(CircuitBreaker.of(name, config));
    }

    /**
     * Executes the supplier through the circuit breaker.
     *
     * @return {@code Success(value)} on success,
     *         {@code Failure(CallNotPermittedException)} when the circuit is open,
     *         or {@code Failure(cause)} when the call itself fails
     */
    public <V> Try<V> executeTry(CheckedSupplier<V> supplier) {
        try {
            return Try.success(circuitBreaker.executeCheckedSupplier(supplier::get));
        } catch (Throwable t) {
            return Try.failure(t);
        }
    }

    /**
     * Executes the supplier through the circuit breaker.
     *
     * @return {@code Ok(value)} on success, {@code Err(cause)} on any failure
     */
    public <V> Result<V, Throwable> executeResult(CheckedSupplier<V> supplier) {
        return executeTry(supplier).toResult();
    }

    /**
     * Executes the supplier through the circuit breaker, surfacing circuit-open rejections
     * as a typed error.
     *
     * <p>Use this overload when you want to pattern-match on the exact resilience failure:
     * <pre>{@code
     * cb.executeResultTyped(() -> service.call())
     *   .fold(
     *       ex  -> handleOpen(ex),   // CallNotPermittedException
     *       val -> process(val)
     *   );
     * }</pre>
     *
     * @return {@code Ok(value)} on success,
     *         {@code Err(CallNotPermittedException)} when the circuit is open;
     *         other exceptions from the call propagate as unchecked
     */
    public <V> Result<V, CallNotPermittedException> executeResultTyped(CheckedSupplier<V> supplier) {
        try {
            return Result.ok(circuitBreaker.executeCheckedSupplier(supplier::get));
        } catch (CallNotPermittedException e) {
            return Result.err(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
