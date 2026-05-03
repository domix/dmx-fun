package dmx.fun.spring.boot.observation;

import dmx.fun.observation.DmxObservation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class DmxFunObservationAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DmxFunObservationAutoConfiguration.class))
        .withUserConfiguration(DefaultConfig.class);

    // ── Default auto-configuration ────────────────────────────────────────────

    @Test
    void registersDmxObservation() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(DmxObservation.class));
    }

    // ── @ConditionalOnMissingBean back-off ────────────────────────────────────

    @Test
    void backsOff_whenUserBeanPresent() {
        runner.withUserConfiguration(CustomDmxObservationConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(DmxObservation.class);
            assertThat(ctx.getBean(DmxObservation.class)).isSameAs(ctx.getBean("customDmxObservation"));
        });
    }

    // ── @ConditionalOnProperty disable ───────────────────────────────────────

    @Test
    void disabledWhenPropertyFalse() {
        runner.withPropertyValues("dmx.fun.observation.enabled=false")
            .run(ctx -> assertThat(ctx).doesNotHaveBean(DmxObservation.class));
    }

    // ── @ConditionalOnBean — no ObservationRegistry, no bean ─────────────────

    @Test
    void doesNotRegister_whenNoObservationRegistryBean() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DmxFunObservationAutoConfiguration.class))
            .run(ctx -> assertThat(ctx).doesNotHaveBean(DmxObservation.class));
    }

    // ── Test configurations ───────────────────────────────────────────────────

    @Configuration
    static class DefaultConfig {
        @Bean
        ObservationRegistry observationRegistry() { return ObservationRegistry.create(); }
    }

    @Configuration
    static class CustomDmxObservationConfig {
        @Bean
        DmxObservation customDmxObservation(ObservationRegistry registry) {
            return DmxObservation.of(registry);
        }
    }
}
