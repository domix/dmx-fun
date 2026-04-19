package dmx.fun.spring.boot.web;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
 * Spring Boot auto-configuration that registers {@link OptionHandlerMethodReturnValueHandler}
 * in Spring MVC's {@link RequestMappingHandlerAdapter}.
 *
 * <p>This allows controller methods to return {@link dmx.fun.Option} directly:
 * {@code Option.some(value)} produces HTTP 200 with the serialized value;
 * {@code Option.none()} produces HTTP 404 with an empty body.
 *
 * <p>The handler is disabled via {@code dmx.fun.mvc.option-handler.enabled=false}.
 */
@AutoConfiguration(afterName = {
    // Boot 3.x location
    "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration",
    // Boot 4.x location (moved)
    "org.springframework.boot.web.servlet.autoconfigure.WebMvcAutoConfiguration"
})
@ConditionalOnClass({DispatcherServlet.class, RequestMappingHandlerAdapter.class})
@ConditionalOnProperty(name = "dmx.fun.mvc.option-handler.enabled", havingValue = "true", matchIfMissing = true)
@NullMarked
public class DmxFunWebMvcAutoConfiguration {

    @Bean
    static BeanPostProcessor optionReturnValueHandlerPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (!(bean instanceof RequestMappingHandlerAdapter adapter)) return bean;

                List<HandlerMethodReturnValueHandler> current = adapter.getReturnValueHandlers();
                if (current == null) return bean;

                List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>(current);

                boolean alreadyPresent = handlers.stream()
                    .anyMatch(h -> h instanceof OptionHandlerMethodReturnValueHandler);
                if (alreadyPresent) return bean;

                @Nullable HandlerMethodReturnValueHandler bodyProcessor = handlers.stream()
                    .filter(h -> h instanceof RequestResponseBodyMethodProcessor)
                    .findFirst()
                    .orElse(null);
                if (bodyProcessor == null) return bean;

                handlers.add(0, new OptionHandlerMethodReturnValueHandler(bodyProcessor));
                adapter.setReturnValueHandlers(handlers);

                return bean;
            }
        };
    }
}
