package dmx.fun.quarkus;

import dmx.fun.Try;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * CDI bean that executes a {@link Try}-returning action inside a JTA-managed
 * transaction, automatically rolling back when the result represents a failure.
 *
 * <p>Quarkus's {@code @Transactional} rolls back only when an unchecked exception
 * escapes the annotated method. Since {@code Try<V>} captures failure as a return value,
 * no exception escapes, and the transaction commits even on failure — silently persisting
 * partial writes. {@code TxTry} solves this by inspecting the returned {@code Try}: if
 * {@link Try#isFailure()} is {@code true}, the transaction is rolled back before returning.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * @ApplicationScoped
 * public class ReportService {
 *     @Inject TxTry tx;
 *
 *     public Try<Report> generate(ReportRequest req) {
 *         return tx.execute(() ->
 *             Try.of(() -> reportRepo.save(build(req)))
 *                .map(r -> { auditLog.record(r); return r; })
 *         );
 *     }
 * }
 * }</pre>
 *
 * @see TxResult
 * @see TransactionalTry
 */
@NullMarked
@ApplicationScoped
public class TxTry {

    private final @Nullable TxExecutor executor;

    @Inject
    TxTry(TransactionManager transactionManager) {
        this.executor = new TxExecutor(transactionManager);
    }

    protected TxTry() {
        // required by CDI for proxy subclass generation
        this.executor = null;
    }

    /**
     * Executes {@code action} inside a new JTA transaction.
     *
     * <p>The transaction commits if the action returns {@link Try#isSuccess()}.
     * The transaction is rolled back when:
     * <ul>
     *   <li>the action returns {@link Try#isFailure()}, or</li>
     *   <li>the action throws an unchecked exception (propagates to the caller).</li>
     * </ul>
     *
     * @param <V>    the success value type
     * @param action the transactional action; must not be {@code null} and must not return
     *               {@code null}
     * @return the {@link Try} returned by {@code action}
     * @throws NullPointerException if {@code action} is {@code null} or returns {@code null}
     */
    public <V> Try<V> execute(Supplier<Try<V>> action) {
        Objects.requireNonNull(action, "action");
        return executor.execute(action, Try::isFailure);
    }

    /**
     * Executes {@code action} in a brand-new JTA transaction (REQUIRES_NEW semantics),
     * suspending any currently active transaction first and resuming it afterwards.
     *
     * @param <V>    the success value type
     * @param action the transactional action; must not be {@code null} and must not return
     *               {@code null}
     * @return the {@link Try} returned by {@code action}
     * @throws NullPointerException if {@code action} is {@code null} or returns {@code null}
     */
    public <V> Try<V> executeNew(Supplier<Try<V>> action) {
        Objects.requireNonNull(action, "action");
        return executor.executeNew(action, Try::isFailure);
    }
}
