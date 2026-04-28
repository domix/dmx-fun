package dmx.fun.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

/**
 * Declarative transaction annotation for methods that return {@link dmx.fun.Try}.
 *
 * <p>When applied to a method, {@link DmxTransactionalAspect} intercepts the call and
 * runs the body inside a Spring-managed transaction. The transaction commits when the
 * method returns {@link dmx.fun.Try#isSuccess()} and rolls back when it returns
 * {@link dmx.fun.Try#isFailure()} or when an unchecked exception escapes.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * @Service
 * public class ReportService {
 *
 *     @TransactionalTry(propagation = Propagation.REQUIRES_NEW)
 *     public Try<AuditLog> auditEvent(Event event) {
 *         return Try.of(() -> auditRepository.save(event));
 *     }
 * }
 * }</pre>
 *
 * <p>Requires {@link DmxTransactionalAspect} to be registered as a Spring bean and
 * {@code @EnableAspectJAutoProxy} (or equivalent) active in the application context.
 *
 * @see DmxTransactionalAspect
 * @see TransactionalResult
 * @see TransactionalValidated
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TransactionalTry {

    /**
     * Transaction propagation behaviour.
     *
     * @return propagation setting; defaults to {@link Propagation#REQUIRED}
     */
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * Transaction isolation level.
     *
     * @return isolation level; defaults to {@link Isolation#DEFAULT}
     */
    Isolation isolation() default Isolation.DEFAULT;

    /**
     * Transaction timeout in seconds.
     *
     * @return timeout in seconds; defaults to {@link TransactionDefinition#TIMEOUT_DEFAULT}
     *         (no explicit timeout)
     */
    int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;

    /**
     * Whether the transaction is read-only.
     *
     * <p>A read-only hint allows the underlying JDBC driver or ORM to apply
     * optimisations (e.g. skip dirty checking, use a read replica). It does
     * not prevent write statements — enforcement depends on the actual
     * transaction manager and data source.
     *
     * @return {@code true} to request a read-only transaction;
     *         defaults to {@code false}
     */
    boolean readOnly() default false;

    /**
     * Bean name of a specific {@link org.springframework.transaction.PlatformTransactionManager}
     * to use.
     *
     * @return bean name; defaults to {@code ""} which selects the primary
     *         {@code PlatformTransactionManager} in the context
     */
    String transactionManager() default "";
}
