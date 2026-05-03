package dmx.fun.quarkus;

import dmx.fun.Result;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * CDI bean that executes a {@link Result}-returning action inside a JTA-managed
 * transaction, automatically rolling back when the result represents a failure.
 *
 * <p>Quarkus's {@code @Transactional} rolls back only when an unchecked exception
 * escapes the annotated method. Since {@code Result<V, E>} captures failure as a
 * return value, no exception escapes, and the transaction commits even on error —
 * silently persisting partial writes. {@code TxResult} solves this by inspecting the
 * returned {@code Result}: if {@link Result#isError()} is {@code true}, the transaction
 * is rolled back before returning.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * @ApplicationScoped
 * public class OrderService {
 *     @Inject TxResult tx;
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
 * @see TxTry
 * @see TransactionalResult
 */
@NullMarked
@ApplicationScoped
public class TxResult {

    private final @Nullable TxExecutor executor;

    @Inject
    TxResult(UserTransaction userTransaction) {
        this.executor = new TxExecutor(userTransaction);
    }

    protected TxResult() {
        // required by CDI for proxy subclass generation
        this.executor = null;
    }

    /**
     * Executes {@code action} inside a new JTA transaction.
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
        return executor.execute(action, Result::isError);
    }
}
