/**
 * Spring Boot autoconfiguration for the dmx-fun library.
 *
 * <p>Provides autoconfiguration for Spring transaction helpers ({@link dmx.fun.spring.TxResult},
 * {@link dmx.fun.spring.TxTry}, {@link dmx.fun.spring.TxValidated}), the transactional AOP
 * aspect, Jackson serialization support, and Spring MVC return-value handlers for
 * {@code Option}, {@code Result}, {@code Validated}, and {@code Try}.
 */
module dmx.fun.spring.boot {
    requires dmx.fun;
    requires dmx.fun.spring;
    requires spring.core;
    requires spring.beans;
    requires spring.context;
    requires spring.tx;
    requires static spring.boot.autoconfigure;
    requires static org.aspectj.weaver;
    requires static spring.webmvc;
    requires static spring.web;
    requires static jakarta.servlet;
    requires static dmx.fun.jackson;
    requires static com.fasterxml.jackson.databind;
    requires static dmx.fun.tracing;
    requires static micrometer.tracing;
    requires org.jspecify;

    exports dmx.fun.spring.boot;
    exports dmx.fun.spring.boot.web;
    exports dmx.fun.spring.boot.jackson;
    exports dmx.fun.spring.boot.tracing;
}
