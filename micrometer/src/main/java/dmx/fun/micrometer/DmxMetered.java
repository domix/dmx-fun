package dmx.fun.micrometer;

import dmx.fun.CheckedSupplier;
import dmx.fun.Result;
import dmx.fun.Try;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for instrumented dmx-fun operations.
 *
 * <p>Provides a chainable alternative to {@link DmxMicrometer} when the metric name,
 * tags, and registry are configured at different points:
 *
 * <pre>{@code
 * DmxMetered.of("payment.charge")
 *     .tags(Tags.of("provider", "stripe"))
 *     .registry(registry)
 *     .recordTry(() -> stripe.charge(amount));
 * }</pre>
 *
 * <p>The same metrics are recorded as with {@link DmxMicrometer#recordTry}:
 * {@code {name}.count}, {@code {name}.duration}, and {@code {name}.failure}.
 * This builder is mutable and not thread-safe; avoid sharing one instance for concurrent per-call reconfiguration.
 */
@NullMarked
public final class DmxMetered {

    private final String name;
    private Tags tags = Tags.empty();
    private @Nullable DmxMicrometer micrometer;

    private DmxMetered(String name) {
        this.name = name;
    }

    /**
     * Creates a builder for the given metric name.
     *
     * @param name the base metric name; must not be null
     * @return a new {@code DmxMetered} builder
     */
    public static DmxMetered of(String name) {
        return new DmxMetered(Objects.requireNonNull(name, "name"));
    }

    /**
     * Sets the tags to attach to all metrics for this operation.
     *
     * @param tags the tags to apply; must not be null
     * @return this builder
     */
    public DmxMetered tags(Tags tags) {
        this.tags = Objects.requireNonNull(tags, "tags");
        return this;
    }

    /**
     * Sets the {@link MeterRegistry} to register metrics with.
     *
     * @param registry the registry to use; must not be null
     * @return this builder
     */
    public DmxMetered registry(MeterRegistry registry) {
        this.micrometer = DmxMicrometer.of(Objects.requireNonNull(registry, "registry"));
        return this;
    }

    /**
     * Executes the supplier and records metrics.
     *
     * @param <V> the value type returned on success
     * @param supplier the operation to execute; must not be null
     * @return {@code Success(value)} on success, {@code Failure(cause)} on any exception
     * @throws IllegalStateException if {@link #registry} was not set
     */
    public <V> Try<V> recordTry(CheckedSupplier<V> supplier) {
        return requireMicrometer().recordTry(name, tags, supplier);
    }

    /**
     * Executes the supplier and records metrics.
     *
     * @param <V> the value type returned on success
     * @param supplier the operation to execute; must not be null
     * @return {@code Ok(value)} on success, {@code Err(cause)} on any exception
     * @throws IllegalStateException if {@link #registry} was not set
     */
    public <V> Result<V, Throwable> recordResult(CheckedSupplier<V> supplier) {
        return requireMicrometer().recordResult(name, tags, supplier);
    }

    private DmxMicrometer requireMicrometer() {
        if (micrometer == null) {
            throw new IllegalStateException("registry must be set before recording — call .registry(meterRegistry) first");
        }
        return micrometer;
    }
}
