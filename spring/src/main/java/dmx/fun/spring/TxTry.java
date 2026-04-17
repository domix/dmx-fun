package dmx.fun.spring;

import dmx.fun.Try;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring component that executes a {@link Try}-returning action inside a managed transaction,
 * automatically rolling back when the result represents a failure.
 *
 * <p>Spring's {@code @Transactional} rolls back only when an unchecked exception escapes the
 * annotated method. Since {@code Try<V>} captures failure as a return value, no exception
 * escapes, and the transaction commits even on failure — silently persisting partial writes.
 * {@code TxTry} solves this by inspecting the returned {@code Try}: if
 * {@link Try#isFailure()} is {@code true}, the transaction is marked rollback-only before the
 * template commits.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * @Service
 * public class ReportService {
 *     private final TxTry tx;
 *
 *     public ReportService(TxTry tx) { this.tx = tx; }
 *
 *     public Try<Report> generate(ReportRequest req) {
 *         return tx.execute(() ->
 *             Try.of(() -> reportRepo.save(build(req)))
 *                .flatMap(r -> Try.of(() -> auditLog.record(r)))
 *         );
 *     }
 * }
 * }</pre>
 *
 * <p>Wire this bean by declaring a {@link PlatformTransactionManager} in your Spring context.
 * Spring Boot auto-configures one for every registered {@code DataSource}.
 *
 * @see TxResult
 */
@NullMarked
@Component
public class TxTry {

    private final PlatformTransactionManager txManager;

    /**
     * Creates a {@code TxTry} backed by the given transaction manager.
     *
     * @param txManager the transaction manager; must not be {@code null}
     * @throws NullPointerException if {@code txManager} is {@code null}
     */
    public TxTry(PlatformTransactionManager txManager) {
        this.txManager = Objects.requireNonNull(txManager, "txManager");
    }

    /**
     * Executes {@code action} inside a transaction using the default
     * {@link TransactionDefinition} (propagation REQUIRED, isolation DEFAULT).
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
     * Try<List<Report>> reports = tx.execute(readOnly, reportRepo::findAll);
     * }</pre>
     *
     * @param <V>    the success value type
     * @param def    the transaction definition; must not be {@code null}
     * @param action the transactional action; must not be {@code null} and must not return
     *               {@code null}
     * @return the {@link Try} returned by {@code action}
     * @throws NullPointerException if any argument is {@code null} or if {@code action}
     *                              returns {@code null}
     */
    @SuppressWarnings("NullAway")
    public <V> Try<V> execute(TransactionDefinition def, Supplier<Try<V>> action) {
        Objects.requireNonNull(def, "def");
        Objects.requireNonNull(action, "action");
        var template = new TransactionTemplate(txManager, def);
        var result = template.execute(status -> {
            var t = action.get();
            Objects.requireNonNull(t, "action must not return null");
            if (t.isFailure()) {
                status.setRollbackOnly();
            }
            return t;
        });
        // template.execute() returns null only if the callback returns null;
        // the requireNonNull inside the callback above prevents that.
        return Objects.requireNonNull(result);
    }
}
