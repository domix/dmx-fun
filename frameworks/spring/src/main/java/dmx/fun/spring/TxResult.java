package dmx.fun.spring;

import dmx.fun.Result;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Spring component that executes a {@link Result}-returning action inside a managed transaction,
 * automatically rolling back when the result represents a failure.
 *
 * <p>Spring's {@code @Transactional} rolls back only when an unchecked exception escapes the
 * annotated method. Since {@code Result<V,E>} captures failure as a return value, no exception
 * escapes, and the transaction commits even on error — silently persisting partial writes.
 * {@code TxResult} solves this by inspecting the returned {@code Result}: if
 * {@link Result#isError()} is {@code true}, the transaction is marked rollback-only before the
 * template commits.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * @Service
 * public class OrderService {
 *     private final TxResult tx;
 *
 *     public OrderService(TxResult tx) { this.tx = tx; }
 *
 *     public Result<Order, OrderError> createOrder(OrderRequest req) {
 *         return tx.execute(() ->
 *             validate(req)
 *                 .flatMap(this::persistOrder)
 *                 .flatMap(this::notifyInventory)
 *         );
 *     }
 * }
 * }</pre>
 *
 * <p>Wire this bean by declaring a {@link PlatformTransactionManager} in your Spring context.
 * Spring Boot auto-configures one for every registered {@code DataSource}.
 *
 * @see TxTry
 */
@NullMarked
@Component
public class TxResult {

    private final TxExecutor executor;

    /**
     * Creates a {@code TxResult} backed by the given transaction manager.
     *
     * @param txManager the transaction manager; must not be {@code null}
     * @throws NullPointerException if {@code txManager} is {@code null}
     */
    public TxResult(PlatformTransactionManager txManager) {
        this.executor = new TxExecutor(txManager);
    }

    /**
     * Executes {@code action} inside a transaction using the default
     * {@link TransactionDefinition} (propagation REQUIRED, isolation DEFAULT).
     *
     * <p>The transaction commits if the action returns {@link Result#isOk()}.
     * The transaction is rolled back when:
     * <ul>
     *   <li>the action returns {@link Result#isError()}, or</li>
     *   <li>the action throws an unchecked exception (propagates to the caller).</li>
     * </ul>
     *
     * @param <V>    the success value type
     * @param <E>    the error type
     * @param action the transactional action; must not be {@code null} and must not return
     *               {@code null}
     * @return the {@link Result} returned by {@code action}
     * @throws NullPointerException if {@code action} is {@code null} or returns {@code null}
     */
    public <V, E> Result<V, E> execute(Supplier<Result<V, E>> action) {
        Objects.requireNonNull(action, "action");
        return execute(new DefaultTransactionDefinition(), action);
    }

    /**
     * Executes {@code action} inside a transaction configured by {@code def}.
     *
     * <p>Use this overload when you need explicit control over propagation, isolation level,
     * timeout, or read-only flag:
     *
     * <pre>{@code
     * var readOnly = new DefaultTransactionDefinition();
     * readOnly.setReadOnly(true);
     *
     * Result<List<Order>, String> orders = tx.execute(readOnly, orderRepo::findAll);
     * }</pre>
     *
     * @param <V>    the success value type
     * @param <E>    the error type
     * @param def    the transaction definition; must not be {@code null}
     * @param action the transactional action; must not be {@code null} and must not return
     *               {@code null}
     * @return the {@link Result} returned by {@code action}
     * @throws NullPointerException if any argument is {@code null} or if {@code action}
     *                              returns {@code null}
     */
    public <V, E> Result<V, E> execute(TransactionDefinition def, Supplier<Result<V, E>> action) {
        return executor.execute(def, action, Result::isError);
    }
}
