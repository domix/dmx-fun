package dmx.fun.spring.boot.jackson;

import dmx.fun.jackson.DmxFunModule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class DmxFunJacksonAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DmxFunJacksonAutoConfiguration.class));

    // ── Default auto-configuration ────────────────────────────────────────────

    @Test
    void registersDmxFunModule() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(DmxFunModule.class));
    }

    // ── @ConditionalOnProperty disable ───────────────────────────────────────

    @Test
    void disabled_whenPropertyFalse() {
        runner.withPropertyValues("dmx.fun.jackson.enabled=false")
            .run(ctx -> assertThat(ctx).doesNotHaveBean(DmxFunModule.class));
    }

    // ── @ConditionalOnMissingBean back-off ───────────────────────────────────

    @Test
    void backsOff_whenDmxFunModuleAlreadyPresent() {
        runner.withUserConfiguration(CustomModuleConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(DmxFunModule.class);
            assertThat(ctx.getBean(DmxFunModule.class)).isSameAs(ctx.getBean("customModule"));
        });
    }

    // ── Test configurations ───────────────────────────────────────────────────

    @Configuration
    static class CustomModuleConfig {
        @Bean
        DmxFunModule customModule() { return new DmxFunModule(); }
    }
}
