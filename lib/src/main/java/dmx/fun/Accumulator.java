package dmx.fun;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An immutable pair of a computed value {@code A} and a side-channel accumulation {@code E}.
 *
 * <p>{@code Accumulator<E, A>} is the functional alternative to mutable global state for
 * cross-cutting concerns such as logging, metrics, audit trails, and diagnostics. It threads
 * a growing accumulation value through a computation chain without passing it explicitly as
 * an argument or storing it in a shared mutable field.
 *
 * <p>The key invariant: <strong>accumulation always continues</strong>. Unlike
 * {@link Result} or {@link Try}, there is no failure path — every step contributes to both
 * the value and the accumulation. This makes {@code Accumulator} the natural choice when you
 * want to record a trace of what happened, not just whether it succeeded.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * // ---- Build a traced computation ----
 *
 * Accumulator<List<String>, Integer> step1 = Accumulator.of(10, List.of("loaded base value"));
 *
 * // flatMap chains steps and merges their logs
 * BinaryOperator<List<String>> concat = (a, b) -> {
 *     var merged = new ArrayList<>(a);
 *     merged.addAll(b);
 *     return merged;
 * };
 *
 * Accumulator<List<String>, Integer> result = step1
 *     .flatMap(v -> Accumulator.of(v * 2, List.of("doubled")), concat)
 *     .flatMap(v -> Accumulator.of(v + 5, List.of("added 5")), concat);
 *
 * result.value();        // 25
 * result.accumulated();  // ["loaded base value", "doubled", "added 5"]
 * }</pre>
 *
 * <h2>Typical accumulation types</h2>
 * <ul>
 *   <li>{@code List<String>} — ordered log entries; merged with list concatenation.</li>
 *   <li>{@link NonEmptyList}{@code <String>} — same guarantee as for {@link Validated} errors;
 *       merged with {@link NonEmptyList#concat}.</li>
 *   <li>{@code int} / {@code long} — counters; merged with addition.</li>
 *   <li>Custom event types — domain audit entries; merged with
 *       {@code NonEmptyList} or {@code List} append.</li>
 * </ul>
 *
 * <h2>Accessing the value</h2>
 * <p>{@link #value()} returns {@code @Nullable A}. For accumulators created via
 * {@link #of(Object, Object)} or {@link #pure(Object, Object)}, the value is always
 * non-null. Accumulators created via {@link #tell(Object)} carry a {@code null} value
 * (the accumulation side-channel is the only meaningful payload). Call
 * {@link #hasValue()} before calling {@link #value()} if the source is not known.
 *
 * @param <E>         the accumulation type (log entries, metrics, etc.)
 * @param <A>         the value type
 * @param value       the computed value; {@code null} only for accumulators created by
 *                    {@link #tell(Object)}
 * @param accumulated the side-channel accumulation; never {@code null}
 */
@NullMarked
public record Accumulator<E, A>(@Nullable A value, E accumulated) {

    /**
     * Compact canonical constructor — validates that {@code accumulated} is non-null.
     *
     * @throws NullPointerException if {@code accumulated} is {@code null}
     */
    public Accumulator {
        Objects.requireNonNull(accumulated, "accumulated");
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    /**
     * Creates an {@code Accumulator} pairing a computed value with an initial accumulation.
     *
     * <p>Example:
     * <pre>{@code
     * Accumulator<List<String>, Integer> acc = Accumulator.of(42, List.of("computed answer"));
     * acc.value();        // 42
     * acc.accumulated();  // ["computed answer"]
     * }</pre>
     *
     * @param <E>         the accumulation type
     * @param <A>         the value type
     * @param value       the computed value; must not be {@code null}
     * @param accumulated the initial accumulation; must not be {@code null}
     * @return a new {@code Accumulator<E, A>}
     * @throws NullPointerException if either argument is {@code null}
     */
    public static <E, A> Accumulator<E, A> of(A value, E accumulated) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(accumulated, "accumulated");
        return new Accumulator<>(value, accumulated);
    }

    /**
     * Creates an {@code Accumulator} with a value and an empty (identity) accumulation.
     *
     * <p>Use this as the starting point of a chain when the first step has no entries
     * to record. The caller provides the {@code empty} value because Java does not have
     * type classes; common choices are {@code List.of()}, {@code 0}, or an empty string.
     *
     * <p>Example:
     * <pre>{@code
     * Accumulator<List<String>, Integer> start = Accumulator.pure(42, List.of());
     * // start has value 42 and an empty log
     * }</pre>
     *
     * @param <E>   the accumulation type
     * @param <A>   the value type
     * @param value the value; must not be {@code null}
     * @param empty the identity / empty accumulation; must not be {@code null}
     * @return a new {@code Accumulator<E, A>} with empty accumulation
     * @throws NullPointerException if either argument is {@code null}
     */
    public static <E, A> Accumulator<E, A> pure(A value, E empty) {
        return of(value, empty);
    }

    /**
     * Creates an {@code Accumulator} that records something without producing a meaningful value.
     *
     * <p>The resulting accumulator's {@link #value()} is {@code null} (type {@code Void}).
     * Use it at the start of a chain or inside a {@link #flatMap(Function, BinaryOperator)}
     * step that ignores the incoming value and contributes only to the accumulation:
     *
     * <pre>{@code
     * BinaryOperator<List<String>> concat = (a, b) -> {
     *     var merged = new ArrayList<>(a);
     *     merged.addAll(b);
     *     return merged;
     * };
     *
     * Accumulator<List<String>, Integer> result =
     *     Accumulator.tell(List.of("pre-check passed"))
     *         .flatMap(__ -> Accumulator.of(42, List.of("computed")), concat);
     *
     * result.value();        // 42
     * result.accumulated();  // ["pre-check passed", "computed"]
     * }</pre>
     *
     * <p><strong>Note:</strong> calling {@link #map(Function)} on a {@code tell} result throws
     * {@link NullPointerException} because {@code value()} is {@code null}. Chain with
     * {@link #flatMap(Function, BinaryOperator)} to produce a real value first.
     *
     * @param <E>         the accumulation type
     * @param accumulated the entry to record; must not be {@code null}
     * @return a new {@code Accumulator<E, Void>} with {@code null} value
     * @throws NullPointerException if {@code accumulated} is {@code null}
     */
    @SuppressWarnings("NullAway") // null is the only valid Void value; tell() is its sole producer
    public static <E> Accumulator<E, @Nullable Void> tell(E accumulated) {
        Objects.requireNonNull(accumulated, "accumulated");
        return new Accumulator<>(null, accumulated);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this accumulator was created with {@link #of(Object, Object)} or
     * {@link #pure(Object, Object)} and therefore carries a non-null value.
     *
     * <p>Returns {@code false} only for accumulators created by {@link #tell(Object)}.
     *
     * @return {@code true} when {@link #value()} is non-null
     */
    public boolean hasValue() {
        return value != null;
    }

    // -------------------------------------------------------------------------
    // Transformations
    // -------------------------------------------------------------------------

    /**
     * Transforms the value, leaving the accumulation unchanged.
     *
     * <p>Example:
     * <pre>{@code
     * Accumulator<List<String>, Integer> acc = Accumulator.of(42, List.of("step 1"));
     * Accumulator<List<String>, String>  str = acc.map(Object::toString);
     * // ("42", ["step 1"])
     * }</pre>
     *
     * <p><strong>Note:</strong> calling this method on a {@link #tell(Object)} result throws
     * {@link NullPointerException} because the value is {@code null}. Use
     * {@link #flatMap(Function, BinaryOperator)} to produce a real value from a
     * {@code tell} accumulator.
     *
     * @param <B> the type of the new value
     * @param f   the transformation function; must not be {@code null}
     * @return a new {@code Accumulator<E, B>} with the transformed value and unchanged accumulation
     * @throws NullPointerException if {@code f} is {@code null}, or if this accumulator was
     *                              created by {@link #tell(Object)} (its value is {@code null})
     */
    @SuppressWarnings("NullAway")
    public <B> Accumulator<E, B> map(Function<? super A, ? extends B> f) {
        Objects.requireNonNull(f, "f");
        Objects.requireNonNull(value, "cannot map over a tell() accumulator — use flatMap instead");
        return Accumulator.of(f.apply(value), accumulated);
    }

    /**
     * Chains a computation that itself produces an {@code Accumulator}, merging both
     * accumulations using {@code merge}.
     *
     * <p>This is the primary composition combinator: it is the functional equivalent of
     * "run step 1, append its log to the current log, then run step 2 with its result."
     *
     * <p>Example:
     * <pre>{@code
     * BinaryOperator<List<String>> concat = (a, b) -> {
     *     var merged = new ArrayList<>(a);
     *     merged.addAll(b);
     *     return merged;
     * };
     *
     * Accumulator<List<String>, Integer> result =
     *     Accumulator.of(10, List.of("step 1"))
     *         .flatMap(v -> Accumulator.of(v * 2, List.of("step 2")), concat)
     *         .flatMap(v -> Accumulator.of(v + 5, List.of("step 3")), concat);
     *
     * result.value();        // 25
     * result.accumulated();  // ["step 1", "step 2", "step 3"]
     * }</pre>
     *
     * @param <B>   the value type produced by the next step
     * @param f     function from the current value to the next {@code Accumulator};
     *              must not be {@code null} and must not return {@code null}
     * @param merge function that combines the current accumulation with the next step's
     *              accumulation; must not be {@code null}
     * @return a new {@code Accumulator<E, B>} with the chained value and merged accumulation
     * @throws NullPointerException if {@code f} or {@code merge} is {@code null}, or if
     *                              {@code f} returns {@code null}
     */
    @SuppressWarnings("NullAway")
    public <B> Accumulator<E, B> flatMap(
            Function<? super A, ? extends Accumulator<E, B>> f,
            BinaryOperator<E> merge) {
        Objects.requireNonNull(f, "f");
        Objects.requireNonNull(merge, "merge");
        Accumulator<E, B> next = f.apply(value);
        Objects.requireNonNull(next, "flatMap function must not return null");
        return new Accumulator<>(next.value(), merge.apply(accumulated, next.accumulated()));
    }

    /**
     * Transforms the accumulation, leaving the value unchanged.
     *
     * <p>Use this to convert between accumulation types — for example, to count log entries
     * or to transform raw strings into structured domain objects:
     *
     * <pre>{@code
     * Accumulator<List<String>, Integer> raw   = Accumulator.of(42, List.of("event 1"));
     * Accumulator<Integer, Integer>      count = raw.mapAccumulated(List::size);
     * // count.value()       == 42
     * // count.accumulated() == 1
     * }</pre>
     *
     * @param <F> the new accumulation type
     * @param f   the transformation function; must not be {@code null} and must not return
     *            {@code null}
     * @return a new {@code Accumulator<F, A>} with the transformed accumulation and unchanged value
     * @throws NullPointerException if {@code f} is {@code null} or returns {@code null}
     */
    public <F> Accumulator<F, A> mapAccumulated(Function<? super E, ? extends F> f) {
        Objects.requireNonNull(f, "f");
        F newAccumulated = f.apply(accumulated);
        Objects.requireNonNull(newAccumulated, "mapAccumulated function must not return null");
        return new Accumulator<>(value, newAccumulated);
    }

    // -------------------------------------------------------------------------
    // Combination
    // -------------------------------------------------------------------------

    /**
     * Combines this accumulator with {@code other} by merging their accumulations and applying
     * {@code f} to both values to produce a new value.
     *
     * <p>Both accumulators are evaluated independently — unlike {@link #flatMap}, there is no
     * sequential dependency between them. Use this when two parallel computations each produce
     * an accumulator and you want to combine their results and logs in one step:
     *
     * <pre>{@code
     * BinaryOperator<List<String>> concat = ...;
     *
     * Accumulator<List<String>, User>  userAcc  = fetchUser(userId);
     * Accumulator<List<String>, Order> orderAcc = fetchOrder(orderId);
     *
     * Accumulator<List<String>, Dashboard> dash =
     *     userAcc.combine(orderAcc, concat, Dashboard::new);
     *
     * // dash.accumulated() contains entries from both userAcc and orderAcc
     * }</pre>
     *
     * @param <B>   the value type of {@code other}
     * @param <C>   the combined value type
     * @param other the second accumulator; must not be {@code null} and must not have been
     *              created by {@link #tell(Object)} (its value must be non-null)
     * @param merge function that combines the two accumulations; must not be {@code null}
     * @param f     function that combines the two values; must not be {@code null}
     * @return a new {@code Accumulator<E, C>} with the combined value and merged accumulation
     * @throws NullPointerException if any argument is {@code null}, or if either accumulator
     *                              was created by {@link #tell(Object)}
     */
    @SuppressWarnings("NullAway")
    public <B, C> Accumulator<E, C> combine(
            Accumulator<E, B> other,
            BinaryOperator<E> merge,
            BiFunction<? super A, ? super B, ? extends C> f) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(merge, "merge");
        Objects.requireNonNull(f, "f");
        Objects.requireNonNull(value,
            "cannot combine a tell() accumulator — use flatMap to assign a value first");
        Objects.requireNonNull(other.value(),
            "cannot combine a tell() accumulator — use flatMap to assign a value first");
        return Accumulator.of(
            f.apply(value, other.value()),
            merge.apply(accumulated, other.accumulated())
        );
    }

    // -------------------------------------------------------------------------
    // Static combination
    // -------------------------------------------------------------------------

    /**
     * Folds a list of accumulators into a single {@code Accumulator<E, List<A>>}, merging all
     * accumulations left-to-right using {@code merge}, starting from {@code empty}.
     *
     * <p>Example:
     * <pre>{@code
     * BinaryOperator<List<String>> concat = ...;
     *
     * List<Accumulator<List<String>, Integer>> steps = List.of(
     *     Accumulator.of(1, List.of("step A")),
     *     Accumulator.of(2, List.of("step B")),
     *     Accumulator.of(3, List.of("step C"))
     * );
     *
     * Accumulator<List<String>, List<Integer>> result =
     *     Accumulator.sequence(steps, concat, List.of());
     *
     * result.value();        // [1, 2, 3]
     * result.accumulated();  // ["step A", "step B", "step C"]
     * }</pre>
     *
     * @param <E>          the accumulation type
     * @param <A>          the value type of each accumulator
     * @param accumulators the list of accumulators to fold; must not be {@code null} and no
     *                     element may be {@code null} or created by {@link #tell(Object)}
     * @param merge        function that combines two accumulations; must not be {@code null}
     * @param empty        the identity accumulation used as the starting value;
     *                     must not be {@code null}
     * @return a single {@code Accumulator<E, List<A>>} containing all values and the merged log
     * @throws NullPointerException if any argument is {@code null}, if any element of
     *                              {@code accumulators} is {@code null}, or if any accumulator
     *                              in the list was created by {@link #tell(Object)}
     */
    public static <E, A> Accumulator<E, List<A>> sequence(
            List<? extends Accumulator<E, A>> accumulators,
            BinaryOperator<E> merge,
            E empty) {
        Objects.requireNonNull(accumulators, "accumulators");
        Objects.requireNonNull(merge, "merge");
        Objects.requireNonNull(empty, "empty");
        E acc = empty;
        List<A> values = new ArrayList<>(accumulators.size());
        for (int i = 0; i < accumulators.size(); i++) {
            Accumulator<E, A> a = accumulators.get(i);
            Objects.requireNonNull(a, "accumulators[" + i + "] must not be null");
            A v = a.value();
            Objects.requireNonNull(v,
                "accumulators[" + i + "] was created by tell() and has no value");
            values.add(v);
            acc = merge.apply(acc, a.accumulated());
        }
        return Accumulator.of(List.copyOf(values), acc);
    }

    /**
     * Lifts an {@link Option} value into an {@code Accumulator}, recording a log entry for
     * both the present and absent cases.
     *
     * <p>Use this to thread option-returning operations through a traced computation chain
     * while keeping a record of whether each lookup succeeded:
     *
     * <pre>{@code
     * Accumulator<List<String>, Option<User>> acc = Accumulator.liftOption(
     *     userRepo.findById(id),
     *     user -> List.of("user found: " + user.name()),
     *     List.of("user not found for id: " + id)
     * );
     * // if found:     (Some(user), ["user found: Alice"])
     * // if not found: (None,       ["user not found for id: 42"])
     * }</pre>
     *
     * @param <E>      the accumulation type
     * @param <A>      the value type inside the option
     * @param option   the option to lift; must not be {@code null}
     * @param someLog  function that produces the accumulation entry when the option is present;
     *                 must not be {@code null} and must not return {@code null}
     * @param noneLog  the accumulation entry recorded when the option is absent;
     *                 must not be {@code null}
     * @return an {@code Accumulator<E, Option<A>>} pairing the option value with its log entry
     * @throws NullPointerException if any argument is {@code null} or if {@code someLog}
     *                              returns {@code null}
     */
    public static <E, A> Accumulator<E, Option<A>> liftOption(
            Option<A> option,
            Function<? super A, ? extends E> someLog,
            E noneLog) {
        Objects.requireNonNull(option, "option");
        Objects.requireNonNull(someLog, "someLog");
        Objects.requireNonNull(noneLog, "noneLog");
        if (option.isDefined()) {
            A v = option.get();
            E log = someLog.apply(v);
            Objects.requireNonNull(log, "someLog function must not return null");
            return Accumulator.of(Option.some(v), log);
        }
        return Accumulator.of(Option.none(), noneLog);
    }

    /**
     * Lifts a {@link Try} value into an {@code Accumulator}, recording a log entry via
     * {@code successLog} on success or {@code failureLog} on failure.
     *
     * <p>The {@code Try<A>} itself becomes the value of the returned accumulator, preserving
     * full access to the outcome. The log records what happened regardless of which branch was
     * taken:
     *
     * <pre>{@code
     * Accumulator<List<String>, Try<Config>> acc = Accumulator.liftTry(
     *     Try.of(() -> ConfigLoader.load(path)),
     *     cfg  -> List.of("config loaded from " + path),
     *     ex   -> List.of("config load failed: " + ex.getMessage())
     * );
     *
     * acc.accumulated();  // always set — success or failure
     * acc.value();        // Try<Config> — caller decides how to handle it
     * }</pre>
     *
     * @param <E>        the accumulation type
     * @param <A>        the success value type of the Try
     * @param result     the Try to lift; must not be {@code null}
     * @param successLog function that produces the log entry on success; must not be {@code null}
     *                   and must not return {@code null}
     * @param failureLog function that produces the log entry on failure; must not be {@code null}
     *                   and must not return {@code null}
     * @return an {@code Accumulator<E, Try<A>>} pairing the Try result with its log entry
     * @throws NullPointerException if any argument is {@code null} or if either log function
     *                              returns {@code null}
     */
    public static <E, A> Accumulator<E, Try<A>> liftTry(
            Try<A> result,
            Function<? super A, ? extends E> successLog,
            Function<? super Throwable, ? extends E> failureLog) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(successLog, "successLog");
        Objects.requireNonNull(failureLog, "failureLog");
        if (result.isSuccess()) {
            E log = successLog.apply(result.get());
            Objects.requireNonNull(log, "successLog function must not return null");
            return Accumulator.of(result, log);
        }
        E log = failureLog.apply(result.getCause());
        Objects.requireNonNull(log, "failureLog function must not return null");
        return Accumulator.of(result, log);
    }

    // -------------------------------------------------------------------------
    // Interoperability
    // -------------------------------------------------------------------------

    /**
     * Returns the value wrapped in {@link Option#some(Object)}, or {@link Option#none()} if
     * this accumulator was created by {@link #tell(Object)} and has no value.
     *
     * <p>The accumulation is discarded. Use this when only the presence or absence of a value
     * matters, not the log:
     *
     * <pre>{@code
     * Accumulator<List<String>, Integer> acc = Accumulator.of(42, List.of("step 1"));
     * acc.toOption();  // Some(42)
     *
     * Accumulator<List<String>, Void> tell = Accumulator.tell(List.of("entry"));
     * tell.toOption(); // None
     * }</pre>
     *
     * @return {@code Some(value)} when {@link #hasValue()} is {@code true}, or {@code None}
     *         for {@link #tell(Object)} results
     */
    public Option<A> toOption() {
        return value != null ? Option.some(value) : Option.none();
    }

    /**
     * Converts this accumulator to a {@link Tuple2} of {@code (accumulated, value)}.
     *
     * <p>The accumulation occupies the first position ({@code _1}) and the value occupies
     * the second ({@code _2}), following the Writer-monad unpacking convention.
     *
     * <pre>{@code
     * Accumulator<List<String>, Integer> acc = Accumulator.of(42, List.of("step 1"));
     * Tuple2<List<String>, Integer> pair = acc.toTuple2();
     * pair._1();  // ["step 1"]
     * pair._2();  // 42
     * }</pre>
     *
     * @return a {@code Tuple2<E, A>} with accumulated as {@code _1} and value as {@code _2}
     */
    public Tuple2<E, @Nullable A> toTuple2() {
        return new Tuple2<>(accumulated, value);
    }
}
