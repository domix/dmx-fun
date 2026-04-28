package dmx.fun.spring.boot;

import dmx.fun.spring.DmxTransactionalAspect;
import dmx.fun.spring.TxResult;
import dmx.fun.spring.TxTry;
import dmx.fun.spring.TxValidated;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies DmxFunSpringAutoConfiguration behaviour when loaded into a plain
 * AnnotationConfigApplicationContext (no Spring Boot test infrastructure).
 *
 * <p>Because this class relies on Spring Boot-specific annotations
 * ({@code @AutoConfiguration}, {@code @ConditionalOnMissingBean},
 * {@code @ConditionalOnSingleCandidate}), {@code spring-boot-autoconfigure}
 * must be on the classpath. The Boot conditions are evaluated by Spring
 * Framework's own {@code ConditionEvaluator}, but full back-off behaviour
 * (i.e. {@code @ConditionalOnMissingBean} ordering guarantees) is only
 * reliable through Spring Boot's test infrastructure — use
 * {@link DmxFunSpringAutoConfigurationTest} with {@code ApplicationContextRunner}
 * for those cases.
 */
class DmxFunSpringAutoConfigurationPlainSpringTest {

    // ── Positive: beans registered when a single PTM is present ──────────────

    @Test
    void registersTxResult_whenSinglePtmPresent() {
        try (var ctx = start(TxManagerConfig.class, DmxFunSpringAutoConfiguration.class)) {
            assertThat(ctx.getBeansOfType(TxResult.class)).hasSize(1);
        }
    }

    @Test
    void registersTxTry_whenSinglePtmPresent() {
        try (var ctx = start(TxManagerConfig.class, DmxFunSpringAutoConfiguration.class)) {
            assertThat(ctx.getBeansOfType(TxTry.class)).hasSize(1);
        }
    }

    @Test
    void registersTxValidated_whenSinglePtmPresent() {
        try (var ctx = start(TxManagerConfig.class, DmxFunSpringAutoConfiguration.class)) {
            assertThat(ctx.getBeansOfType(TxValidated.class)).hasSize(1);
        }
    }

    @Test
    void registersDmxTransactionalAspect_whenAspectJAndSinglePtmPresent() {
        try (var ctx = start(TxManagerConfig.class, DmxFunSpringAutoConfiguration.class)) {
            assertThat(ctx.getBeansOfType(DmxTransactionalAspect.class)).hasSize(1);
        }
    }

    // ── Negative: no beans when no PTM is present ─────────────────────────────

    @Test
    void doesNotRegisterTxBeans_whenNoPtmPresent() {
        // Without a PlatformTransactionManager, @ConditionalOnSingleCandidate
        // prevents all beans from being registered.
        try (var ctx = start(DmxFunSpringAutoConfiguration.class)) {
            assertThat(ctx.getBeansOfType(TxResult.class)).isEmpty();
            assertThat(ctx.getBeansOfType(TxTry.class)).isEmpty();
            assertThat(ctx.getBeansOfType(TxValidated.class)).isEmpty();
            assertThat(ctx.getBeansOfType(DmxTransactionalAspect.class)).isEmpty();
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static AnnotationConfigApplicationContext start(Class<?>... configs) {
        var ctx = new AnnotationConfigApplicationContext();
        ctx.register(configs);
        ctx.refresh();
        return ctx;
    }

    // ── Configurations ────────────────────────────────────────────────────────

    @Configuration
    static class TxManagerConfig {
        @Bean
        PlatformTransactionManager txManager() { return new StubTxManager(); }
    }

}
