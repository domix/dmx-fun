package dmx.fun.micrometer;

import dmx.fun.CheckedSupplier;
import dmx.fun.NonFatal;
import dmx.fun.Result;
import dmx.fun.Try;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * dmx-fun adapter for Micrometer.
 *
 * <p>Instruments {@link Try} and {@link Result} executions automatically, recording
 * success/failure counts and execution latency without manual metric tracking:
 *
 * <pre>{@code
 * DmxMicrometer dmx = DmxMicrometer.of(registry);
 *
 * Try<Response> result = dmx.recordTry("http.client.get",
 *     Tags.of("endpoint", "/users"),
 *     () -> httpClient.get(url)
 * );
 * }</pre>
 *
 * <h2>Metrics recorded per call</h2>
 * <ul>
 *   <li>{@code {name}.count} — Counter tagged with {@code outcome=success|failure}</li>
 *   <li>{@code {name}.duration} — Timer tagged with {@code outcome=success|failure}</li>
 *   <li>{@code {name}.failure} — Counter tagged with {@code exception=<label>} (on failure only)</li>
 * </ul>
 *
 * <h2>Exception tag cardinality</h2>
 * <p>By default the {@code exception} tag uses {@code getClass().getSimpleName()}, which is
 * unbounded when arbitrary third-party exceptions appear at runtime — a violation of
 * Micrometer's low-cardinality contract. <strong>In production, supply an explicit
 * {@code exceptionClassifier}</strong> that maps every reachable exception type to one of a
 * small, fixed set of labels:
 *
 * <pre>{@code
 * DmxMicrometer dmx = DmxMicrometer.of(registry, cause ->
 *     switch (cause) {
 *         case IOException __          -> "io";
 *         case TimeoutException __     -> "timeout";
 *         default                      -> "other";
 *     }
 * );
 * }</pre>
 *
 * <p>For a fluent builder alternative see {@link DmxMetered}.
 */
@NullMarked
public final class DmxMicrometer {

    private static final Function<Throwable, String> DEFAULT_CLASSIFIER =
        t -> t.getClass().getSimpleName();

    private final MeterRegistry registry;
    private final Function<Throwable, String> exceptionClassifier;

    private DmxMicrometer(MeterRegistry registry, Function<Throwable, String> exceptionClassifier) {
        this.registry = registry;
        this.exceptionClassifier = exceptionClassifier;
    }

    /**
     * Creates an instance bound to the given {@link MeterRegistry}.
     *
     * <p><strong>Warning:</strong> uses {@code getClass().getSimpleName()} as the
     * {@code exception} tag — an unsafe default in production where arbitrary exceptions
     * may appear. Prefer {@link #of(MeterRegistry, Function)} with an explicit classifier.
     *
     * @param registry the registry to record metrics into; must not be null
     * @return a new {@code DmxMicrometer} bound to the given registry
     */
    public static DmxMicrometer of(MeterRegistry registry) {
        return new DmxMicrometer(Objects.requireNonNull(registry, "registry"), DEFAULT_CLASSIFIER);
    }

    /**
     * Creates an instance bound to the given {@link MeterRegistry} and exception classifier.
     *
     * <p>The {@code exceptionClassifier} maps each failure {@link Throwable} to the value
     * written to the {@code exception} tag. It must return a value from a small, bounded set
     * to satisfy Micrometer's low-cardinality requirement.
     *
     * @param registry            the registry to record metrics into; must not be null
     * @param exceptionClassifier maps a failure cause to its {@code exception} tag value;
     *                            must not be null, must return bounded values
     * @return a new {@code DmxMicrometer} bound to the given registry and classifier
     */
    public static DmxMicrometer of(MeterRegistry registry,
                                   Function<Throwable, String> exceptionClassifier) {
        return new DmxMicrometer(
            Objects.requireNonNull(registry, "registry"),
            Objects.requireNonNull(exceptionClassifier, "exceptionClassifier")
        );
    }

    /**
     * Executes the supplier and records metrics under {@code name}.
     *
     * <p>Fatal throwables are never captured: if the supplier fails with one — per
     * {@link NonFatal}, including one reachable through the cause chain or suppressed
     * exceptions — the {@code failure} outcome, the duration and the exception counter are
     * recorded first, and then it is rethrown via {@link NonFatal#rethrowIfFatal(Throwable)},
     * with the interrupt flag restored when an {@link InterruptedException} is involved.
     * The fatal throwable still propagates if recording itself throws — from the
     * {@code exceptionClassifier}, for instance — and it takes precedence over that exception.
     * After a non-fatal result, an exception thrown while recording propagates as is.
     *
     * @param <V> the value type returned on success
     * @param name the base metric name; must not be null
     * @param tags additional tags to attach to all metrics; must not be null
     * @param supplier the operation to execute; must not be null
     * @return {@code Success(value)} on success, {@code Failure(cause)} on any non-fatal
     *         exception
     */
    public <V> Try<V> recordTry(String name, Tags tags, CheckedSupplier<V> supplier) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(tags, "tags");
        Objects.requireNonNull(supplier, "supplier");

        var sample = Timer.start(registry);
        Try<V> result;
        try {
            result = Try.success(supplier.get());
        } catch (Throwable t) {
            result = Try.failure(t);
        }

        try {
            String outcome = result.isSuccess() ? "success" : "failure";
            Tags withOutcome = tags.and("outcome", outcome);

            Counter.builder(name + ".count").tags(withOutcome).register(registry).increment();
            sample.stop(Timer.builder(name + ".duration").tags(withOutcome).register(registry));

            if (result.isFailure()) {
                Counter.builder(name + ".failure")
                    .tags(tags)
                    .tag("exception", exceptionClassifier.apply(result.getCause()))
                    .register(registry)
                    .increment();
            }
        } finally {
            // Recorded first, so a fatal failure is visible in metrics before it propagates.
            // In a finally so a fatal still wins if recording itself throws (e.g. the
            // classifier); for a non-fatal result this is a no-op and that exception propagates.
            result.rethrowFatal();
        }
        return result;
    }

    /**
     * Executes the supplier and records metrics under {@code name}.
     *
     * <p>Fatal throwables are rethrown rather than returned as {@code Err} — see
     * {@link #recordTry(String, Tags, CheckedSupplier)}.
     *
     * @param <V> the value type returned on success
     * @param name the base metric name; must not be null
     * @param tags additional tags to attach to all metrics; must not be null
     * @param supplier the operation to execute; must not be null
     * @return {@code Ok(value)} on success, {@code Err(cause)} on any non-fatal exception
     */
    public <V> Result<V, Throwable> recordResult(String name, Tags tags, CheckedSupplier<V> supplier) {
        return recordTry(name, tags, supplier).toResult();
    }
}
