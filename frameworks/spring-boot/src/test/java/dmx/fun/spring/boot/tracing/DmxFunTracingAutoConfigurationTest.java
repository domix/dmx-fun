package dmx.fun.spring.boot.tracing;

import dmx.fun.tracing.DmxTracing;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class DmxFunTracingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DmxFunTracingAutoConfiguration.class))
        .withUserConfiguration(DefaultConfig.class);

    // ── Default auto-configuration ────────────────────────────────────────────

    @Test
    void registersDmxTracing() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(DmxTracing.class));
    }

    // ── @ConditionalOnMissingBean back-off ────────────────────────────────────

    @Test
    void backsOff_whenUserBeanPresent() {
        runner.withUserConfiguration(CustomDmxTracingConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(DmxTracing.class);
            assertThat(ctx.getBean(DmxTracing.class)).isSameAs(ctx.getBean("customDmxTracing"));
        });
    }

    // ── @ConditionalOnProperty disable ───────────────────────────────────────

    @Test
    void disabledWhenPropertyFalse() {
        runner.withPropertyValues("dmx.fun.tracing.enabled=false")
            .run(ctx -> assertThat(ctx).doesNotHaveBean(DmxTracing.class));
    }

    // ── @ConditionalOnBean — no Tracer, no bean ───────────────────────────────

    @Test
    void doesNotRegister_whenNoTracerBean() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DmxFunTracingAutoConfiguration.class))
            .run(ctx -> assertThat(ctx).doesNotHaveBean(DmxTracing.class));
    }

    // ── Test configurations ───────────────────────────────────────────────────

    @Configuration
    static class DefaultConfig {
        @Bean
        Tracer tracer() { return new SimpleTracer(); }
    }

    @Configuration
    static class CustomDmxTracingConfig {
        @Bean
        DmxTracing customDmxTracing(Tracer tracer) {
            return DmxTracing.of(tracer);
        }
    }
}
