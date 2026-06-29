/**
 * Project Reactor adapter for dmx-fun.
 *
 * <p>Provides idiomatic, explicit conversions between Reactor's
 * {@link reactor.core.publisher.Mono} and dmx-fun's {@link dmx.fun.Option},
 * {@link dmx.fun.Result}, and {@link dmx.fun.Try}, so absence, failure, and
 * exceptions are modeled as values at reactive boundaries instead of being
 * reconstructed from ad-hoc conversion code.
 */
// Mono appears in the public API of ReactorFun, so requires-transitive is correct.
// The warning fires because reactor-core ships as an automatic module
// (Automatic-Module-Name only, no module-info.class).
@SuppressWarnings("requires-transitive-automatic")
module dmx.fun.reactor {
    requires transitive dmx.fun;
    requires static transitive reactor.core;
    requires static org.jspecify;

    exports dmx.fun.reactor;
}
