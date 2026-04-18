package dmx.fun.spring.boot;

import dmx.fun.spring.DmxTransactionalAspect;
import dmx.fun.spring.TxResult;
import dmx.fun.spring.TxTry;
import dmx.fun.spring.TxValidated;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DmxFunSpringAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DmxFunSpringAutoConfiguration.class))
        .withUserConfiguration(DefaultConfig.class);

    // ── Default auto-configuration ────────────────────────────────────────────

    @Test
    void registersTxResult() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(TxResult.class));
    }

    @Test
    void registersTxTry() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(TxTry.class));
    }

    @Test
    void registersTxValidated() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(TxValidated.class));
    }

    @Test
    void registersDmxTransactionalAspect_whenAspectJPresent() {
        // aspectjweaver is on the test classpath → @ConditionalOnClass(Aspect.class) matches
        runner.run(ctx -> assertThat(ctx).hasSingleBean(DmxTransactionalAspect.class));
    }

    // ── @ConditionalOnMissingBean back-off ────────────────────────────────────

    @Test
    void backsOff_txResult_whenUserBeanPresent() {
        runner.withUserConfiguration(CustomTxResultConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(TxResult.class);
            assertThat(ctx.getBean(TxResult.class)).isSameAs(ctx.getBean("customTxResult"));
        });
    }

    @Test
    void backsOff_txTry_whenUserBeanPresent() {
        runner.withUserConfiguration(CustomTxTryConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(TxTry.class);
            assertThat(ctx.getBean(TxTry.class)).isSameAs(ctx.getBean("customTxTry"));
        });
    }

    @Test
    void backsOff_txValidated_whenUserBeanPresent() {
        runner.withUserConfiguration(CustomTxValidatedConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(TxValidated.class);
            assertThat(ctx.getBean(TxValidated.class)).isSameAs(ctx.getBean("customTxValidated"));
        });
    }

    @Test
    void backsOff_aspect_whenUserBeanPresent() {
        runner.withUserConfiguration(CustomAspectConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(DmxTransactionalAspect.class);
            assertThat(ctx.getBean(DmxTransactionalAspect.class)).isSameAs(ctx.getBean("customAspect"));
        });
    }

    // ── Test configurations ───────────────────────────────────────────────────

    @Configuration
    static class DefaultConfig {
        @Bean
        PlatformTransactionManager txManager() { return new StubTxManager(); }
    }

    @Configuration
    static class CustomTxResultConfig {
        @Bean
        TxResult customTxResult(PlatformTransactionManager txManager) {
            return new TxResult(txManager);
        }
    }

    @Configuration
    static class CustomTxTryConfig {
        @Bean
        TxTry customTxTry(PlatformTransactionManager txManager) {
            return new TxTry(txManager);
        }
    }

    @Configuration
    static class CustomTxValidatedConfig {
        @Bean
        TxValidated customTxValidated(PlatformTransactionManager txManager) {
            return new TxValidated(txManager);
        }
    }

    @Configuration
    static class CustomAspectConfig {
        @Bean
        DmxTransactionalAspect customAspect(
                PlatformTransactionManager txManager,
                org.springframework.beans.factory.BeanFactory beanFactory) {
            return new DmxTransactionalAspect(txManager, beanFactory);
        }
    }

    // ── Stub transaction manager ──────────────────────────────────────────────

    static class StubTxManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }
        @Override public void commit(TransactionStatus status) {}
        @Override public void rollback(TransactionStatus status) {}
    }
}
