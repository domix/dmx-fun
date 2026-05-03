package dmx.fun.quarkus;

import dmx.fun.Try;
import jakarta.interceptor.InterceptorBinding;
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
 * }
 * }</pre>
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
public @interface TransactionalTry {}
