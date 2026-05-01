package dmx.fun.observation;

import dmx.fun.CheckedSupplier;
import dmx.fun.Result;
import dmx.fun.Try;
import io.micrometer.observation.ObservationRegistry;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for observed dmx-fun operations.
 *
 * <p>Provides a chainable alternative to {@link DmxObservation} when the observation
 * name and registry are configured at different points:
 *
 * <pre>{@code
 * DmxObserved.of("payment.charge")
 *     .registry(observationRegistry)
 *     .observeTry(() -> stripe.charge(amount));
 * }</pre>
 *
 * <p>The same signals are recorded as with {@link DmxObservation#observeTry}: the
 * observation is named and tagged with {@code outcome}, and on failure the
 * {@code exception} key is set and the observation is marked as error.
 *
 * <p>This builder is mutable and not thread-safe; do not share one instance across
 * concurrent per-call reconfigurations.
 */
@NullMarked
public final class DmxObserved {

    private final String name;
    private @Nullable DmxObservation observation;

    private DmxObserved(String name) {
        this.name = name;
    }

    /**
     * Creates a builder for the given observation name.
     *
     * @param name the observation name; must not be {@code null}
     * @return a new {@code DmxObserved} builder
     */
    public static DmxObserved of(String name) {
        return new DmxObserved(Objects.requireNonNull(name, "name"));
    }

    /**
     * Sets the {@link ObservationRegistry} to create observations with.
     *
     * @param registry the registry to use; must not be {@code null}
     * @return this builder
     */
    public DmxObserved registry(ObservationRegistry registry) {
        this.observation = DmxObservation.of(registry);
        return this;
    }

    /**
     * Executes {@code supplier} inside a new observation.
     *
     * @param <V>      the value type returned on success
     * @param supplier the operation to execute; must not be {@code null}
     * @return {@code Success(value)} on success, {@code Failure(cause)} on any exception
     * @throws IllegalStateException if {@link #registry} was not set
     */
    public <V> Try<V> observeTry(CheckedSupplier<V> supplier) {
        return requireObservation()
            .observeTry(name, supplier);
    }

    /**
     * Executes {@code supplier} inside a new observation.
     *
     * @param <V>      the value type returned on success
     * @param supplier the operation to execute; must not be {@code null}
     * @return {@code Ok(value)} on success, {@code Err(cause)} on any exception
     * @throws IllegalStateException if {@link #registry} was not set
     */
    public <V> Result<V, Throwable> observeResult(CheckedSupplier<V> supplier) {
        return requireObservation()
            .observeResult(name, supplier);
    }

    private DmxObservation requireObservation() {
        if (observation == null) {
            throw new IllegalStateException(
                "registry must be set before observing — call .registry(registry) first");
        }
        return observation;
    }
}
