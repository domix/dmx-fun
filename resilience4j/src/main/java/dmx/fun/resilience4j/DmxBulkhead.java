package dmx.fun.resilience4j;

import dmx.fun.CheckedSupplier;
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

    /** Wraps an existing {@link Bulkhead} instance. */
    public static DmxBulkhead of(Bulkhead bulkhead) {
        return new DmxBulkhead(Objects.requireNonNull(bulkhead, "bulkhead"));
    }

    /** Creates a new {@link Bulkhead} from the given name and config, then wraps it. */
    public static DmxBulkhead of(String name, BulkheadConfig config) {
        return new DmxBulkhead(Bulkhead.of(name, config));
    }

    /**
     * Executes the supplier through the bulkhead.
     *
     * @return {@code Success(value)} on success,
     *         {@code Failure(BulkheadFullException)} when the bulkhead is saturated,
     *         or {@code Failure(cause)} when the call itself fails
     */
    public <V> Try<V> executeTry(CheckedSupplier<V> supplier) {
        try {
            return Try.success(bulkhead.executeCheckedSupplier(supplier::get));
        } catch (Throwable t) {
            return Try.failure(t);
        }
    }

    /**
     * Executes the supplier through the bulkhead.
     *
     * @return {@code Ok(value)} on success, {@code Err(cause)} on any failure
     */
    public <V> Result<V, Throwable> executeResult(CheckedSupplier<V> supplier) {
        return executeTry(supplier).toResult();
    }

    /**
     * Executes the supplier through the bulkhead, surfacing bulkhead-full rejections
     * as a typed error.
     *
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
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
