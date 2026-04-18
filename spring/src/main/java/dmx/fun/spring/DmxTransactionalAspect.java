package dmx.fun.spring;

import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Spring AOP aspect that backs the {@link TransactionalResult}, {@link TransactionalTry},
 * and {@link TransactionalValidated} annotations.
 *
 * <p>Intercepts annotated methods and delegates to {@link TxExecutor} with a
 * {@link org.springframework.transaction.TransactionDefinition} built from the annotation
 * attributes. The transaction commits on a success value and rolls back on a failure value
 * or an unchecked exception.
 *
 * <p>Register this bean and enable AspectJ auto-proxying in your Spring context:
 *
 * <pre>{@code
 * @Configuration
 * @EnableAspectJAutoProxy
 * public class AppConfig {
 *     @Bean
 *     public DmxTransactionalAspect dmxTransactionalAspect(
 *             PlatformTransactionManager txManager, BeanFactory beanFactory) {
 *         return new DmxTransactionalAspect(txManager, beanFactory);
 *     }
 * }
 * }</pre>
 *
 * @see TransactionalResult
 * @see TransactionalTry
 * @see TransactionalValidated
 */
@Aspect
@Component
@NullMarked
public class DmxTransactionalAspect {

    private final PlatformTransactionManager defaultTxManager;
    private final BeanFactory beanFactory;

    /**
     * Creates the aspect backed by the given transaction manager and bean factory.
     *
     * @param txManager   the default transaction manager used when no bean name is specified
     *                    in the annotation; must not be {@code null}
     * @param beanFactory the factory used to look up named transaction managers; must not
     *                    be {@code null}
     */
    public DmxTransactionalAspect(
        PlatformTransactionManager txManager,
        BeanFactory beanFactory
    ) {
        this.defaultTxManager = Objects.requireNonNull(txManager, "txManager");
        this.beanFactory = Objects.requireNonNull(beanFactory, "beanFactory");
    }

    /**
     * Runs the annotated method inside a transaction and rolls back if the returned
     * {@link Result} is an error.
     *
     * @param pjp the proceeding join point supplied by AspectJ
     * @param ann the annotation instance carrying the transaction attributes
     * @return the {@link Result} returned by the target method
     */
    @Around("@annotation(ann)")
    @SuppressWarnings("unchecked")
    public Object aroundResult(ProceedingJoinPoint pjp, TransactionalResult ann) {
        return executor(ann.transactionManager())
            .execute(
                definition(ann.propagation(), ann.isolation(), ann.timeout()),
                () -> (Result<Object, Object>) proceed(pjp),
                Result::isError
            );
    }

    /**
     * Runs the annotated method inside a transaction and rolls back if the returned
     * {@link Try} is a failure.
     *
     * @param pjp the proceeding join point supplied by AspectJ
     * @param ann the annotation instance carrying the transaction attributes
     * @return the {@link Try} returned by the target method
     */
    @Around("@annotation(ann)")
    @SuppressWarnings("unchecked")
    public Object aroundTry(ProceedingJoinPoint pjp, TransactionalTry ann) {
        return executor(ann.transactionManager())
            .execute(
                definition(ann.propagation(), ann.isolation(), ann.timeout()),
                () -> (Try<Object>) proceed(pjp),
                Try::isFailure
            );
    }

    /**
     * Runs the annotated method inside a transaction and rolls back if the returned
     * {@link Validated} is invalid.
     *
     * @param pjp the proceeding join point supplied by AspectJ
     * @param ann the annotation instance carrying the transaction attributes
     * @return the {@link Validated} returned by the target method
     */
    @Around("@annotation(ann)")
    @SuppressWarnings("unchecked")
    public Object aroundValidated(ProceedingJoinPoint pjp, TransactionalValidated ann) {
        return executor(ann.transactionManager())
            .execute(
                definition(ann.propagation(), ann.isolation(), ann.timeout()),
                () -> (Validated<Object, Object>) proceed(pjp),
                Validated::isInvalid
            );
    }

    private TxExecutor executor(String txManagerBeanName) {
        var txm = txManagerBeanName.isEmpty()
            ? defaultTxManager
            : beanFactory.getBean(txManagerBeanName, PlatformTransactionManager.class);
        return new TxExecutor(txm);
    }

    private static DefaultTransactionDefinition definition(
            Propagation propagation, Isolation isolation, int timeout) {
        var def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(propagation.value());
        def.setIsolationLevel(isolation.value());
        def.setTimeout(timeout);
        return def;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proceed(ProceedingJoinPoint pjp) {
        try {
            return (T) pjp.proceed();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
