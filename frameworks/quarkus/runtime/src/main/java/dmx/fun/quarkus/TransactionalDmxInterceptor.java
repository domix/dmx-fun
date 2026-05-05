package dmx.fun.quarkus;

import dmx.fun.Result;
import dmx.fun.Try;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NullMarked;

/**
 * CDI interceptor that backs {@link TransactionalResult} and {@link TransactionalTry}.
 *
 * <p>Activates on any method annotated with either {@link TransactionalResult} or
 * {@link TransactionalTry} (both carry the shared {@link DmxTransactionalBinding}
 * meta-interceptor-binding, per CDI §2.7.1.1). For each intercepted method the
 * interceptor:
 * <ol>
 *   <li>Resolves the {@link Transactional.TxType} from the annotation (default REQUIRED).</li>
 *   <li>Applies the corresponding JTA propagation semantics via {@link TxExecutor}.</li>
 *   <li>Rolls back (or marks rollback-only) if the returned {@code Result} is error or
 *       {@code Try} is failure.</li>
 *   <li>Commits on success; rolls back and re-throws on any unchecked exception.</li>
 * </ol>
 *
 * <p>This interceptor is registered automatically when the {@code fun-quarkus-deployment}
 * module is on the build classpath. No manual {@code @EnableInterceptors} or
 * {@code beans.xml} registration is required in a standard Quarkus application.
 *
 * @see TransactionalResult
 * @see TransactionalTry
 */
@NullMarked
@DmxTransactionalBinding
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 100)
public class TransactionalDmxInterceptor {

    private final TxExecutor executor;

    @Inject
    TransactionalDmxInterceptor(TransactionManager transactionManager) {
        this.executor = new TxExecutor(transactionManager);
    }

    /**
     * Wraps the intercepted method invocation in a JTA transaction, rolling back (or marking
     * rollback-only when joining an existing transaction) when the returned {@link Result} is
     * error or {@link Try} is failure.
     *
     * @param ctx the invocation context; must not be {@code null}
     * @return the value returned by the intercepted method
     * @throws IllegalStateException if the annotated method does not return {@link Result}
     *                               or {@link Try}
     * @throws Exception if the intercepted method throws, after rolling back the transaction
     */
    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        var txType = resolveTxType(ctx);
        return executor.execute(() -> {
            try {
                return ctx.proceed();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, returnValue -> {
            if (returnValue instanceof Result<?, ?> r) {
                return r.isError();
            }
            if (returnValue instanceof Try<?> t) {
                return t.isFailure();
            }
            return false;
        }, txType);
    }

    private static Transactional.TxType resolveTxType(InvocationContext ctx) {
        // Method-level annotations take precedence over class-level.
        var resultAnn = ctx.getMethod().getAnnotation(TransactionalResult.class);
        if (resultAnn != null) {
            return resultAnn.value();
        }

        var tryAnn = ctx.getMethod().getAnnotation(TransactionalTry.class);
        if (tryAnn != null) {
            return tryAnn.value();
        }

        // Fall back to the declaring class (supports class-level @TransactionalResult/Try).
        var declaring = ctx.getMethod().getDeclaringClass();
        resultAnn = declaring.getAnnotation(TransactionalResult.class);
        if (resultAnn != null) {
            return resultAnn.value();
        }

        tryAnn = declaring.getAnnotation(TransactionalTry.class);
        if (tryAnn != null) {
            return tryAnn.value();
        }

        return Transactional.TxType.REQUIRED;
    }
}
