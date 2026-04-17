package dmx.fun.spring;

import dmx.fun.Validated;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring component that executes a {@link Validated}-returning action inside a managed
 * transaction, automatically rolling back when the result is {@link Validated#isInvalid()}.
 *
 * <p>Spring's {@code @Transactional} rolls back only when an unchecked exception escapes the
 * annotated method. Since {@code Validated<E,A>} captures validation failure as a return value,
 * no exception escapes, and the transaction commits even on invalid outcomes — silently
 * persisting partial writes. {@code TxValidated} solves this by inspecting the returned
 * {@code Validated}: if {@link Validated#isInvalid()} is {@code true}, the transaction is
 * marked rollback-only before the template commits.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * @Service
 * public class RegistrationService {
 *     private final TxValidated tx;
 *     private final UserRepository repo;
 *
 *     public RegistrationService(TxValidated tx, UserRepository repo) {
 *         this.tx = tx; this.repo = repo;
 *     }
 *
 *     public Validated<NonEmptyList<String>, User> register(RegistrationRequest req) {
 *         return tx.execute(() ->
 *             validateName(req)
 *                 .combine(validateEmail(req), (name, email) -> new UserDraft(name, email))
 *                 .map(repo::save)
 *         );
 *         // • Valid(user)     → transaction commits
 *         // • Invalid(errors) → transaction rolls back; partial writes undone
 *     }
 * }
 * }</pre>
 *
 * <p>Wire this bean by declaring a {@link PlatformTransactionManager} in your Spring context.
 * Spring Boot auto-configures one for every registered {@code DataSource}.
 *
 * @see TxResult
 * @see TxTry
 */
@NullMarked
@Component
public class TxValidated {

    private final PlatformTransactionManager txManager;

    /**
     * Creates a {@code TxValidated} backed by the given transaction manager.
     *
     * @param txManager the transaction manager; must not be {@code null}
     * @throws NullPointerException if {@code txManager} is {@code null}
     */
    public TxValidated(PlatformTransactionManager txManager) {
        this.txManager = Objects.requireNonNull(txManager, "txManager");
    }

    /**
     * Executes {@code action} inside a transaction using the default
     * {@link TransactionDefinition} (propagation REQUIRED, isolation DEFAULT).
     *
     * <p>The transaction commits if the action returns {@link Validated#isValid()}.
     * The transaction is rolled back when:
     * <ul>
     *   <li>the action returns {@link Validated#isInvalid()}, or</li>
     *   <li>the action throws an unchecked exception (propagates to the caller).</li>
     * </ul>
     *
     * @param <E>    the error accumulation type
     * @param <A>    the valid value type
     * @param action the transactional action; must not be {@code null} and must not return
     *               {@code null}
     * @return the {@link Validated} returned by {@code action}
     * @throws NullPointerException if {@code action} is {@code null} or returns {@code null}
     */
    public <E, A> Validated<E, A> execute(Supplier<Validated<E, A>> action) {
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
     * var serializable = new DefaultTransactionDefinition();
     * serializable.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
     *
     * Validated<NonEmptyList<String>, User> result =
     *     tx.execute(serializable, () -> validateAndPersist(req));
     * }</pre>
     *
     * @param <E>    the error accumulation type
     * @param <A>    the valid value type
     * @param def    the transaction definition; must not be {@code null}
     * @param action the transactional action; must not be {@code null} and must not return
     *               {@code null}
     * @return the {@link Validated} returned by {@code action}
     * @throws NullPointerException if any argument is {@code null} or if {@code action}
     *                              returns {@code null}
     */
    @SuppressWarnings("NullAway")
    public <E, A> Validated<E, A> execute(TransactionDefinition def, Supplier<Validated<E, A>> action) {
        Objects.requireNonNull(def, "def");
        Objects.requireNonNull(action, "action");
        var template = new TransactionTemplate(txManager, def);
        var result = template.execute(status -> {
            var v = action.get();
            Objects.requireNonNull(v, "action must not return null");
            if (v.isInvalid()) {
                status.setRollbackOnly();
            }
            return v;
        });
        // template.execute() returns null only if the callback returns null;
        // the requireNonNull inside the callback above prevents that.
        return Objects.requireNonNull(result);
    }
}
