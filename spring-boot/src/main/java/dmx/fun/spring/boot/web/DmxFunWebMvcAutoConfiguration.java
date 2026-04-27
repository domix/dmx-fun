package dmx.fun.spring.boot.web;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

/**
 * Spring Boot auto-configuration that registers dmx-fun Spring MVC
 * {@link org.springframework.web.method.support.HandlerMethodReturnValueHandler}s
 * in Spring MVC's {@link RequestMappingHandlerAdapter}.
 *
 * <p>Registers:
 * <ul>
 *   <li>{@link OptionHandlerMethodReturnValueHandler} for {@link dmx.fun.Option}
 *       (some → 200, none → 404), controlled by
 *       {@code dmx.fun.mvc.option-handler.enabled}.</li>
 *   <li>{@link ResultHandlerMethodReturnValueHandler} for {@link dmx.fun.Result},
 *       {@link dmx.fun.Validated}, and {@link dmx.fun.Try}
 *       (success/valid → 200, error/invalid/failure → 500), controlled by
 *       {@code dmx.fun.mvc.result-handler.enabled}.</li>
 * </ul>
 */
@AutoConfiguration(afterName = {
    // Boot 3.x location
    "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration",
    // Boot 4.x location (moved)
    "org.springframework.boot.web.servlet.autoconfigure.WebMvcAutoConfiguration"
})
@ConditionalOnClass({DispatcherServlet.class, RequestMappingHandlerAdapter.class})
@NullMarked
public class DmxFunWebMvcAutoConfiguration {

    /** Default constructor required for Spring Boot auto-configuration instantiation. */
    public DmxFunWebMvcAutoConfiguration() {}

    @Bean
    @ConditionalOnProperty(name = "dmx.fun.mvc.result-handler.enabled", havingValue = "true", matchIfMissing = true)
    static BeanPostProcessor resultReturnValueHandlerPostProcessor() {
        return insertionPostProcessor(
            ResultHandlerMethodReturnValueHandler.class,
            ResultHandlerMethodReturnValueHandler::new);
    }

    @Bean
    @ConditionalOnProperty(name = "dmx.fun.mvc.option-handler.enabled", havingValue = "true", matchIfMissing = true)
    static BeanPostProcessor optionReturnValueHandlerPostProcessor() {
        return insertionPostProcessor(
            OptionHandlerMethodReturnValueHandler.class,
            OptionHandlerMethodReturnValueHandler::new);
    }

    private static BeanPostProcessor insertionPostProcessor(
            Class<? extends HandlerMethodReturnValueHandler> guard,
            Function<HandlerMethodReturnValueHandler, HandlerMethodReturnValueHandler> factory) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (!(bean instanceof RequestMappingHandlerAdapter adapter)) return bean;

                List<HandlerMethodReturnValueHandler> current = adapter.getReturnValueHandlers();
                if (current == null) return bean;

                List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>(current);
                if (handlers.stream().anyMatch(guard::isInstance)) return bean;

                HandlerMethodReturnValueHandler bodyProcessor = handlers.stream()
                    .filter(h -> h instanceof RequestResponseBodyMethodProcessor)
                    .findFirst()
                    .orElse(null);
                if (bodyProcessor == null) return bean;

                handlers.add(0, factory.apply(bodyProcessor));
                adapter.setReturnValueHandlers(handlers);
                return bean;
            }
        };
    }
}
