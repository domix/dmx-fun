package dmx.fun.quarkus;

import dmx.fun.Result;
import jakarta.interceptor.InterceptorBinding;
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
 * }
 * }</pre>
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
public @interface TransactionalResult {}
