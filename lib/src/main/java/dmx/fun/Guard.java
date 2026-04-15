package dmx.fun;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;

/**
 * A named, composable predicate that produces a {@link Validated} result when applied to a value.
 *
 * <p>{@code Guard<T>} is a functional interface whose single abstract method is
 * {@link #check(Object) check(T)}, which returns
 * {@code Validated<NonEmptyList<String>, T>}: {@code Valid(value)} when the predicate passes,
 * or {@code Invalid(errors)} when it fails.
 *
 * <p>Guards are designed to be defined once and reused across validation pipelines, eliminating
 * the repetitive {@code if}/{@link Validated#invalidNel(Object)} pattern:
 *
 * <pre>{@code
 * Guard<String> notBlank     = Guard.of(s -> !s.isBlank(),         "must not be blank");
 * Guard<String> minLength3   = Guard.of(s -> s.length() >= 3,      "must be at least 3 chars");
 * Guard<String> alphanumeric = Guard.of(s -> s.matches("[\\w]+"),  "must be alphanumeric");
 *
 * Guard<String> username = notBlank.and(minLength3).and(alphanumeric);
 *
 * username.check("al");  // Invalid(["must be at least 3 chars"])
 * username.check("ok?"); // Invalid(["must be alphanumeric"])
 * username.check("alice"); // Valid("alice")
 * }</pre>
 *
 * <h2>Composition semantics</h2>
 * <ul>
 *   <li>{@link #and(Guard) and} — both guards must pass; errors from all failing guards are
 *       accumulated (not fail-fast).</li>
 *   <li>{@link #or(Guard) or} — the first passing guard short-circuits; if all fail, all errors
 *       are accumulated.</li>
 *   <li>{@link #negate() negate} / {@link #negate(String) negate(message)} — inverts the
 *       predicate.</li>
 * </ul>
 *
 * @param <T> the type of value being validated
 */
@FunctionalInterface
@NullMarked
public interface Guard<T> {

    // -------------------------------------------------------------------------
    // Core method
    // -------------------------------------------------------------------------

    /**
     * Applies this guard to {@code value}.
     *
     * @param value the value to validate; must not be {@code null}
     * @return {@code Valid(value)} if the predicate passes, or
     *         {@code Invalid(errors)} if it fails
     */
    Validated<NonEmptyList<String>, T> check(T value);

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code Guard<T>} from a predicate and a static error message.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
     * }</pre>
     *
     * @param <T>          the value type
     * @param predicate    the condition that must hold for the value to be valid
     * @param errorMessage the error message produced when the predicate fails
     * @return a new {@code Guard<T>}
     * @throws NullPointerException if {@code predicate} or {@code errorMessage} is {@code null}
     */
    static <T> Guard<T> of(Predicate<? super T> predicate, String errorMessage) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(errorMessage, "errorMessage");
        return value -> predicate.test(value)
            ? Validated.valid(value)
            : Validated.invalidNel(errorMessage);
    }

    /**
     * Creates a {@code Guard<T>} from a predicate and a dynamic error message function.
     *
     * <p>The {@code errorMessageFn} receives the failing value so it can produce a
     * context-specific message.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<Integer> max = Guard.of(
     *     n -> n <= 100,
     *     n -> "must be ≤ 100, got " + n
     * );
     * }</pre>
     *
     * @param <T>            the value type
     * @param predicate      the condition that must hold for the value to be valid
     * @param errorMessageFn function that produces an error message from the failing value
     * @return a new {@code Guard<T>}
     * @throws NullPointerException if {@code predicate} or {@code errorMessageFn} is {@code null}
     */
    static <T> Guard<T> of(Predicate<? super T> predicate, Function<? super T, String> errorMessageFn) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(errorMessageFn, "errorMessageFn");
        return value -> predicate.test(value)
            ? Validated.valid(value)
            : Validated.invalidNel(errorMessageFn.apply(value));
    }

    // -------------------------------------------------------------------------
    // Composition
    // -------------------------------------------------------------------------

    /**
     * Returns a composed guard that requires <em>both</em> this guard and {@code other} to pass.
     *
     * <p>Both guards are always evaluated — this is <strong>not</strong> fail-fast. Errors from
     * all failing guards are accumulated into a single {@code NonEmptyList}, so the caller
     * receives a complete picture of all violations at once.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<Integer> positive    = Guard.of(n -> n > 0,   "must be positive");
     * Guard<Integer> lessThan100 = Guard.of(n -> n < 100, "must be less than 100");
     * Guard<Integer> range = positive.and(lessThan100);
     *
     * range.check(-5);  // Invalid(["must be positive", "must be less than 100"])
     *                   //  — both guards evaluated, both errors collected
     * }</pre>
     *
     * @param other the guard that must also pass; must not be {@code null}
     * @return a composed {@code Guard<T>}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    default Guard<T> and(Guard<T> other) {
        Objects.requireNonNull(other, "other");
        return value -> this.check(value)
            .combine(other.check(value), NonEmptyList::concat, (v1, v2) -> v1);
    }

    /**
     * Returns a composed guard that passes when <em>at least one</em> of this guard or
     * {@code other} passes.
     *
     * <p>Evaluation is <strong>short-circuit</strong>: if this guard passes, {@code other} is
     * never evaluated. If both fail, errors from both guards are accumulated.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> email = Guard.of(s -> s.contains("@"),  "must contain @");
     * Guard<String> phone = Guard.of(s -> s.matches("\\d+"), "must be digits");
     * Guard<String> contact = email.or(phone);
     *
     * contact.check("alice@example.com");  // Valid — email passes, phone not evaluated
     * contact.check("12345");             // Valid — phone passes
     * contact.check("hello");             // Invalid(["must contain @", "must be digits"])
     * }</pre>
     *
     * @param other the alternative guard; must not be {@code null}
     * @return a composed {@code Guard<T>}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    default Guard<T> or(Guard<T> other) {
        Objects.requireNonNull(other, "other");
        return value -> {
            Validated<NonEmptyList<String>, T> left = this.check(value);
            if (left.isValid()) return left;
            Validated<NonEmptyList<String>, T> right = other.check(value);
            if (right.isValid()) return right;
            return left.combine(right, NonEmptyList::concat, (v1, v2) -> v1);
        };
    }

    /**
     * Returns a guard that is the logical negation of this guard, using a generic error message.
     *
     * <p>The composed guard returns {@code Valid(value)} when this guard <em>fails</em>, and
     * {@code Invalid(["must not satisfy the condition"])} when this guard <em>passes</em>.
     * Use {@link #negate(String) negate(message)} to supply a domain-specific error message.
     *
     * @return the negated {@code Guard<T>}
     */
    default Guard<T> negate() {
        return negate("must not satisfy the condition");
    }

    /**
     * Returns a guard that is the logical negation of this guard, using the supplied error
     * message when the original guard passes.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> notAdmin = Guard.of(s -> s.equals("admin"), "is admin")
     *                               .negate("username must not be 'admin'");
     *
     * notAdmin.check("alice"); // Valid("alice")
     * notAdmin.check("admin"); // Invalid(["username must not be 'admin'"])
     * }</pre>
     *
     * @param errorMessage the error message returned when the original guard passes
     * @return the negated {@code Guard<T>}
     * @throws NullPointerException if {@code errorMessage} is {@code null}
     */
    default Guard<T> negate(String errorMessage) {
        Objects.requireNonNull(errorMessage, "errorMessage");
        return value -> this.check(value).isValid()
            ? Validated.invalidNel(errorMessage)
            : Validated.valid(value);
    }
}
