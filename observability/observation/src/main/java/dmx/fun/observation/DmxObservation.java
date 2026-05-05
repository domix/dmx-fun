package dmx.fun.observation;

import dmx.fun.CheckedSupplier;
import dmx.fun.Result;
import dmx.fun.Try;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * dmx-fun adapter for the Micrometer Observation API.
 *
 * <p>Instruments {@link Try} and {@link Result} executions automatically, opening
 * a named {@link Observation} around each call. Depending on the handlers registered
 * in the {@link ObservationRegistry}, a single call records both metrics
 * (counters, timers) and distributed tracing spans — without manual bookkeeping:
 *
 * <pre>{@code
 * DmxObservation dmx = DmxObservation.of(observationRegistry);
 *
 * Try<Response> result = dmx.observeTry("http.client.get",
 *     () -> httpClient.get(url)
 * );
 * }</pre>
 *
 * <h2>Signals recorded per call</h2>
 * <ul>
 *   <li>Observation named after the {@code name} argument.</li>
 *   <li>{@code outcome} low-cardinality key — {@code "success"} or {@code "failure"}.</li>
 *   <li>{@code exception} low-cardinality key — classifier label of the cause
 *       (failure only).</li>
 *   <li>Observation marked as error via {@link Observation#error} (failure only).</li>
 * </ul>
 *
 * <h2>Exception key cardinality</h2>
 * <p>By default the {@code exception} key uses {@code getClass().getSimpleName()}, which is
 * unbounded when arbitrary third-party exceptions can appear — a violation of Micrometer's
 * {@code lowCardinalityKeyValue} contract (≤ 100 distinct values). <strong>In production,
 * supply an explicit {@code exceptionClassifier}</strong> via
 * {@link #of(ObservationRegistry, Function)}:
 *
 * <pre>{@code
 * DmxObservation dmx = DmxObservation.of(observationRegistry, cause ->
 *     switch (cause) {
 *         case IOException _           -> "io";
 *         case TimeoutException _      -> "timeout";
 *         default                      -> "other";
 *     }
 * );
 * }</pre>
 *
 * <p>Spring Boot 3.0+ autoconfigures an {@link ObservationRegistry} with metrics and
 * tracing handlers when both {@code micrometer-core} and a Micrometer Tracing bridge
 * are on the classpath. When {@code fun-spring-boot} is also present, a
 * {@link DmxObservation} bean is registered automatically — inject it directly.
 *
 * <p>For a fluent builder alternative see {@link DmxObserved}.
 *
 * <p>Requires {@code micrometer-core} ≥ 1.10 on the classpath at runtime.
 */
@NullMarked
public final class DmxObservation {

    static final String OUTCOME_KEY     = "outcome";
    static final String EXCEPTION_KEY   = "exception";
    static final String OUTCOME_SUCCESS = "success";
    static final String OUTCOME_FAILURE = "failure";

    private static final Function<Throwable, String> DEFAULT_CLASSIFIER =
        t -> t.getClass().getSimpleName();

    private final ObservationRegistry registry;
    private final Function<Throwable, String> exceptionClassifier;

    private DmxObservation(ObservationRegistry registry,
                           Function<Throwable, String> exceptionClassifier) {
        this.registry = registry;
        this.exceptionClassifier = exceptionClassifier;
    }

    /**
     * Creates an instance bound to the given {@link ObservationRegistry}.
     *
     * <p><strong>Warning:</strong> uses {@code getClass().getSimpleName()} as the
     * {@code exception} key — an unsafe default in production where arbitrary exceptions
     * may appear. Prefer {@link #of(ObservationRegistry, Function)} with an explicit classifier.
     *
     * @param registry the registry to create observations with; must not be {@code null}
     * @return a new {@code DmxObservation} bound to the given registry
     */
    public static DmxObservation of(ObservationRegistry registry) {
        return new DmxObservation(Objects.requireNonNull(registry, "registry"), DEFAULT_CLASSIFIER);
    }

    /**
     * Creates an instance bound to the given {@link ObservationRegistry} and exception classifier.
     *
     * <p>The {@code exceptionClassifier} maps each failure {@link Throwable} to the value
     * written to the {@code exception} low-cardinality key. It must return a value from a
     * small, bounded set (≤ 100 distinct values) to satisfy Micrometer's contract.
     *
     * @param registry            the registry to create observations with; must not be {@code null}
     * @param exceptionClassifier maps a failure cause to its {@code exception} key value;
     *                            must not be null, must return bounded values
     * @return a new {@code DmxObservation} bound to the given registry and classifier
     */
    public static DmxObservation of(ObservationRegistry registry,
                                    Function<Throwable, String> exceptionClassifier) {
        return new DmxObservation(
            Objects.requireNonNull(registry, "registry"),
            Objects.requireNonNull(exceptionClassifier, "exceptionClassifier")
        );
    }

    /**
     * Executes {@code supplier} inside a new {@link Observation} named {@code name}.
     *
     * <p>The observation is tagged with {@code outcome=success} on success, or
     * {@code outcome=failure} plus {@code exception=<classifier result>} (the value
     * returned by {@code exceptionClassifier.apply(cause)}) and marked as error on failure.
     * The observation is always stopped before this method returns.
     *
     * @param <V>      the value type returned on success
     * @param name     the observation name; must not be {@code null}
     * @param supplier the operation to execute; must not be {@code null}
     * @return {@code Success(value)} on success, {@code Failure(cause)} on any exception
     */
    public <V> Try<V> observeTry(String name, CheckedSupplier<V> supplier) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(supplier, "supplier");

        var observation = Observation
            .createNotStarted(name, registry)
            .start();

        Try<V> result;

        try (var _ = observation.openScope()) {
            result = Try.of(supplier)
                .onSuccess(_ -> observation.lowCardinalityKeyValue(OUTCOME_KEY, OUTCOME_SUCCESS))
                .onFailure(cause -> {
                    observation.lowCardinalityKeyValue(OUTCOME_KEY, OUTCOME_FAILURE);
                    observation.lowCardinalityKeyValue(EXCEPTION_KEY, exceptionClassifier.apply(cause));
                    observation.error(cause);
                });
        } finally {
            observation.stop();
        }
        return result;
    }

    /**
     * Executes {@code supplier} inside a new {@link Observation} named {@code name}.
     *
     * <p>Equivalent to {@link #observeTry} converted to a {@link Result}.
     *
     * @param <V>      the value type returned on success
     * @param name     the observation name; must not be {@code null}
     * @param supplier the operation to execute; must not be {@code null}
     * @return {@code Ok(value)} on success, {@code Err(cause)} on any exception
     */
    public <V> Result<V, Throwable> observeResult(String name, CheckedSupplier<V> supplier) {
        return observeTry(name, supplier)
            .toResult();
    }
}
