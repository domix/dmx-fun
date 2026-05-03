/**
 * Spring Framework integration for dmx-fun types.
 *
 * <p>Provides transaction support so applications can use {@code Result}, {@code Try},
 * {@code Option}, and {@code Validated} idiomatically without giving up declarative
 * transaction management via Spring's {@code @Transactional}.
 *
 * <p>Spring is declared as {@code compileOnly} — consumers bring their own Spring
 * dependency. Supported Spring release lines: 6.0.x, 6.1.x, 6.2.x, 7.0.x.
 */
module dmx.fun.spring {
    requires dmx.fun;
    requires spring.beans;
    requires spring.tx;
    requires spring.context;
    requires static org.aspectj.weaver;
    requires org.jspecify;

    exports dmx.fun.spring;
}
