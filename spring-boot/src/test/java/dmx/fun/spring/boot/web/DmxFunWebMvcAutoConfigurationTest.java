package dmx.fun.spring.boot.web;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import static org.assertj.core.api.Assertions.assertThat;

class DmxFunWebMvcAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DmxFunWebMvcAutoConfiguration.class));

    // ── Registration ──────────────────────────────────────────────────────────

    @Test
    void registersPostProcessor_byDefault() {
        runner.run(ctx -> assertThat(ctx).hasBean("optionReturnValueHandlerPostProcessor"));
    }

    @Test
    void postProcessor_disabled_byProperty() {
        runner.withPropertyValues("dmx.fun.mvc.option-handler.enabled=false")
            .run(ctx -> assertThat(ctx).doesNotHaveBean("optionReturnValueHandlerPostProcessor"));
    }

    // ── BeanPostProcessor behavior ────────────────────────────────────────────

    @Test
    void postProcessor_ignoresNonAdapterBeans() {
        runner.run(ctx -> {
            BeanPostProcessor bpp = ctx.getBean("optionReturnValueHandlerPostProcessor", BeanPostProcessor.class);
            Object other = "not-an-adapter";
            assertThat(bpp.postProcessAfterInitialization(other, "x")).isSameAs(other);
        });
    }

    @Test
    void postProcessor_returnsEarly_whenHandlersNull() {
        runner.run(ctx -> {
            BeanPostProcessor bpp = ctx.getBean("optionReturnValueHandlerPostProcessor", BeanPostProcessor.class);
            StubAdapter adapter = new StubAdapter(null);
            assertThat(bpp.postProcessAfterInitialization(adapter, "adapter")).isSameAs(adapter);
            assertThat(adapter.handlers).isNull();
        });
    }

    @Test
    void postProcessor_insertsOptionHandler_atIndex0_beforeBodyProcessor() {
        runner.run(ctx -> {
            BeanPostProcessor bpp = ctx.getBean("optionReturnValueHandlerPostProcessor", BeanPostProcessor.class);
            HandlerMethodReturnValueHandler bodyProcessor = new StubBodyProcessor();
            StubAdapter adapter = new StubAdapter(new ArrayList<>(List.of(bodyProcessor)));

            bpp.postProcessAfterInitialization(adapter, "adapter");

            assertThat(adapter.handlers).hasSize(2);
            assertThat(adapter.handlers.get(0)).isInstanceOf(OptionHandlerMethodReturnValueHandler.class);
            assertThat(adapter.handlers.get(1)).isSameAs(bodyProcessor);
        });
    }

    @Test
    void postProcessor_isIdempotent_whenOptionHandlerAlreadyPresent() {
        runner.run(ctx -> {
            BeanPostProcessor bpp = ctx.getBean("optionReturnValueHandlerPostProcessor", BeanPostProcessor.class);
            HandlerMethodReturnValueHandler bodyProcessor = new StubBodyProcessor();
            OptionHandlerMethodReturnValueHandler existing = new OptionHandlerMethodReturnValueHandler(bodyProcessor);
            StubAdapter adapter = new StubAdapter(new ArrayList<>(List.of(existing, bodyProcessor)));

            bpp.postProcessAfterInitialization(adapter, "adapter");

            assertThat(adapter.handlers).hasSize(2);
            assertThat(adapter.handlers.get(0)).isSameAs(existing);
        });
    }

    @Test
    void postProcessor_skipsInsert_whenNoBodyProcessorFound() {
        runner.run(ctx -> {
            BeanPostProcessor bpp = ctx.getBean("optionReturnValueHandlerPostProcessor", BeanPostProcessor.class);
            HandlerMethodReturnValueHandler other = new StubHandler();
            StubAdapter adapter = new StubAdapter(new ArrayList<>(List.of(other)));

            bpp.postProcessAfterInitialization(adapter, "adapter");

            assertThat(adapter.handlers).hasSize(1);
            assertThat(adapter.handlers.get(0)).isSameAs(other);
        });
    }

    // ── Stubs ─────────────────────────────────────────────────────────────────

    static class StubAdapter extends RequestMappingHandlerAdapter {
        List<HandlerMethodReturnValueHandler> handlers;

        StubAdapter(List<HandlerMethodReturnValueHandler> handlers) {
            this.handlers = handlers;
        }

        @Override
        public List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
            return handlers;
        }

        @Override
        public void setReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
            this.handlers = new ArrayList<>(handlers);
        }
    }

    static class StubBodyProcessor extends RequestResponseBodyMethodProcessor {
        StubBodyProcessor() {
            super(List.of(new StringHttpMessageConverter()));
        }
    }

    static class StubHandler implements HandlerMethodReturnValueHandler {
        @Override
        public boolean supportsReturnType(MethodParameter param) { return false; }

        @Override
        public void handleReturnValue(Object value, MethodParameter param,
                ModelAndViewContainer mav, NativeWebRequest req) {}
    }
}
