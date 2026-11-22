package dmx.fun.spring.boot.jackson3;

import dmx.fun.Result;
import dmx.fun.jackson3.DmxFunModule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class DmxFunJackson3AutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DmxFunJackson3AutoConfiguration.class));

    // ── Default auto-configuration ────────────────────────────────────────────

    @Test
    void registersDmxFunModule() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(DmxFunModule.class));
    }

    @Test
    void registeredModuleIsAJacksonModule_andSerializesDmxFunTypes() {
        runner.run(ctx -> {
            var module = ctx.getBean(JacksonModule.class);
            var mapper = JsonMapper.builder().addModule(module).build();
            assertThat(mapper.writeValueAsString(Result.ok(42))).isEqualTo("{\"ok\":42}");
        });
    }

    // ── @ConditionalOnProperty disable ───────────────────────────────────────

    @Test
    void disabled_whenPropertyFalse() {
        runner.withPropertyValues("dmx.fun.jackson3.enabled=false")
            .run(ctx -> assertThat(ctx).doesNotHaveBean(DmxFunModule.class));
    }

    // ── @ConditionalOnMissingBean back-off ───────────────────────────────────

    @Test
    void backsOff_whenDmxFunModuleAlreadyPresent() {
        runner.withUserConfiguration(CustomModuleConfig.class)
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(DmxFunModule.class);
                assertThat(ctx.getBean(DmxFunModule.class)).isSameAs(ctx.getBean("customModule"));
            });
    }

    // ── Test configurations ───────────────────────────────────────────────────

    @Configuration
    static class CustomModuleConfig {
        @Bean
        DmxFunModule customModule() {
            return new DmxFunModule();
        }
    }
}
