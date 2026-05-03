package dmx.fun.quarkus;

import dmx.fun.Result;
import dmx.fun.Try;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.UserTransaction;
import org.jspecify.annotations.NullMarked;

/**
 * CDI interceptor that backs {@link TransactionalResult} and {@link TransactionalTry}.
 *
 * <p>Activates on any method annotated with either {@link TransactionalResult} or
 * {@link TransactionalTry} (both carry the shared {@link DmxTransactionalBinding}
 * meta-interceptor-binding, per CDI §2.7.1.1). For each intercepted method the
 * interceptor:
 * <ol>
 *   <li>Begins a new JTA transaction.</li>
 *   <li>Proceeds with the method invocation.</li>
 *   <li>Rolls back if the returned {@code Result} is error or {@code Try} is failure.</li>
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
    TransactionalDmxInterceptor(UserTransaction userTransaction) {
        this.executor = new TxExecutor(userTransaction);
    }

    /**
     * Wraps the intercepted method invocation in a JTA transaction, rolling back when
     * the returned {@link Result} is error or {@link Try} is failure.
     *
     * @param ctx the invocation context; must not be {@code null}
     * @return the value returned by the intercepted method
     * @throws Exception if the intercepted method throws, after rolling back the transaction
     */
    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        boolean isResult = ctx.getMethod().isAnnotationPresent(TransactionalResult.class)
            || ctx.getTarget().getClass().isAnnotationPresent(TransactionalResult.class);
        boolean isTry = ctx.getMethod().isAnnotationPresent(TransactionalTry.class)
            || ctx.getTarget().getClass().isAnnotationPresent(TransactionalTry.class);

        return executor.execute(() -> {
            try {
                return ctx.proceed();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, returnValue -> {
            if (isResult && returnValue instanceof Result<?, ?> r) {
                return r.isError();
            }
            if (isTry && returnValue instanceof Try<?> t) {
                return t.isFailure();
            }
            return false;
        });
    }
}
