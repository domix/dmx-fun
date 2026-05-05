/**
 * Micrometer Tracing adapter for dmx-fun.
 *
 * <p>Instruments {@link dmx.fun.Try} and {@link dmx.fun.Result} executions with
 * distributed tracing spans. Each execution opens a named span, tags it with
 * {@code outcome=success|failure}, and on failure also records the exception class
 * and marks the span as error.
 *
 * <p>{@code micrometer-tracing} is declared as a peer dependency ({@code compileOnly});
 * users must provide it and a backend bridge (e.g.
 * {@code micrometer-tracing-bridge-otel} or {@code micrometer-tracing-bridge-brave})
 * at runtime.
 */
// micrometer-tracing ships as an automatic module, so requires-transitive-automatic
// warning is expected.
@SuppressWarnings("requires-transitive-automatic")
module dmx.fun.tracing {
    requires transitive dmx.fun;
    requires static transitive micrometer.tracing;
    requires static org.jspecify;

    exports dmx.fun.tracing;
}
