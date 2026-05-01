package dmx.fun.tracing;

import dmx.fun.CheckedSupplier;
import dmx.fun.Result;
import dmx.fun.Try;
import io.micrometer.tracing.Tracer;
import java.util.Objects;
import java.util.function.Function;
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
 * <p>Use {@link #exceptionClassifier} to control the {@code exception} tag value and
 * keep tag cardinality bounded in tracing backends. See
 * {@link DmxTracing#of(Tracer, java.util.function.Function)} for details.
 *
 * <p>This builder is mutable and not thread-safe; do not share one instance across
 * concurrent per-call reconfigurations.
 */
@NullMarked
public final class DmxTraced {

    private final String name;
    private @Nullable Tracer tracerRef;
    private @Nullable Function<Throwable, String> exceptionClassifier;

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
        this.tracerRef = Objects.requireNonNull(tracer, "tracer");
        return this;
    }

    /**
     * Sets the function that maps each failure cause to its {@code exception} tag value.
     *
     * <p>The classifier should return a value from a small, bounded set to keep tag
     * cardinality predictable in tracing backends. When not set, defaults to
     * {@code getClass().getSimpleName()} — an unsafe default in production.
     *
     * @param classifier maps a failure cause to its {@code exception} tag value;
     *                   must not be null, should return bounded values
     * @return this builder
     */
    public DmxTraced exceptionClassifier(Function<Throwable, String> classifier) {
        this.exceptionClassifier = Objects.requireNonNull(classifier, "exceptionClassifier");
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
        if (tracerRef == null) {
            throw new IllegalStateException(
                "tracer must be set before tracing — call .tracer(tracer) first");
        }
        return exceptionClassifier != null
            ? DmxTracing.of(tracerRef, exceptionClassifier)
            : DmxTracing.of(tracerRef);
    }
}
