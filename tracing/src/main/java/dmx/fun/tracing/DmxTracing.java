package dmx.fun.tracing;

import dmx.fun.CheckedSupplier;
import dmx.fun.Result;
import dmx.fun.Try;
import io.micrometer.tracing.Tracer;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * dmx-fun adapter for Micrometer Tracing.
 *
 * <p>Instruments {@link Try} and {@link Result} executions automatically, opening
 * a named span around each call and recording outcome without manual span management:
 *
 * <pre>{@code
 * DmxTracing dmx = DmxTracing.of(tracer);  // io.micrometer.tracing.Tracer
 *
 * Try<Response> result = dmx.traceTry("http.client.get",
 *     () -> httpClient.get(url)
 * );
 * }</pre>
 *
 * <h2>Signals recorded per call</h2>
 * <ul>
 *   <li>Span named after the {@code name} argument.</li>
 *   <li>{@code outcome} tag — {@code "success"} or {@code "failure"}.</li>
 *   <li>{@code exception} tag — simple class name of the cause (failure only).</li>
 *   <li>Span marked as error via {@link io.micrometer.tracing.Span#error} (failure only).</li>
 * </ul>
 *
 * <p>For a fluent builder alternative see {@link DmxTraced}.
 *
 * <p>Requires {@code micrometer-tracing} on the classpath at runtime, plus a backend
 * bridge ({@code micrometer-tracing-bridge-otel} or {@code micrometer-tracing-bridge-brave}).
 * Spring Boot auto-configures a {@link Tracer} bean when either bridge is present.
 */
@NullMarked
public final class DmxTracing {

    private final Tracer tracer;

    private DmxTracing(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Creates an instance bound to the given {@link Tracer}.
     *
     * @param tracer the tracer to open spans with; must not be {@code null}
     * @return a new {@code DmxTracing} bound to the given tracer
     */
    public static DmxTracing of(Tracer tracer) {
        return new DmxTracing(Objects.requireNonNull(tracer, "tracer"));
    }

    /**
     * Executes {@code supplier} inside a new span named {@code name}.
     *
     * <p>The span is tagged with {@code outcome=success} on success, or
     * {@code outcome=failure} plus {@code exception=<SimpleClassName>} and marked as
     * error on failure. The span is always ended before this method returns.
     *
     * @param <V>      the value type returned on success
     * @param name     the span name; must not be {@code null}
     * @param supplier the operation to execute; must not be {@code null}
     * @return {@code Success(value)} on success, {@code Failure(cause)} on any exception
     */
    public <V> Try<V> traceTry(String name, CheckedSupplier<V> supplier) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(supplier, "supplier");

        var span = tracer
            .nextSpan()
            .name(name)
            .start();

        try (var _ = tracer.withSpan(span)) {
            return Try.of(supplier)
                .onSuccess(_ -> span.tag("outcome", "success"))
                .onFailure(cause -> {
                    span.tag("outcome", "failure");
                    span.tag("exception", cause.getClass().getSimpleName());
                    span.error(cause);
                });
        } finally {
            span.end();
        }
    }

    /**
     * Executes {@code supplier} inside a new span named {@code name}.
     *
     * <p>Equivalent to {@link #traceTry} converted to a {@link Result}.
     *
     * @param <V>      the value type returned on success
     * @param name     the span name; must not be {@code null}
     * @param supplier the operation to execute; must not be {@code null}
     * @return {@code Ok(value)} on success, {@code Err(cause)} on any exception
     */
    public <V> Result<V, Throwable> traceResult(String name, CheckedSupplier<V> supplier) {
        return traceTry(name, supplier).toResult();
    }
}
