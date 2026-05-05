package dmx.fun.quarkus;

import dmx.fun.Result;
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
 * Declarative transaction annotation for methods that return {@link Result}.
 *
 * <p>When applied to a CDI bean method, {@link TransactionalDmxInterceptor} intercepts
 * the call and wraps the body in a JTA transaction. The transaction commits when the
 * method returns {@link Result#isOk()} and rolls back when it returns
 * {@link Result#isError()} or when an unchecked exception escapes.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * @ApplicationScoped
 * public class OrderService {
 *
 *     @TransactionalResult
 *     public Result<Order, OrderError> createOrder(OrderRequest req) {
 *         return validate(req)
 *             .flatMap(this::persistOrder)
 *             .flatMap(this::notifyInventory);
 *     }
 *
 *     // Always suspend any outer transaction and start a fresh one.
 *     @TransactionalResult(Transactional.TxType.REQUIRES_NEW)
 *     public Result<AuditEntry, String> audit(Event event) { ... }
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
 * @see TransactionalTry
 * @see TransactionalDmxInterceptor
 */
@DmxTransactionalBinding
@InterceptorBinding
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TransactionalResult {

    /**
     * The transaction propagation type.
     *
     * <p>Marked {@link Nonbinding} so that CDI selects the same interceptor regardless
     * of the chosen {@link Transactional.TxType}; the interceptor reads the value at runtime.
     */
    @Nonbinding Transactional.TxType value() default Transactional.TxType.REQUIRED;
}
