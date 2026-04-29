package dmx.fun.observation;

import dmx.fun.CheckedSupplier;
import dmx.fun.Result;
import dmx.fun.Try;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Objects;
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
 *   <li>{@code exception} low-cardinality key — simple class name of the cause
 *       (failure only).</li>
 *   <li>Observation marked as error via {@link Observation#error} (failure only).</li>
 * </ul>
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

    private final ObservationRegistry registry;

    private DmxObservation(ObservationRegistry registry) {
        this.registry = registry;
    }

    /**
     * Creates an instance bound to the given {@link ObservationRegistry}.
     *
     * @param registry the registry to create observations with; must not be {@code null}
     * @return a new {@code DmxObservation} bound to the given registry
     */
    public static DmxObservation of(ObservationRegistry registry) {
        return new DmxObservation(Objects.requireNonNull(registry, "registry"));
    }

    /**
     * Executes {@code supplier} inside a new {@link Observation} named {@code name}.
     *
     * <p>The observation is tagged with {@code outcome=success} on success, or
     * {@code outcome=failure} plus {@code exception=<SimpleClassName>} and marked as
     * error on failure. The observation is always stopped before this method returns.
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

        try (var ignored = observation.openScope()) {
            result = Try.of(supplier)
                .onSuccess(_ -> observation.lowCardinalityKeyValue("outcome", "success"))
                .onFailure(cause -> {
                    observation.lowCardinalityKeyValue("outcome", "failure");
                    observation.lowCardinalityKeyValue("exception", cause.getClass().getSimpleName());
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
        return observeTry(name, supplier).toResult();
    }
}
