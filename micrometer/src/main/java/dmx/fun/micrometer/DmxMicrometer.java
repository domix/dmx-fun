package dmx.fun.micrometer;

import dmx.fun.CheckedSupplier;
import dmx.fun.Result;
import dmx.fun.Try;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
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
 *   <li>{@code {name}.failure} — Counter tagged with {@code exception=<ClassName>} (on failure only)</li>
 * </ul>
 *
 * <p>For a fluent builder alternative see {@link DmxMetered}.
 */
@NullMarked
public final class DmxMicrometer {

    private final MeterRegistry registry;

    private DmxMicrometer(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Creates an instance bound to the given {@link MeterRegistry}. */
    public static DmxMicrometer of(MeterRegistry registry) {
        return new DmxMicrometer(Objects.requireNonNull(registry, "registry"));
    }

    /**
     * Executes the supplier and records metrics under {@code name}.
     *
     * @return {@code Success(value)} on success, {@code Failure(cause)} on any exception
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

        String outcome = result.isSuccess() ? "success" : "failure";
        Tags withOutcome = tags.and("outcome", outcome);

        Counter.builder(name + ".count").tags(withOutcome).register(registry).increment();
        sample.stop(Timer.builder(name + ".duration").tags(withOutcome).register(registry));

        if (result.isFailure()) {
            Counter.builder(name + ".failure")
                .tags(tags)
                .tag("exception", result.getCause().getClass().getSimpleName())
                .register(registry)
                .increment();
        }

        return result;
    }

    /**
     * Executes the supplier and records metrics under {@code name}.
     *
     * @return {@code Ok(value)} on success, {@code Err(cause)} on any exception
     */
    public <V> Result<V, Throwable> recordResult(String name, Tags tags, CheckedSupplier<V> supplier) {
        return recordTry(name, tags, supplier).toResult();
    }
}
