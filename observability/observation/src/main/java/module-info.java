/**
 * Micrometer Observation adapter for dmx-fun.
 *
 * <p>Instruments {@link dmx.fun.Try} and {@link dmx.fun.Result} executions with
 * both metrics and distributed tracing spans via the Micrometer Observation API.
 * Requires {@code micrometer-core} ≥ 1.10 at runtime.
 */
// ObservationRegistry appears in the public API of DmxObservation and DmxObserved,
// so requires-transitive is correct. The warning fires because micrometer-core ships
// as an automatic module (Automatic-Module-Name only, no module-info.class).
@SuppressWarnings("requires-transitive-automatic")
module dmx.fun.observation {
    requires transitive dmx.fun;
    requires static transitive micrometer.core;
    requires static org.jspecify;
    requires micrometer.observation;

    exports dmx.fun.observation;
}
