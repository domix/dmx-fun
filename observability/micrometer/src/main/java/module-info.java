/**
 * Micrometer adapter for dmx-fun.
 *
 * <p>Instruments {@link dmx.fun.Try} and {@link dmx.fun.Result} executions with
 * Micrometer counters, timers, and failure metrics without requiring manual
 * metric tracking at call sites.
 */
// MeterRegistry and Tags appear in the public API of DmxMicrometer and DmxMetered,
// so requires-transitive is correct. The warning fires because micrometer-core ships
// as an automatic module (Automatic-Module-Name only, no module-info.class).
@SuppressWarnings("requires-transitive-automatic")
module dmx.fun.micrometer {
    requires transitive dmx.fun;
    requires static transitive micrometer.core;
    requires static org.jspecify;

    exports dmx.fun.micrometer;
}
