package dmx.fun.resilience4j;

import dmx.fun.CheckedSupplier;
import dmx.fun.NonFatal;
import dmx.fun.Result;
import dmx.fun.Try;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * dmx-fun adapter for Resilience4J {@link Bulkhead}.
 *
 * <p>Executes a supplier through the bulkhead and returns a dmx-fun type instead of
 * throwing. Configure the bulkhead using the native Resilience4J API:
 *
 * <pre>{@code
 * DmxBulkhead bh = DmxBulkhead.of("db-pool", BulkheadConfig.ofDefaults());
 *
 * Try<User>                           r1 = bh.executeTry(() -> userRepo.findById(id));
 * Result<User, Throwable>             r2 = bh.executeResult(() -> userRepo.findById(id));
 * Result<User, BulkheadFullException> r3 = bh.executeResultTyped(() -> userRepo.findById(id));
 * }</pre>
 */
@NullMarked
public final class DmxBulkhead {

    private final Bulkhead bulkhead;

    private DmxBulkhead(Bulkhead bulkhead) {
        this.bulkhead = bulkhead;
    }

    /**
     * Wraps an existing {@link Bulkhead} instance.
     *
     * @param bulkhead the Resilience4J bulkhead to wrap
     * @return a new {@code DmxBulkhead} backed by the given bulkhead
     */
    public static DmxBulkhead of(Bulkhead bulkhead) {
        return new DmxBulkhead(Objects.requireNonNull(bulkhead, "bulkhead"));
    }

    /**
     * Creates a new {@link Bulkhead} from the given name and config, then wraps it.
     *
     * @param name   the bulkhead name
     * @param config the bulkhead configuration
     * @return a new {@code DmxBulkhead} backed by the created bulkhead
     */
    public static DmxBulkhead of(String name, BulkheadConfig config) {
        return new DmxBulkhead(Bulkhead.of(name, config));
    }

    /**
     * Executes the supplier through the bulkhead.
     *
     * <p>Fatal throwables are never captured: if the call fails with one — per
     * {@link NonFatal}, including one reachable through the cause chain or suppressed
     * exceptions — it is rethrown via {@link NonFatal#rethrowIfFatal(Throwable)}, with the
     * interrupt flag restored when an {@link InterruptedException} is involved.
     *
     * @param <V>      the value type
     * @param supplier the operation to execute
     * @return {@code Success(value)} on success,
     *         {@code Failure(BulkheadFullException)} when the bulkhead is saturated,
     *         or {@code Failure(cause)} when the call itself fails
     */
    public <V> Try<V> executeTry(CheckedSupplier<V> supplier) {
        try {
            return Try.success(bulkhead.executeCheckedSupplier(supplier::get));
        } catch (Throwable t) {
            NonFatal.rethrowIfFatal(t);
            return Try.failure(t);
        }
    }

    /**
     * Executes the supplier through the bulkhead.
     *
     * <p>Fatal throwables are rethrown rather than returned as {@code Err} — see
     * {@link #executeTry(CheckedSupplier)}.
     *
     * @param <V>      the value type
     * @param supplier the operation to execute
     * @return {@code Ok(value)} on success, {@code Err(cause)} on any failure
     */
    public <V> Result<V, Throwable> executeResult(CheckedSupplier<V> supplier) {
        return executeTry(supplier).toResult();
    }

    /**
     * Executes the supplier through the bulkhead, surfacing bulkhead-full rejections
     * as a typed error.
     *
     * <p>A checked exception from the call is rethrown wrapped in a
     * {@link RuntimeException}; if it is, or was caused by, an
     * {@link InterruptedException}, the current thread's interrupt flag is restored
     * before the wrap.
     *
     * @param <V>      the value type
     * @param supplier the operation to execute
     * @return {@code Ok(value)} on success,
     *         {@code Err(BulkheadFullException)} when the bulkhead is saturated;
     *         other exceptions from the call propagate as unchecked
     */
    public <V> Result<V, BulkheadFullException> executeResultTyped(CheckedSupplier<V> supplier) {
        try {
            return Result.ok(bulkhead.executeCheckedSupplier(supplier::get));
        } catch (BulkheadFullException e) {
            return Result.err(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw Unchecked.wrap(t);
        }
    }
}
