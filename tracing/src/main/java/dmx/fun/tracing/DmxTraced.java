package dmx.fun.tracing;

import dmx.fun.CheckedSupplier;
import dmx.fun.Result;
import dmx.fun.Try;
import io.micrometer.tracing.Tracer;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for traced dmx-fun operations.
 *
 * <p>Provides a chainable alternative to {@link DmxTracing} when the span name and
 * tracer are configured at different points:
 *
 * <pre>{@code
 * DmxTraced.of("payment.charge")
 *     .tracer(tracer)
 *     .traceTry(() -> stripe.charge(amount));
 * }</pre>
 *
 * <p>The same signals are recorded as with {@link DmxTracing#traceTry}: the span is
 * named, tagged with {@code outcome}, and on failure the {@code exception} tag is set
 * and the span is marked as error.
 *
 * <p>This builder is mutable and not thread-safe; do not share one instance across
 * concurrent per-call reconfigurations.
 */
@NullMarked
public final class DmxTraced {

    private final String name;
    private @Nullable DmxTracing tracing;

    private DmxTraced(String name) {
        this.name = name;
    }

    /**
     * Creates a builder for the given span name.
     *
     * @param name the span name; must not be {@code null}
     * @return a new {@code DmxTraced} builder
     */
    public static DmxTraced of(String name) {
        return new DmxTraced(Objects.requireNonNull(name, "name"));
    }

    /**
     * Sets the {@link Tracer} to open spans with.
     *
     * @param tracer the tracer to use; must not be {@code null}
     * @return this builder
     */
    public DmxTraced tracer(Tracer tracer) {
        this.tracing = DmxTracing.of(tracer);
        return this;
    }

    /**
     * Executes {@code supplier} inside a new span.
     *
     * @param <V>      the value type returned on success
     * @param supplier the operation to execute; must not be {@code null}
     * @return {@code Success(value)} on success, {@code Failure(cause)} on any exception
     * @throws IllegalStateException if {@link #tracer} was not set
     */
    public <V> Try<V> traceTry(CheckedSupplier<V> supplier) {
        return requireTracing()
            .traceTry(name, supplier);
    }

    /**
     * Executes {@code supplier} inside a new span.
     *
     * @param <V>      the value type returned on success
     * @param supplier the operation to execute; must not be {@code null}
     * @return {@code Ok(value)} on success, {@code Err(cause)} on any exception
     * @throws IllegalStateException if {@link #tracer} was not set
     */
    public <V> Result<V, Throwable> traceResult(CheckedSupplier<V> supplier) {
        return requireTracing()
            .traceResult(name, supplier);
    }

    private DmxTracing requireTracing() {
        if (tracing == null) {
            throw new IllegalStateException(
                "tracer must be set before tracing — call .tracer(tracer) first");
        }
        return tracing;
    }
}
