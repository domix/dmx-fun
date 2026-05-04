package dmx.fun.quarkus;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;

/**
 * Package-private helper that executes a value-returning action inside a JTA-managed
 * transaction, marking it rollback-only when a caller-supplied predicate says so.
 *
 * <p>All public {@code Tx*} components and {@link TransactionalDmxInterceptor} delegate
 * to this class so the transactional bookkeeping lives in one place.
 */
@NullMarked
final class TxExecutor {

    private final TransactionManager transactionManager;

    TxExecutor(TransactionManager transactionManager) {
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager");
    }

    /**
     * Runs {@code action} inside a new JTA transaction.
     *
     * <p>If {@code shouldRollback} returns {@code true} for the action's result, the
     * transaction is rolled back. If the action throws, the exception propagates and
     * the transaction is rolled back. Otherwise the transaction commits.
     *
     * @param <T>            the return type of the action
     * @param action         the transactional action; must not be {@code null} and must not
     *                       return {@code null}
     * @param shouldRollback predicate that returns {@code true} when the result represents
     *                       a failure; must not be {@code null}
     * @return the value returned by {@code action}
     * @throws RuntimeException wrapping any JTA infrastructure exception, or re-throwing
     *                          any unchecked exception thrown by {@code action}
     */
    <T> T execute(Supplier<T> action, Predicate<T> shouldRollback) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(shouldRollback, "shouldRollback");
        try {
            transactionManager.begin();
            T result;
            try {
                result = action.get();
                Objects.requireNonNull(result, "action must not return null");
            } catch (RuntimeException e) {
                rollbackQuietly();
                throw e;
            }
            if (shouldRollback.test(result)) {
                transactionManager.rollback();
            } else {
                transactionManager.commit();
            }
            return result;
        } catch (NotSupportedException | SystemException | RollbackException
                 | HeuristicMixedException | HeuristicRollbackException e) {
            throw new RuntimeException("JTA transaction management failed", e);
        }
    }

    /**
     * Runs {@code action} in a brand-new JTA transaction, suspending any active transaction
     * first and resuming it afterwards (REQUIRES_NEW semantics).
     *
     * @param <T>            the return type of the action
     * @param action         the transactional action; must not be {@code null} and must not
     *                       return {@code null}
     * @param shouldRollback predicate that returns {@code true} when the result represents
     *                       a failure; must not be {@code null}
     * @return the value returned by {@code action}
     */
    <T> T executeNew(Supplier<T> action, Predicate<T> shouldRollback) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(shouldRollback, "shouldRollback");
        Transaction suspended;
        try {
            suspended = transactionManager.suspend();
        } catch (SystemException e) {
            throw new RuntimeException("JTA transaction management failed", e);
        }
        try {
            return execute(action, shouldRollback);
        } finally {
            if (suspended != null) {
                try {
                    transactionManager.resume(suspended);
                } catch (InvalidTransactionException | SystemException e) {
                    throw new RuntimeException("Failed to resume suspended transaction", e);
                }
            }
        }
    }

    private void rollbackQuietly() {
        try {
            transactionManager.rollback();
        } catch (SystemException ignored) {
        }
    }
}
