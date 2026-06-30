/**
 * Spring WebFlux adapter for dmx-fun.
 *
 * <p>Maps {@link dmx.fun.Option}, {@link dmx.fun.Result}, {@link dmx.fun.Try}, and
 * {@link dmx.fun.Validated} to Spring WebFlux
 * {@code org.springframework.web.reactive.function.server.ServerResponse} for
 * functional endpoints, with documented, overridable HTTP mapping conventions.
 */
// ServerResponse and Mono appear in the public API of WebfluxFun, so requires-transitive
// is correct. The warning fires because spring-webflux and reactor-core ship as automatic
// modules (Automatic-Module-Name only, no module-info.class).
@SuppressWarnings("requires-transitive-automatic")
module dmx.fun.spring.webflux {
    requires transitive dmx.fun;
    requires transitive dmx.fun.reactor;
    requires static transitive spring.webflux;
    requires static transitive reactor.core;
    requires static org.jspecify;

    exports dmx.fun.spring.webflux;
}
