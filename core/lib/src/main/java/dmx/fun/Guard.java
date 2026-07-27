package dmx.fun;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A named, composable predicate that produces a {@link Validated} result when applied to a value.
 *
 * <p>{@code Guard<T>} is a {@code @FunctionalInterface} whose single abstract method is
 * {@link #check(Object) check(T)}, which returns
 * {@code Validated<NonEmptyList<String>, T>}: {@code Valid(value)} when the predicate passes,
 * or {@code Invalid(errors)} when it fails.
 * All composition operators ({@link #and}, {@link #or}, {@link #negate()}, {@link #andThen},
 * {@link #contramap}, {@link #withMessage}) are {@code default} methods, so guards can be
 * defined as lambdas and composed without inheritance; {@link #allOf} and {@link #anyOf}
 * compose several guards at once. For logging and metrics, {@link #named(String)} attaches a
 * descriptive identity retrievable via {@link #name()}. The choice to use {@code @FunctionalInterface} with
 * {@code default} methods rather than an abstract class is documented in
 * <a href="https://domix.github.io/dmx-fun/adr/adr-011-guard-functional-interface/">
 * ADR-011 — Guard&lt;T&gt; as a @FunctionalInterface with default methods</a>.
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
 * <h2>Error type</h2>
 * <p>The error type is fixed as {@link NonEmptyList}{@code <String>} — human-readable messages
 * accumulated across all failing guards. This design avoids requiring a {@code BinaryOperator<E>}
 * for merging in {@link #and}/{@link #or}, and guarantees at least one error is always present.
 * The trade-off is that typed domain error objects require working directly with
 * {@link Validated}{@code <E, A>} instead. This decision is documented in
 * <a href="https://domix.github.io/dmx-fun/adr/adr-005-guard-error-type/">
 * ADR-005 — Guard&lt;T&gt; accumulates errors as a fixed NonEmptyList&lt;String&gt;</a>.
 *
 * <h2>Composition semantics</h2>
 * <ul>
 *   <li>{@link #and(Guard) and} — both guards must pass; errors from all failing guards are
 *       accumulated (not fail-fast).</li>
 *   <li>{@link #or(Guard) or} — the first passing guard short-circuits; if all fail, all errors
 *       are accumulated.</li>
 *   <li>{@link #negate() negate} / {@link #negate(String) negate(message)} /
 *       {@link #negate(Function) negate(messageFromValue)} — inverts the predicate.</li>
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
     * <p><strong>Contract:</strong> a guard is a validator, not a transformer — an
     * implementation must return {@code Valid} of the checked value itself, never a
     * substituted or normalized one. The composition operators rely on this: they re-wrap the
     * original input, so any value produced by a contract-violating guard is discarded during
     * composition. Note also that {@code Valid} rejects {@code null}, so a guard over a
     * nullable type can never <em>pass</em> a {@code null} value — it must reject it (see
     * {@link #nonNull()}).
     *
     * @param value the value to validate
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
     * <p>The predicate must not throw: any exception it raises escapes {@link #check(Object)}
     * unwrapped, breaking the contract that a guard always returns a {@link Validated}. For
     * predicates that would throw on {@code null} input, compose with
     * {@link #nonNull()}{@code .andThen(...)} so the null check short-circuits first; for
     * predicates that may throw on other inputs (parsing, regex on malformed data), use
     * {@link #ofCatching(Predicate, String)}.
     *
     * @param <T>          the value type
     * @param predicate    the condition that must hold for the value to be valid; must not throw
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
     * <p>The predicate must not throw: any exception it raises escapes {@link #check(Object)}
     * unwrapped, breaking the contract that a guard always returns a {@link Validated}. For
     * predicates that would throw on {@code null} input, compose with
     * {@link #nonNull()}{@code .andThen(...)} so the null check short-circuits first; for
     * predicates that may throw on other inputs (parsing, regex on malformed data), use
     * {@link #ofCatching(Predicate, String)}.
     *
     * @param <T>            the value type
     * @param predicate      the condition that must hold for the value to be valid; must not
     *                       throw
     * @param errorMessageFn function that produces an error message from the failing value
     * @return a new {@code Guard<T>}
     * @throws NullPointerException if {@code predicate} or {@code errorMessageFn} is {@code null}
     */
    static <T> Guard<T> of(
        Predicate<? super T> predicate,
        Function<? super T, String> errorMessageFn
    ) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(errorMessageFn, "errorMessageFn");
        return value -> predicate.test(value)
            ? Validated.valid(value)
            : Validated.invalidNel(errorMessageFn.apply(value));
    }

    /**
     * Creates a {@code Guard<T>} from a predicate that may throw, treating a thrown
     * {@link RuntimeException} as a failed check.
     *
     * <p>Unlike {@link #of(Predicate, String)}, whose predicate must not throw, this factory
     * preserves the contract that a guard always returns a {@link Validated}: if the predicate
     * throws a {@code RuntimeException}, the guard returns {@code Invalid([errorMessage])}
     * exactly as if the predicate had returned {@code false}. {@link Error}s and other
     * {@link Throwable}s still propagate.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> numeric = Guard.ofCatching(
     *     s -> Integer.parseInt(s) >= 0,       // parseInt throws on non-numeric input
     *     "must be a non-negative number");
     *
     * numeric.check("42");   // Valid("42")
     * numeric.check("abc");  // Invalid(["must be a non-negative number"]) — exception folded
     * }</pre>
     *
     * @param <T>          the value type
     * @param predicate    the condition to evaluate; a thrown {@code RuntimeException} counts
     *                     as a failure
     * @param errorMessage the error message produced when the predicate fails or throws
     * @return a new {@code Guard<T>}
     * @throws NullPointerException if {@code predicate} or {@code errorMessage} is {@code null}
     */
    static <T> Guard<T> ofCatching(Predicate<? super T> predicate, String errorMessage) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(errorMessage, "errorMessage");
        return value -> {
            try {
                return predicate.test(value)
                    ? Validated.valid(value)
                    : Validated.invalidNel(errorMessage);
            } catch (RuntimeException _) {
                return Validated.invalidNel(errorMessage);
            }
        };
    }

    /**
     * Creates and returns a Guard instance that ensures a value is non-null.
     *
     * @param <T> the type of the value to be guarded
     * @return a Guard that validates the value is not null
     */
    static <T extends @Nullable Object> Guard<T> nonNull() {
        return Guard.of(Objects::nonNull, "must not be null");
    }

    /**
     * Returns a guard that passes only when <em>all</em> of the given guards pass.
     *
     * <p>Same semantics as chaining {@link #and(Guard) and}: every guard is always evaluated
     * and errors from all failing guards are accumulated in order — not fail-fast. The first
     * parameter is mandatory, so the composition is never empty. Unlike a manual {@code and}
     * chain, the guards are evaluated in a single pass with one error list.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> username = Guard.allOf(notBlank, minLength3, alphanumeric);
     *
     * username.check("a?");   // Invalid(["must be at least 3 chars", "must be alphanumeric"])
     * username.check("alice"); // Valid("alice")
     * }</pre>
     *
     * @param <T>   the value type
     * @param first the first guard; must not be {@code null}
     * @param rest  the remaining guards; must not be {@code null} or contain {@code null}
     * @return a composed {@code Guard<T>} requiring every guard to pass
     * @throws NullPointerException if {@code first}, {@code rest}, or any element is {@code null}
     */
    @SafeVarargs
    static <T> Guard<T> allOf(Guard<? super T> first, Guard<? super T>... rest) {
        List<Guard<? super T>> guards = collectGuards(first, rest);
        return value -> {
            List<String> errors = null;
            for (Guard<? super T> guard : guards) {
                var result = guard.check(value);
                if (result.isInvalid()) {
                    if (errors == null) {
                        errors = new ArrayList<>();
                    }
                    errors.addAll(result.getError().toList());
                }
            }
            return errors == null ? Validated.valid(value) : Validated.invalid(nel(errors));
        };
    }

    /**
     * Returns a guard that passes when <em>at least one</em> of the given guards passes.
     *
     * <p>Same semantics as chaining {@link #or(Guard) or}: evaluation short-circuits on the
     * first passing guard; if every guard fails, errors from all of them are accumulated in
     * order. The first parameter is mandatory, so the composition is never empty.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> contact = Guard.anyOf(email, phone);
     *
     * contact.check("alice@example.com"); // Valid — phone never evaluated
     * contact.check("hello");             // Invalid(["must contain @", "must be digits"])
     * }</pre>
     *
     * @param <T>   the value type
     * @param first the first guard; must not be {@code null}
     * @param rest  the remaining guards; must not be {@code null} or contain {@code null}
     * @return a composed {@code Guard<T>} requiring at least one guard to pass
     * @throws NullPointerException if {@code first}, {@code rest}, or any element is {@code null}
     */
    @SafeVarargs
    static <T> Guard<T> anyOf(Guard<? super T> first, Guard<? super T>... rest) {
        List<Guard<? super T>> guards = collectGuards(first, rest);
        return value -> {
            List<String> errors = new ArrayList<>();
            for (Guard<? super T> guard : guards) {
                var result = guard.check(value);
                if (result.isValid()) {
                    return Validated.valid(value);
                }
                errors.addAll(result.getError().toList());
            }
            return Validated.invalid(nel(errors));
        };
    }

    /**
     * Validates and copies the varargs guards into one list at composition time.
     */
    @SafeVarargs
    private static <T> List<Guard<? super T>> collectGuards(
        Guard<? super T> first,
        Guard<? super T>... rest
    ) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(rest, "rest");
        List<Guard<? super T>> guards = new ArrayList<>(rest.length + 1);
        guards.add(first);
        for (Guard<? super T> guard : rest) {
            guards.add(Objects.requireNonNull(guard, "rest must not contain null"));
        }
        return guards;
    }

    /**
     * Builds a {@code NonEmptyList} from a non-empty accumulation list.
     */
    private static NonEmptyList<String> nel(List<String> errors) {
        return NonEmptyList.of(errors.getFirst(), errors.subList(1, errors.size()));
    }

    /**
     * Adapts a {@code Guard<? super T>} to a {@code Guard<T>} by re-wrapping the checked value,
     * preserving the accumulated errors on failure.
     *
     * <p>Use this when assigning or passing a guard written against a supertype where a
     * {@code Guard<T>} is required outside the variance-aware combinators — the adaptation
     * cannot be written as a plain lambda because the {@code Validated} value types differ:
     *
     * <pre>{@code
     * Guard<CharSequence> notEmpty = Guard.of(cs -> !cs.isEmpty(), "must not be empty");
     * Guard<String> forStrings = Guard.narrow(notEmpty);
     * }</pre>
     *
     * @param <T>   the narrower value type
     * @param guard the guard written against a supertype; must not be {@code null}
     * @return a {@code Guard<T>} delegating to {@code guard}
     * @throws NullPointerException if {@code guard} is {@code null}
     */
    static <T> Guard<T> narrow(Guard<? super T> guard) {
        Objects.requireNonNull(guard, "guard");
        return keepName(guard, narrowRaw(guard));
    }

    /**
     * The adaptation behind {@link #narrow} without name preservation — used internally by
     * the composition operators, whose result is anonymous anyway.
     */
    private static <T> Guard<T> narrowRaw(Guard<? super T> guard) {
        return value -> guard.check(value).map(_ -> value);
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
     * Guard<Integer> positive = Guard.of(n -> n > 0,      "must be positive");
     * Guard<Integer> even     = Guard.of(n -> n % 2 == 0, "must be even");
     * Guard<Integer> positiveEven = positive.and(even);
     *
     * positiveEven.check(4);   // Valid(4)
     * positiveEven.check(3);   // Invalid(["must be even"])
     * positiveEven.check(-1);  // Invalid(["must be positive", "must be even"])
     *                          //  — both guards evaluated, both errors collected
     * }</pre>
     *
     * <p>The parameter is contravariant ({@code Guard<? super T>}), mirroring
     * {@link Predicate#and(Predicate) Predicate.and}, so a guard written against a supertype
     * (e.g. {@code Guard<CharSequence>}) can be composed into a {@code Guard<String>}.
     *
     * @param other the guard that must also pass; must not be {@code null}
     * @return a composed {@code Guard<T>}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    default Guard<T> and(Guard<? super T> other) {
        Objects.requireNonNull(other, "other");
        return value -> this.check(value)
            .combine(other.check(value), NonEmptyList::concat, (v1, _) -> v1);
    }

    /**
     * Returns a composed guard that evaluates {@code next} only when this guard passes.
     *
     * <p>Unlike {@link #and(Guard) and}, evaluation is <strong>short-circuit</strong>:
     * if this guard returns {@code Invalid}, {@code next} is never called and its error is
     * never accumulated. This makes {@code andThen} the safe choice when the downstream
     * guard's predicate would throw on the values rejected by this guard — most notably
     * when composing {@link #nonNull()} with a rule that dereferences the value:
     *
     * <pre>{@code
     * Guard<@Nullable String> nonNullAndNotBlank =
     *     Guard.<@Nullable String>nonNull()
     *         .andThen(Guard.<@Nullable String>of(s -> s != null && !s.isBlank(),
     *                                             "must not be blank"));
     *
     * nonNullAndNotBlank.check("hello"); // Valid("hello")
     * nonNullAndNotBlank.check(null);    // Invalid(["must not be null"]) — next not evaluated
     * nonNullAndNotBlank.check("   ");   // Invalid(["must not be blank"])
     * }</pre>
     *
     * <p>Use {@link #and(Guard) and} when you want both guards evaluated regardless of the
     * first result (error accumulation). Use {@code andThen} when the second guard must not
     * run until the first has passed.
     *
     * <p>The parameter is contravariant ({@code Guard<? super T>}); the composed guard
     * re-wraps the original value, so the result type stays {@code Guard<T>}.
     *
     * @param next the guard to evaluate when this guard passes; must not be {@code null}
     * @return a composed {@code Guard<T>}
     * @throws NullPointerException if {@code next} is {@code null}
     */
    default Guard<T> andThen(Guard<? super T> next) {
        Objects.requireNonNull(next, "next");
        Guard<T> narrowed = narrowRaw(next);
        return value -> {
            var first = this.check(value);
            return first.isInvalid() ? first : narrowed.check(value);
        };
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
     * <p>The parameter is contravariant ({@code Guard<? super T>}), mirroring
     * {@link Predicate#or(Predicate) Predicate.or}; the composed guard re-wraps the original
     * value, so the result type stays {@code Guard<T>}.
     *
     * @param other the alternative guard; must not be {@code null}
     * @return a composed {@code Guard<T>}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    default Guard<T> or(Guard<? super T> other) {
        Objects.requireNonNull(other, "other");
        Guard<T> narrowed = narrowRaw(other);
        return value -> {
            var left = this.check(value);
            if (left.isValid()) {
                return left;
            }
            var right = narrowed.check(value);
            if (right.isValid()) {
                return right;
            }
            return left.combine(right, NonEmptyList::concat, (v1, _) -> v1);
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
        return keepName(this, value -> this.check(value).isValid()
            ? Validated.invalidNel(errorMessage)
            : Validated.valid(value));
    }

    /**
     * Returns a guard that is the logical negation of this guard, using a dynamic error message.
     *
     * <p>The {@code messageFromValue} function receives the value that passed the original guard,
     * allowing a context-specific error message.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> notReserved = Guard.of(s -> s.equals("admin") || s.equals("root"), "")
     *     .negate(s -> "username '" + s + "' is reserved");
     *
     * notReserved.check("alice"); // Valid("alice")
     * notReserved.check("admin"); // Invalid(["username 'admin' is reserved"])
     * }</pre>
     *
     * @param messageFromValue function producing an error message from the passing value;
     *                         must not be {@code null}
     * @return the negated {@code Guard<T>}
     * @throws NullPointerException if {@code messageFromValue} is {@code null}
     */
    default Guard<T> negate(Function<? super T, String> messageFromValue) {
        Objects.requireNonNull(messageFromValue, "messageFromValue");
        return keepName(this, value -> this.check(value).isValid()
            ? Validated.invalidNel(messageFromValue.apply(value))
            : Validated.valid(value));
    }

    /**
     * Returns a guard that replaces any error messages produced by this guard with
     * {@code message}.
     *
     * <p>Useful when you want to expose a single clean message at a public API boundary
     * regardless of the internal validation details.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> username = notBlank.and(minLength3).withMessage("invalid username");
     *
     * username.check("");    // Invalid(["invalid username"])
     * username.check("a");   // Invalid(["invalid username"])
     * username.check("alice"); // Valid("alice")
     * }</pre>
     *
     * @param message the replacement error message; must not be {@code null}
     * @return a new {@code Guard<T>} that returns a single fixed error when this guard fails
     * @throws NullPointerException if {@code message} is {@code null}
     */
    default Guard<T> withMessage(String message) {
        Objects.requireNonNull(message, "message");
        return keepName(this, value -> this.check(value).isValid()
            ? Validated.valid(value)
            : Validated.invalidNel(message));
    }

    // -------------------------------------------------------------------------
    // Naming / observability
    // -------------------------------------------------------------------------

    /**
     * The name reported by {@link #name()} for guards that were never {@link #named(String)}.
     */
    String ANONYMOUS = "anonymous";

    /**
     * Returns a descriptive name for this guard, for logging and metrics.
     *
     * <p>Guards are anonymous by default; use {@link #named(String)} to attach a name. The
     * name identifies <em>which guard</em> was applied — a different piece of information
     * from the error messages, which say <em>which rule</em> was violated. It never alters
     * {@link #check(Object)} results or error messages.
     *
     * <p><strong>Note for implementors:</strong> this is a {@code default} method on a
     * public interface. A class implementing {@code Guard} that already declares its own
     * {@code String name()} accessor — a record with a {@code name} component, say —
     * silently overrides this method, and that value will surface wherever guard names are
     * logged or tagged. Rename such a component or override {@code name()} deliberately.
     *
     * @return the guard's name, or {@link #ANONYMOUS} when none was assigned
     */
    default String name() {
        return ANONYMOUS;
    }

    /**
     * Returns {@code true} when this guard carries a name assigned via {@link #named(String)}
     * (or an overridden {@link #name()}), {@code false} for anonymous guards.
     *
     * <p>Lets observability code skip or bucket unnamed guards without comparing against the
     * {@link #ANONYMOUS} literal.
     *
     * @return whether this guard has a non-default name
     */
    default boolean isNamed() {
        return !ANONYMOUS.equals(name());
    }

    /**
     * Returns a guard with the same {@link #check(Object)} behavior carrying a descriptive
     * name, retrievable via {@link #name()}.
     *
     * <p><strong>Name propagation.</strong> Operators that decorate this same guard —
     * {@link #withMessage}, {@link #mapMessages}, {@link #negate()}, {@link #contramap} and
     * {@link #narrow} — preserve the name. Operators that <em>compose</em> guards —
     * {@link #and}, {@link #or}, {@link #andThen}, {@link #allOf}, {@link #anyOf} — produce a
     * new anonymous guard, so name the composite after composing. The rationale is documented
     * in <a href="https://domix.github.io/dmx-fun/adr/adr-024-guard-naming/">ADR-024 — Guard
     * naming</a>.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> username = notBlank.and(minLength3).and(alphanumeric)
     *     .named("username");
     *
     * var result = username.check(input);
     * if (result.isInvalid()) {
     *     log.warn("guard '{}' failed: {}", username.name(), result.getError().toList());
     * }
     * }</pre>
     *
     * @param name the descriptive name; must not be {@code null}, blank, or the
     *             {@link #ANONYMOUS} sentinel (a guard named {@code "anonymous"} would be
     *             indistinguishable from an unnamed one)
     * @return a guard delegating to this one, with the given name
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank or equals {@link #ANONYMOUS}
     */
    default Guard<T> named(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (ANONYMOUS.equals(name)) {
            throw new IllegalArgumentException(
                "name must not be the reserved \"" + ANONYMOUS + "\" sentinel");
        }
        var self = this;
        return new Guard<>() {
            @Override
            public Validated<NonEmptyList<String>, T> check(T value) {
                return self.check(value);
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    /**
     * Re-attaches {@code source}'s name to {@code decorated} when one was assigned — the
     * single propagation site behind every unary decorator and {@link #narrow}.
     */
    private static <U> Guard<U> keepName(Guard<?> source, Guard<U> decorated) {
        return source.isNamed() ? decorated.named(source.name()) : decorated;
    }

    // -------------------------------------------------------------------------
    // Interoperability
    // -------------------------------------------------------------------------

    /**
     * Returns a standard {@link Predicate Predicate&lt;T&gt;} that returns {@code true} when this
     * guard passes and {@code false} when it fails.
     *
     * <p>Use this to integrate guards with standard Java APIs that accept {@code Predicate}
     * (e.g., {@link java.util.stream.Stream#filter Stream.filter},
     * {@link java.util.Collection#removeIf Collection.removeIf}).
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
     *
     * List<String> valid = Stream.of("alice", "  ", "bob", "")
     *     .filter(notBlank.asPredicate())
     *     .toList();
     * // ["alice", "bob"]
     * }</pre>
     *
     * @return a {@code Predicate<T>} backed by this guard
     */
    default Predicate<T> asPredicate() {
        return value -> this.check(value).isValid();
    }

    /**
     * Returns a {@code Guard<U>} that applies {@code mapper} to its input before checking.
     *
     * <p>This is the <em>contravariant map</em> operation: it adapts a guard written for type
     * {@code T} to work on an enclosing type {@code U} by projecting {@code U → T} first.
     * It is the idiomatic way to reuse field-level guards on whole objects.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "username must not be blank");
     *
     * // Lift notBlank to validate User objects by their username field
     * Guard<User> userGuard = notBlank.contramap(User::username);
     *
     * userGuard.check(new User("alice")); // Valid(user)
     * userGuard.check(new User("   "));   // Invalid(["username must not be blank"])
     * }</pre>
     *
     * @param <U>    the input type of the returned guard
     * @param mapper function that extracts the {@code T} value from a {@code U}; must not be
     *               {@code null}
     * @return a new {@code Guard<U>} that projects {@code U → T} before checking
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    default <U> Guard<U> contramap(Function<? super U, ? extends T> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return keepName(this, u -> this.check(mapper.apply(u)).map(_ -> u));
    }

    /**
     * Returns a {@code Guard<U>} that applies {@code mapper} to its input before checking,
     * prefixing every error message with {@code fieldName}.
     *
     * <p>Like {@link #contramap(Function)}, but each accumulated error is rewritten as
     * {@code "fieldName: originalMessage"}, so messages stay unambiguous when several
     * field-level guards are combined on the same object.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
     *
     * Guard<User> userGuard = Guard.allOf(
     *     notBlank.contramap(User::username, "username"),
     *     notBlank.contramap(User::email,    "email"));
     *
     * userGuard.check(new User(" ", " "));
     * // Invalid(["username: must not be blank", "email: must not be blank"])
     * }</pre>
     *
     * @param <U>       the input type of the returned guard
     * @param mapper    function that extracts the {@code T} value from a {@code U}; must not be
     *                  {@code null}
     * @param fieldName prefix identifying the projected field in error messages; must not be
     *                  {@code null}
     * @return a new {@code Guard<U>} that projects {@code U → T} before checking and prefixes
     *         errors with {@code fieldName}
     * @throws NullPointerException if {@code mapper} or {@code fieldName} is {@code null}
     */
    default <U> Guard<U> contramap(Function<? super U, ? extends T> mapper, String fieldName) {
        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(fieldName, "fieldName");
        return keepName(this, u -> this.check(mapper.apply(u))
            .map(_ -> u)
            .mapError(errors -> errors.map(message -> fieldName + ": " + message)));
    }

    /**
     * Returns a guard that rewrites every error message produced by this guard with
     * {@code transform}, leaving valid results untouched.
     *
     * <p>This is the general form behind {@link #contramap(Function, String)}'s field
     * prefixing — use it directly for any other message convention: nested paths, i18n keys,
     * suffixes, or structured formats.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> username = notBlank.and(minLength3)
     *     .mapMessages(m -> "user.name: " + m);
     *
     * username.check("");  // Invalid(["user.name: must not be blank", ...])
     * }</pre>
     *
     * <p>Unlike {@link #withMessage(String)}, which collapses all errors into one fixed
     * message, {@code mapMessages} transforms each accumulated message individually.
     *
     * @param transform function applied to each error message; must not be {@code null} and
     *                  must not return {@code null}
     * @return a new {@code Guard<T>} with rewritten error messages
     * @throws NullPointerException if {@code transform} is {@code null}
     */
    default Guard<T> mapMessages(Function<? super String, String> transform) {
        Objects.requireNonNull(transform, "transform");
        return keepName(this, value -> this.check(value).mapError(errors -> errors.map(transform)));
    }

    /**
     * Applies this guard to {@code value} and returns a
     * {@code Result<T, NonEmptyList<String>>}.
     *
     * <p>Equivalent to {@code this.check(value).toResult()} but removes the need to import and
     * chain the conversion manually.
     *
     * @param value the value to validate
     * @return {@code Result.ok(value)} if the guard passes, or
     *         {@code Result.err(errors)} if it fails
     */
    default Result<T, NonEmptyList<String>> checkToResult(T value) {
        return this.check(value).toResult();
    }

    /**
     * Applies this guard to {@code value} and returns a {@code Result<T, E>}, mapping the
     * accumulated error list to a domain-specific error type via {@code toError}.
     *
     * <p>Use this at domain service boundaries where {@code Result} is the preferred
     * container and the error type is richer than a plain list of strings.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> username = notBlank.and(minLength3);
     *
     * Result<String, ValidationException> result = username.checkToResult(
     *     input,
     *     errors -> new ValidationException("username", errors.toList())
     * );
     * }</pre>
     *
     * @param <E>     the domain error type
     * @param value   the value to validate
     * @param toError function mapping the accumulated error list to {@code E}
     * @return {@code Result.ok(value)} on success, or {@code Result.err(toError(errors))} on
     *         failure
     * @throws NullPointerException if {@code toError} is {@code null}
     */
    default <E> Result<T, E> checkToResult(
        T value,
        Function<? super NonEmptyList<String>, ? extends E> toError
    ) {
        Objects.requireNonNull(toError, "toError");
        Validated<E, T> mapped = this.check(value).mapError(toError);
        return mapped.toResult();
    }

    /**
     * Applies this guard to {@code value} and returns an {@link Option Option&lt;T&gt;}.
     *
     * <p>Returns {@code Some(value)} when the guard passes and {@link Option#none() None} when
     * it fails, discarding the error details. Use this when you only need to know whether a
     * value is valid, not why it is not.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
     *
     * // Filter a stream keeping only valid values
     * List<String> valid = Stream.of("alice", "  ", "bob")
     *     .flatMap(s -> notBlank.checkToOption(s).stream())
     *     .toList();
     * // ["alice", "bob"]
     * }</pre>
     *
     * <p>A guard can never produce {@code Valid(null)} — {@code Validated.Valid} rejects
     * {@code null} at construction — so the returned {@code Option} never wraps {@code null};
     * a guard over a nullable type must reject {@code null} (see {@link #check(Object)}).
     *
     * @param value the value to validate
     * @return {@code Option.some(value)} if the guard passes, or {@code Option.none()} if it
     *         fails
     */
    default Option<T> checkToOption(T value) {
        var result = this.check(value);
        return result.isValid() ? Option.some(result.get()) : Option.none();
    }

    /**
     * Applies this guard to {@code value} and returns an {@link Either Either&lt;NonEmptyList&lt;String&gt;, T&gt;}.
     *
     * <p>Returns {@code Either.right(value)} when the guard passes and
     * {@code Either.left(errors)} when it fails. Use this when downstream logic is already
     * expressed in terms of {@link Either}.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
     *
     * Either<NonEmptyList<String>, String> right = notBlank.checkToEither("hello");
     * // Either.right("hello")
     *
     * Either<NonEmptyList<String>, String> left = notBlank.checkToEither("   ");
     * // Either.left(NonEmptyList.of("must not be blank"))
     * }</pre>
     *
     * @param value the value to validate
     * @return {@code Either.right(value)} if the guard passes, or
     *         {@code Either.left(errors)} if it fails
     */
    default Either<NonEmptyList<String>, T> checkToEither(T value) {
        var result = this.check(value);
        return result.isValid()
            ? Either.right(result.get())
            : Either.left(result.getError());
    }

    /**
     * Applies this guard to {@code value} and returns a {@code Try<T>}.
     *
     * <p>Returns {@code Try.success(value)} when the guard passes. When it fails, the
     * accumulated error messages are joined with {@code "; "} and wrapped in an
     * {@link IllegalArgumentException}; for a {@link #named(String) named} guard the message
     * is prefixed with {@code "guard 'name': "} so the failure stays traceable after the
     * {@code Try} propagates. Use {@link #checkToTry(Object, Function)} to supply
     * a domain-specific exception instead.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
     *
     * Try<String> success = notBlank.checkToTry("hello");
     * // Try.success("hello")
     *
     * Try<String> failure = notBlank.checkToTry("   ");
     * // Try.failure(new IllegalArgumentException("must not be blank"))
     * }</pre>
     *
     * @param value the value to validate
     * @return {@code Try.success(value)} if the guard passes, or a {@code Try.failure} wrapping
     *         an {@link IllegalArgumentException} with the joined error messages
     */
    default Try<T> checkToTry(T value) {
        return checkToTry(
            value,
            errors -> new IllegalArgumentException(
                (isNamed() ? "guard '" + name() + "': " : "")
                    + String.join("; ", errors.toList())
            )
        );
    }

    /**
     * Applies this guard to {@code value} and returns a {@code Try<T>}, converting any
     * accumulated errors to a domain-specific {@link Throwable} via {@code toThrowable}.
     *
     * <p>Use this at boundaries where the caller controls the exception type.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> username = notBlank.and(minLength3);
     *
     * Try<String> result = username.checkToTry(
     *     input,
     *     errors -> new ValidationException(errors.toList())
     * );
     * }</pre>
     *
     * @param <X>         the throwable type
     * @param value       the value to validate
     * @param toThrowable function mapping the accumulated error list to a {@link Throwable};
     *                    must not be {@code null}
     * @return {@code Try.success(value)} if the guard passes, or
     *         {@code Try.failure(toThrowable(errors))} if it fails
     * @throws NullPointerException if {@code toThrowable} is {@code null}
     */
    default <X extends Throwable> Try<T> checkToTry(
        T value,
        Function<? super NonEmptyList<String>, ? extends X> toThrowable
    ) {
        Objects.requireNonNull(toThrowable, "toThrowable");
        var result = this.check(value);
        return result.isValid()
            ? Try.success(result.get())
            : Try.failure(toThrowable.apply(result.getError()));
    }

    /**
     * Applies this guard to {@code value} and returns a standard {@link Optional Optional&lt;T&gt;}.
     *
     * <p>Returns {@code Optional.of(value)} when the guard passes and {@link Optional#empty()}
     * when it fails, discarding the error details. Use this to integrate with Java standard
     * library APIs that work with {@link Optional}.
     *
     * <p>Example:
     * <pre>{@code
     * Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
     *
     * Optional<String> present = notBlank.checkToOptional("hello"); // Optional.of("hello")
     * Optional<String> empty   = notBlank.checkToOptional("   ");   // Optional.empty()
     * }</pre>
     *
     * <p>A guard can never produce {@code Valid(null)} — {@code Validated.Valid} rejects
     * {@code null} at construction — so this method never passes {@code null} to
     * {@link Optional#of(Object)}; a guard over a nullable type must reject {@code null}
     * (see {@link #check(Object)}). Note that {@code Optional.of(value)} wraps the
     * <em>input</em>: for a {@code Guard<@Nullable T>} checking a {@code null} input, the
     * guard must return {@code Invalid}, which yields {@link Optional#empty()} here.
     *
     * @param value the value to validate
     * @return {@code Optional.of(value)} if the guard passes,
     *         {@code Optional.empty()} if the guard fails
     */
    default Optional<T> checkToOptional(T value) {
        return this.check(value).isValid() ? Optional.of(value) : Optional.empty();
    }
}
