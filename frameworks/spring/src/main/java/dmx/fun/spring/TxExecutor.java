package dmx.fun.spring;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Package-private helper that executes a value-returning action inside a Spring-managed
 * transaction, marking it rollback-only when a caller-supplied predicate says so.
 *
 * <p>All public {@code Tx*} components delegate to this class so the transactional
 * bookkeeping lives in one place.
 */
@NullMarked
final class TxExecutor {

    private final PlatformTransactionManager txManager;

    TxExecutor(PlatformTransactionManager txManager) {
        this.txManager = Objects.requireNonNull(txManager, "txManager");
    }

    /**
     * Runs {@code action} inside a transaction defined by {@code def}.
     *
     * <p>If {@code shouldRollback} returns {@code true} for the action's result, the
     * transaction is marked rollback-only before the template commits. If the action
     * throws, the exception propagates and the transaction rolls back normally.
     *
     * @param <T>            the return type of the action
     * @param def            the transaction definition; must not be {@code null}
     * @param action         the transactional action; must not be {@code null} and must not
     *                       return {@code null}
     * @param shouldRollback predicate that returns {@code true} when the result represents
     *                       a failure; must not be {@code null}
     * @return the value returned by {@code action}
     * @throws NullPointerException if any argument is {@code null} or if {@code action}
     *                              returns {@code null}
     */
    @SuppressWarnings("NullAway")
    <T> T execute(TransactionDefinition def, Supplier<T> action, Predicate<T> shouldRollback) {
        Objects.requireNonNull(def, "def");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(shouldRollback, "shouldRollback");
        var template = new TransactionTemplate(txManager, def);
        var result = template.execute(status -> {
            var t = action.get();
            Objects.requireNonNull(t, "action must not return null");
            if (shouldRollback.test(t)) {
                status.setRollbackOnly();
            }
            return t;
        });
        // template.execute() returns null only if the callback returns null;
        // the requireNonNull inside the callback above prevents that.
        return Objects.requireNonNull(result);
    }
}
