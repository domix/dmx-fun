package dmx.fun.quarkus;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionRequiredException;
import jakarta.transaction.Transactional;
import jakarta.transaction.TransactionalException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
     * Runs {@code action} inside a new JTA transaction (programmatic API).
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
            resumeIfPresent(suspended);
        }
    }

    /**
     * Runs {@code action} applying JTA semantics described by {@code txType}.
     *
     * <p>Used by the declarative interceptor; callers that always want a new transaction
     * should use {@link #execute(Supplier, Predicate)} or {@link #executeNew(Supplier, Predicate)}
     * directly.
     *
     * @param <T>            the return type of the action
     * @param action         the transactional action; must not be {@code null} and must not
     *                       return {@code null}
     * @param shouldRollback predicate that returns {@code true} when the result represents
     *                       a failure; must not be {@code null}
     * @param txType         the JTA propagation type; must not be {@code null}
     * @return the value returned by {@code action}
     * @throws TransactionalException if MANDATORY is requested with no active transaction,
     *                                or NEVER is requested with an active transaction
     */
    <T> T execute(Supplier<T> action, Predicate<T> shouldRollback, Transactional.TxType txType) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(shouldRollback, "shouldRollback");
        Objects.requireNonNull(txType, "txType");
        return switch (txType) {
            case REQUIRED      -> executeRequired(action, shouldRollback);
            case REQUIRES_NEW  -> executeNew(action, shouldRollback);
            case MANDATORY     -> executeMandatory(action, shouldRollback);
            case SUPPORTS      -> executeSupports(action, shouldRollback);
            case NOT_SUPPORTED -> executeNotSupported(action, shouldRollback);
            case NEVER         -> executeNever(action, shouldRollback);
        };
    }

    // ── TxType implementations ────────────────────────────────────────────────

    private <T> T executeRequired(Supplier<T> action, Predicate<T> shouldRollback) {
        if (hasActiveTransaction()) {
            return executeJoined(action, shouldRollback);
        }
        return execute(action, shouldRollback);
    }

    private <T> T executeMandatory(Supplier<T> action, Predicate<T> shouldRollback) {
        if (!hasActiveTransaction()) {
            throw new TransactionalException(
                "MANDATORY: no active transaction",
                new TransactionRequiredException());
        }
        return executeJoined(action, shouldRollback);
    }

    private <T> T executeSupports(Supplier<T> action, Predicate<T> shouldRollback) {
        if (hasActiveTransaction()) {
            return executeJoined(action, shouldRollback);
        }
        return runWithoutTx(action);
    }

    private <T> T executeNotSupported(Supplier<T> action, Predicate<T> shouldRollback) {
        @Nullable Transaction suspended = null;
        try {
            if (hasActiveTransaction()) {
                suspended = transactionManager.suspend();
            }
        } catch (SystemException e) {
            throw new RuntimeException("JTA transaction management failed", e);
        }
        try {
            return runWithoutTx(action);
        } finally {
            resumeIfPresent(suspended);
        }
    }

    private <T> T executeNever(Supplier<T> action, Predicate<T> shouldRollback) {
        if (hasActiveTransaction()) {
            throw new TransactionalException(
                "NEVER: active transaction found",
                new InvalidTransactionException());
        }
        return runWithoutTx(action);
    }

    /**
     * Runs within the CALLER'S transaction (join semantics).
     *
     * <p>Does NOT begin, commit, or rollback — the caller owns the transaction boundary.
     * Marks the transaction rollback-only when {@code shouldRollback} returns {@code true}
     * or when the action or predicate throws any throwable, including {@link Error}.
     * If {@link #setRollbackOnlyQuietly()} itself fails with an infrastructure exception,
     * that exception is attached as a suppressed exception to the original throwable so
     * neither failure is silently lost.
     */
    private <T> T executeJoined(Supplier<T> action, Predicate<T> shouldRollback) {
        T result;
        try {
            result = action.get();
            Objects.requireNonNull(result, "action must not return null");
        } catch (RuntimeException | Error appEx) {
            try {
                setRollbackOnlyQuietly();
            } catch (RuntimeException infraEx) {
                appEx.addSuppressed(infraEx);
            }
            throw appEx;
        }
        boolean rollback;
        try {
            rollback = shouldRollback.test(result);
        } catch (RuntimeException | Error predicateEx) {
            try {
                setRollbackOnlyQuietly();
            } catch (RuntimeException infraEx) {
                predicateEx.addSuppressed(infraEx);
            }
            throw predicateEx;
        }
        if (rollback) {
            setRollbackOnlyQuietly(); // no original exception — infra failure propagates directly
        }
        return result;
    }

    /** Runs the action outside of any JTA transaction (no begin/commit/rollback). */
    private <T> T runWithoutTx(Supplier<T> action) {
        T result = action.get();
        Objects.requireNonNull(result, "action must not return null");
        return result;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private boolean hasActiveTransaction() {
        try {
            int status = transactionManager.getStatus();
            // STATUS_MARKED_ROLLBACK is still a joinable/suspendable transaction —
            // treat it as active so propagation decisions (REQUIRED join, MANDATORY
            // join, NOT_SUPPORTED suspend, NEVER throw) remain correct even after
            // setRollbackOnly() has been called on an enclosing transaction.
            return status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException e) {
            throw new RuntimeException("JTA transaction management failed", e);
        }
    }

    private void resumeIfPresent(@Nullable Transaction suspended) {
        if (suspended != null) {
            try {
                transactionManager.resume(suspended);
            } catch (InvalidTransactionException | SystemException e) {
                throw new RuntimeException("Failed to resume suspended transaction", e);
            }
        }
    }

    private void rollbackQuietly() {
        try {
            transactionManager.rollback();
        } catch (SystemException _) {
        }
    }

    private void setRollbackOnlyQuietly() {
        try {
            transactionManager.setRollbackOnly();
        } catch (SystemException e) {
            throw new RuntimeException("JTA transaction management failed", e);
        }
    }
}
