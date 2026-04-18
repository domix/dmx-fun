package dmx.fun.spring.boot;

import dmx.fun.spring.DmxTransactionalAspect;
import dmx.fun.spring.TxResult;
import dmx.fun.spring.TxTry;
import dmx.fun.spring.TxValidated;
import org.aspectj.lang.annotation.Aspect;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Boot auto-configuration for dmx-fun Spring integration.
 *
 * <p>Registers {@link TxResult}, {@link TxTry}, and {@link TxValidated} when a
 * {@link PlatformTransactionManager} is on the classpath. Additionally enables
 * AspectJ auto-proxying and registers {@link DmxTransactionalAspect} when
 * {@code aspectjweaver} is also present.
 *
 * <p>All beans are guarded by {@link ConditionalOnMissingBean} so application
 * code can override any of them with a custom bean declaration.
 *
 * <p>This class is registered in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * and is picked up automatically by Spring Boot. It can also be used with plain
 * Spring Framework via {@code @Import(DmxFunSpringAutoConfiguration.class)}.
 *
 * @see TxResult
 * @see TxTry
 * @see TxValidated
 * @see DmxTransactionalAspect
 */
@AutoConfiguration
@ConditionalOnClass(PlatformTransactionManager.class)
@NullMarked
public class DmxFunSpringAutoConfiguration {

    /**
     * Registers a {@link TxResult} backed by the primary transaction manager.
     *
     * @param txManager the transaction manager to back the executor
     * @return a ready-to-use {@link TxResult} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public TxResult txResult(PlatformTransactionManager txManager) {
        return new TxResult(txManager);
    }

    /**
     * Registers a {@link TxTry} backed by the primary transaction manager.
     *
     * @param txManager the transaction manager to back the executor
     * @return a ready-to-use {@link TxTry} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public TxTry txTry(PlatformTransactionManager txManager) {
        return new TxTry(txManager);
    }

    /**
     * Registers a {@link TxValidated} backed by the primary transaction manager.
     *
     * @param txManager the transaction manager to back the executor
     * @return a ready-to-use {@link TxValidated} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public TxValidated txValidated(PlatformTransactionManager txManager) {
        return new TxValidated(txManager);
    }

    /**
     * Enables AspectJ auto-proxying and registers {@link DmxTransactionalAspect}
     * when {@code aspectjweaver} is present on the classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Aspect.class)
    @EnableAspectJAutoProxy
    static class AspectConfiguration {

        /**
         * Registers {@link DmxTransactionalAspect} if no other bean of that type is
         * present. The aspect intercepts methods annotated with
         * {@link dmx.fun.spring.TransactionalResult}, {@link dmx.fun.spring.TransactionalTry},
         * or {@link dmx.fun.spring.TransactionalValidated}.
         *
         * @param txManager   the default transaction manager
         * @param beanFactory used to look up named transaction managers
         * @return a ready-to-use {@link DmxTransactionalAspect}
         */
        @Bean
        @ConditionalOnMissingBean
        public DmxTransactionalAspect dmxTransactionalAspect(
                PlatformTransactionManager txManager, BeanFactory beanFactory) {
            return new DmxTransactionalAspect(txManager, beanFactory);
        }
    }
}
