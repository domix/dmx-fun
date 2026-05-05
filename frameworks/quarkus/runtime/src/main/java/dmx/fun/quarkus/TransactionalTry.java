package dmx.fun.quarkus;

import dmx.fun.Try;
import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;
import jakarta.transaction.Transactional;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative transaction annotation for methods that return {@link Try}.
 *
 * <p>When applied to a CDI bean method, {@link TransactionalDmxInterceptor} intercepts
 * the call and wraps the body in a JTA transaction. The transaction commits when the
 * method returns {@link Try#isSuccess()} and rolls back when it returns
 * {@link Try#isFailure()} or when an unchecked exception escapes.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * @ApplicationScoped
 * public class ReportService {
 *
 *     @TransactionalTry
 *     public Try<Report> generate(ReportRequest req) {
 *         return Try.of(() -> reportRepo.save(build(req)))
 *             .map(r -> { auditLog.record(r); return r; });
 *     }
 *
 *     // Always start a fresh transaction, independent of any outer one.
 *     @TransactionalTry(Transactional.TxType.REQUIRES_NEW)
 *     public Try<AuditEntry> audit(Event event) { ... }
 * }
 * }</pre>
 *
 * <h2>Transaction semantics</h2>
 * <ul>
 *   <li>{@link Transactional.TxType#REQUIRED} (default) — join an existing transaction or
 *       begin a new one.</li>
 *   <li>{@link Transactional.TxType#REQUIRES_NEW} — always suspend any active transaction
 *       and begin a fresh one; the new transaction commits or rolls back independently.</li>
 *   <li>{@link Transactional.TxType#MANDATORY} — require an active transaction; throw
 *       {@link jakarta.transaction.TransactionalException} if none exists.</li>
 *   <li>{@link Transactional.TxType#SUPPORTS} — join an active transaction if present;
 *       otherwise execute without one.</li>
 *   <li>{@link Transactional.TxType#NOT_SUPPORTED} — suspend any active transaction and
 *       execute without one; resume it afterwards.</li>
 *   <li>{@link Transactional.TxType#NEVER} — throw
 *       {@link jakarta.transaction.TransactionalException} if a transaction is active.</li>
 * </ul>
 *
 * @see TransactionalResult
 * @see TransactionalDmxInterceptor
 */
@DmxTransactionalBinding
@InterceptorBinding
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TransactionalTry {

    /**
     * The transaction propagation type.
     *
     * <p>Marked {@link Nonbinding} so that CDI selects the same interceptor regardless
     * of the chosen {@link Transactional.TxType}; the interceptor reads the value at runtime.
     */
    @Nonbinding Transactional.TxType value() default Transactional.TxType.REQUIRED;
}
