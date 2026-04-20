/**
 * Micrometer adapter for dmx-fun.
 *
 * <p>Instruments {@link dmx.fun.Try} and {@link dmx.fun.Result} executions with
 * Micrometer counters, timers, and failure metrics without requiring manual
 * metric tracking at call sites.
 */
module dmx.fun.micrometer {
    requires transitive dmx.fun;
    requires static transitive micrometer.core;
    requires static org.jspecify;

    exports dmx.fun.micrometer;
}
