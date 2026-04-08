package dmx.fun;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A sealed interface representing a validated value that can either be a successful value
 * ({@link Validated.Valid}) or an error ({@link Validated.Invalid}).
 *
 * <p>Unlike {@link Result}, which stops at the first error (fail-fast), {@code Validated} supports
 * applicative-style <em>error accumulation</em> via {@link #combine} and {@link #product}.
 * This makes it ideal for form/DTO validation where all errors should be reported at once.
 * Sequential fail-fast composition is still available through {@link #flatMap}.
 *
 * <p>This interface is {@link NullMarked}: all types are non-null by default.
 *
 * @param <E> the type of the error contained in an invalid result
 * @param <A> the type of the value contained in a valid result
 */
@NullMarked
public sealed interface Validated<E, A> extends Bicontainer<A, E> permits Validated.Valid, Validated.Invalid {

    /**
     * Represents a valid result containing a value of type {@code A}.
     *
     * @param <E>   the type of the error (unused here)
     * @param <A>   the type of the value
     * @param value the non-null value; passing {@code null} throws {@link NullPointerException}
     */
    record Valid<E, A>(A value) implements Validated<E, A> {
        /** Compact canonical constructor — validates that {@code value} is non-null. */
        public Valid {
            Objects.requireNonNull(value, "Valid value must not be null");
        }
    }

    /**
     * Represents an invalid result containing an error of type {@code E}.
     *
     * @param <E>   the type of the error
     * @param <A>   the type of the value (unused here)
     * @param error the non-null error; passing {@code null} throws {@link NullPointerException}
     */
    record Invalid<E, A>(E error) implements Validated<E, A> {
        /** Compact canonical constructor — validates that {@code error} is non-null. */
        public Invalid {
            Objects.requireNonNull(error, "Invalid error must not be null");
        }
    }

    // ---------- Factories ----------

    /**
     * Creates a {@link Valid} instance wrapping the given value.
     *
     * @param <E>   the error type
     * @param <A>   the value type
     * @param value the non-null value
     * @return a {@code Validated} in the valid state
     * @throws NullPointerException if {@code value} is {@code null}
     */
    static <E, A> Validated<E, A> valid(A value) {
        return new Valid<>(value);
    }

    /**
     * Creates an {@link Invalid} instance wrapping the given error.
     *
     * @param <E>   the error type
     * @param <A>   the value type
     * @param error the non-null error
     * @return a {@code Validated} in the invalid state
     * @throws NullPointerException if {@code error} is {@code null}
     */
    static <E, A> Validated<E, A> invalid(E error) {
        return new Invalid<>(error);
    }

    // ---------- Predicates ----------

    /** Returns {@code true} if this instance is the success ({@link Valid}) variant. */
    @Override
    default boolean isSuccess() {
        return this instanceof Valid<?, ?>;
    }

    /**
     * Returns {@code true} if this instance is {@link Valid}.
     *
     * @return {@code true} for {@code Valid}, {@code false} for {@code Invalid}
     */
    default boolean isValid() {
        return isSuccess();
    }

    /**
     * Returns {@code true} if this instance is {@link Invalid}.
     *
     * @return {@code true} for {@code Invalid}, {@code false} for {@code Valid}
     */
    default boolean isInvalid() {
        return !isSuccess();
    }

    // ---------- Accessors ----------

    /**
     * Retrieves the value if this is {@link Valid}.
     *
     * @return the contained value
     * @throws NoSuchElementException if this is {@link Invalid}
     */
    default A get() {
        return switch (this) {
            case Valid<E, A> v -> v.value();
            case Invalid<E, A> _ -> throw new NoSuchElementException("No value present. This Validated is Invalid.");
        };
    }

    /**
     * Retrieves the error if this is {@link Invalid}.
     *
     * @return the contained error
     * @throws NoSuchElementException if this is {@link Valid}
     */
    default E getError() {
        return switch (this) {
            case Invalid<E, A> inv -> inv.error();
            case Valid<E, A> _ -> throw new NoSuchElementException("No error present. This Validated is Valid.");
        };
    }

    // ---------- Transformations ----------

    /**
     * Transforms the value with {@code mapper} if {@link Valid}; leaves {@link Invalid} unchanged.
     *
     * @param <B>    the new value type
     * @param mapper the mapping function
     * @return a new {@code Validated} with the mapped value, or the original error
     */
    default <B> Validated<E, B> map(Function<A, B> mapper) {
        return switch (this) {
            case Valid<E, A> v -> Validated.valid(Objects.requireNonNull(mapper.apply(v.value()), "map mapper returned null"));
            case Invalid<E, A> inv -> Validated.invalid(inv.error());
        };
    }

    /**
     * Transforms the error with {@code mapper} if {@link Invalid}; leaves {@link Valid} unchanged.
     *
     * @param <F>    the new error type
     * @param mapper the error mapping function
     * @return a new {@code Validated} with the mapped error, or the original value
     */
    default <F> Validated<F, A> mapError(Function<E, F> mapper) {
        return switch (this) {
            case Valid<E, A> v -> Validated.valid(v.value());
            case Invalid<E, A> inv -> Validated.invalid(Objects.requireNonNull(mapper.apply(inv.error()), "mapError mapper returned null"));
        };
    }

    /**
     * Fail-fast sequential composition: applies {@code mapper} to the value if {@link Valid}.
     * If this is {@link Invalid}, the error is propagated without calling {@code mapper}.
     *
     * @param <B>    the new value type
     * @param mapper a function that maps the value to a new {@code Validated}
     * @return the result of {@code mapper} if valid, otherwise this invalid
     */
    default <B> Validated<E, B> flatMap(Function<A, Validated<E, B>> mapper) {
        return switch (this) {
            case Valid<E, A> v -> Objects.requireNonNull(mapper.apply(v.value()), "flatMap mapper must not return null");
            case Invalid<E, A> inv -> Validated.invalid(inv.error());
        };
    }

    // ---------- Error-accumulating combination ----------

    /**
     * Combines this {@code Validated} with {@code other}, accumulating errors when both are invalid.
     *
     * <table>
     *   <caption>Combination semantics</caption>
     *   <tr><th>this</th><th>other</th><th>result</th></tr>
     *   <tr><td>Valid</td><td>Valid</td><td>Valid(Tuple2(a, b))</td></tr>
     *   <tr><td>Valid</td><td>Invalid</td><td>Invalid(other.error)</td></tr>
     *   <tr><td>Invalid</td><td>Valid</td><td>Invalid(this.error)</td></tr>
     *   <tr><td>Invalid</td><td>Invalid</td><td>Invalid(errMerge(this.error, other.error))</td></tr>
     * </table>
     *
     * @param <B>      the value type of the other {@code Validated}
     * @param other    the other {@code Validated} to combine with
     * @param errMerge a function to merge two errors when both are invalid
     * @return a {@code Validated} containing a {@link Tuple2} of values, or an accumulated error
     */
    default <B> Validated<E, Tuple2<A, B>> product(Validated<E, B> other, BinaryOperator<E> errMerge) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(errMerge, "errMerge");
        record Pair<X, Y>(X left, Y right) {}
        return switch (new Pair<>(this, other)) {
            case Pair(Valid<E, A> v,     Valid<E, B>   ob) -> Validated.valid(new Tuple2<>(v.value(), ob.value()));
            case Pair(Valid<E, A> _,     Invalid<E, B> oi) -> Validated.invalid(oi.error());
            case Pair(Invalid<E, A> inv, Valid<E, B>   _ ) -> Validated.invalid(inv.error());
            case Pair(Invalid<E, A> inv, Invalid<E, B> oi) -> Validated.invalid(
                Objects.requireNonNull(errMerge.apply(inv.error(), oi.error()), "errMerge returned null")
            );
        };
    }

    /**
     * Combines this {@code Validated} with {@code other}, accumulating errors and merging values.
     * Implemented as {@code product(other, errMerge).map(t -> valueMerge.apply(t._1(), t._2()))}.
     *
     * @param <B>        the value type of the other {@code Validated}
     * @param <C>        the result value type
     * @param other      the other {@code Validated} to combine with
     * @param errMerge   a function to merge two errors when both are invalid
     * @param valueMerge a function to merge two values when both are valid
     * @return a {@code Validated} with the merged value, or an accumulated error
     */
    default <B, C> Validated<E, C> combine(
        Validated<E, B> other,
        BinaryOperator<E> errMerge,
        BiFunction<A, B, C> valueMerge
    ) {
        Objects.requireNonNull(valueMerge, "valueMerge");
        return product(other, errMerge).map(t -> valueMerge.apply(t._1(), t._2()));
    }

    // ---------- Side effects ----------

    /**
     * Executes {@code action} with the value if {@link Valid}; returns {@code this} unchanged.
     *
     * @param action a consumer of the value
     * @return this instance
     */
    default Validated<E, A> peek(Consumer<A> action) {
        Objects.requireNonNull(action, "action");
        if (this instanceof Valid<E, A>(A value)) {
            action.accept(value);
        }
        return this;
    }

    /**
     * Executes {@code action} with the error if {@link Invalid}; returns {@code this} unchanged.
     *
     * @param action a consumer of the error
     * @return this instance
     */
    default Validated<E, A> peekError(Consumer<E> action) {
        Objects.requireNonNull(action, "action");
        if (this instanceof Invalid<E, A>(E error)) {
            action.accept(error);
        }
        return this;
    }

    // ---------- Interop ----------

    /**
     * Converts this {@code Validated} to a {@link Result}.
     * {@link Valid} maps to {@link Result.Ok}; {@link Invalid} maps to {@link Result.Err}.
     *
     * @return a {@code Result} equivalent of this {@code Validated}
     */
    default Result<A, E> toResult() {
        return switch (this) {
            case Valid<E, A> v -> Result.ok(v.value());
            case Invalid<E, A> inv -> Result.err(inv.error());
        };
    }

    /**
     * Converts this {@code Validated} to a {@link Try}.
     * {@link Valid} maps to {@link Try#success}; {@link Invalid} maps to {@link Try#failure}
     * using the provided error mapper.
     *
     * @param errorMapper maps the error to a {@link Throwable}
     * @return a {@code Try} equivalent of this {@code Validated}
     * @throws NullPointerException if {@code errorMapper} is null or returns null
     */
    default Try<A> toTry(Function<E, ? extends Throwable> errorMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper");
        return switch (this) {
            case Valid<E, A> v -> Try.success(v.value());
            case Invalid<E, A> inv -> Try.failure(
                Objects.requireNonNull(errorMapper.apply(inv.error()), "errorMapper returned null")
            );
        };
    }

    /**
     * Converts a {@link Result} to a {@code Validated}.
     * {@link Result.Ok} maps to {@link Valid}; {@link Result.Err} maps to {@link Invalid}.
     *
     * @param <E>    the error type
     * @param <A>    the value type
     * @param result the result to convert; must not be null
     * @return the equivalent {@code Validated}
     * @throws NullPointerException if {@code result} is null
     */
    static <E, A> Validated<E, A> fromResult(Result<A, E> result) {
        Objects.requireNonNull(result, "result");
        return switch (result) {
            case Result.Ok<A, E> ok -> Validated.valid(ok.value());
            case Result.Err<A, E> err -> Validated.invalid(err.error());
        };
    }

    /**
     * Converts an {@link Option} to a {@code Validated}.
     * {@link Option.Some} maps to {@link Valid}; {@link Option#none()} maps to {@link Invalid}
     * using the provided error.
     *
     * @param <E>         the error type
     * @param <A>         the value type
     * @param option      the option to convert; must not be null
     * @param errorIfNone the error to use if the option is empty
     * @return the equivalent {@code Validated}
     * @throws NullPointerException if {@code option} is null
     */
    static <E, A> Validated<E, A> fromOption(Option<? extends A> option, E errorIfNone) {
        Objects.requireNonNull(option, "option");
        Objects.requireNonNull(errorIfNone, "errorIfNone");
        return switch (option) {
            case Option.Some<? extends A> s -> Validated.valid(s.value());
            case Option.None<? extends A> _ -> Validated.invalid(errorIfNone);
        };
    }

    /**
     * Converts a {@link Try} to a {@code Validated}.
     * {@link Try#success} maps to {@link Valid}; {@link Try#failure} maps to {@link Invalid}
     * using the provided error mapper.
     *
     * @param <E>         the error type
     * @param <A>         the value type
     * @param t           the try to convert; must not be null
     * @param errorMapper maps the throwable to an error; must not return null
     * @return the equivalent {@code Validated}
     * @throws NullPointerException if {@code t} or {@code errorMapper} is null
     */
    static <E, A> Validated<E, A> fromTry(Try<A> t, Function<? super Throwable, E> errorMapper) {
        Objects.requireNonNull(t, "t");
        Objects.requireNonNull(errorMapper, "errorMapper");
        return switch (t) {
            case Try.Success<A> s -> Validated.valid(s.value());
            case Try.Failure<A> f -> Validated.invalid(
                Objects.requireNonNull(errorMapper.apply(f.cause()), "errorMapper returned null")
            );
        };
    }

    // ---------- sequence / traverse ----------

    /**
     * Transforms an iterable of {@code Validated<E, A>} into a single {@code Validated<E, List<A>>}.
     * All errors are accumulated using {@code errMerge}; all valid values are collected in order.
     * If any element is {@link Invalid}, no values are collected and the accumulated error is returned.
     *
     * @param <E>       the error type
     * @param <A>       the value type
     * @param validated the iterable of validated values; must not be null or contain null elements
     * @param errMerge  a function to merge two errors; used when multiple elements are invalid
     * @return {@code Valid(List<A>)} if all elements are valid, or {@code Invalid(accumulatedError)}
     * @throws NullPointerException if {@code validated} or {@code errMerge} is null,
     *                              or if {@code validated} contains a null element
     */
    static <E, A> Validated<E, List<A>> sequence(
        Iterable<Validated<E, A>> validated,
        BinaryOperator<E> errMerge
    ) {
        Objects.requireNonNull(validated, "validated");
        Objects.requireNonNull(errMerge, "errMerge");
        return accumulate(validated, errMerge);
    }

    /**
     * Transforms a stream of {@code Validated<E, A>} into a single {@code Validated<E, List<A>>}.
     * All errors are accumulated using {@code errMerge}; all valid values are collected in order.
     *
     * @param <E>       the error type
     * @param <A>       the value type
     * @param validated the stream of validated values; must not be null or contain null elements
     * @param errMerge  a function to merge two errors
     * @return {@code Valid(List<A>)} if all elements are valid, or {@code Invalid(accumulatedError)}
     * @throws NullPointerException if {@code validated} or {@code errMerge} is null,
     *                              or if the stream contains a null element
     */
    static <E, A> Validated<E, List<A>> sequence(
        Stream<Validated<E, A>> validated,
        BinaryOperator<E> errMerge
    ) {
        Objects.requireNonNull(validated, "validated");
        Objects.requireNonNull(errMerge, "errMerge");
        try (validated) {
            return accumulate((Iterable<Validated<E, A>>) validated::iterator, errMerge);
        }
    }

    /**
     * Maps each element of an iterable through {@code mapper} and collects the results into a
     * {@code Validated<E, List<B>>}, accumulating all errors.
     *
     * @param <E>      the error type
     * @param <A>      the input element type
     * @param <B>      the mapped value type
     * @param values   the iterable of input values; must not be null
     * @param mapper   a function that maps each value to a {@code Validated}; must not return null
     * @param errMerge a function to merge two errors
     * @return {@code Valid(List<B>)} if all mappings succeed, or {@code Invalid(accumulatedError)}
     * @throws NullPointerException if any argument is null or if the mapper returns null
     */
    static <E, A, B> Validated<E, List<B>> traverse(
        Iterable<A> values,
        Function<? super A, Validated<E, B>> mapper,
        BinaryOperator<E> errMerge
    ) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(errMerge, "errMerge");
        Iterable<Validated<E, B>> mapped = () ->
            StreamSupport.stream(values.spliterator(), false)
                .map(a -> Objects.requireNonNull(mapper.apply(a), "traverse mapper must not return null"))
                .iterator();
        return accumulate(mapped, errMerge);
    }

    // ---------- collector / traverseCollector ----------

    /**
     * Returns a {@link Collector} that accumulates a stream of {@code Validated<E, A>} into a
     * single {@code Validated<E, List<A>>}, collecting all valid values and merging all errors
     * left-to-right using {@code errMerge}.
     *
     * <p>Compatible with parallel streams: partial results from each split are combined via
     * {@code errMerge} on errors and list concatenation on values.
     *
     * @param <E>      the error type
     * @param <A>      the value type
     * @param errMerge a function to merge two errors; must not return {@code null}
     * @return a {@code Collector} producing {@code Valid(List<A>)} if all elements are valid,
     *         or {@code Invalid(accumulatedError)} otherwise
     * @throws NullPointerException if {@code errMerge} is null
     */
    static <E, A> Collector<Validated<E, A>, ?, Validated<E, List<A>>> collector(
        BinaryOperator<E> errMerge
    ) {
        Objects.requireNonNull(errMerge, "errMerge");
        return Collector.of(
            () -> new ArrayList<Validated<E, A>>(),
            ArrayList::add,
            (left, right) -> { left.addAll(right); return left; },
            list -> accumulate(list, errMerge)
        );
    }

    /**
     * Returns a {@link Collector} that maps each input element through {@code mapper} and
     * accumulates the results into a single {@code Validated<E, List<B>>}, collecting all valid
     * values and merging all errors left-to-right using {@code errMerge}.
     *
     * <p>The mapper is applied eagerly during accumulation, so parallel streams benefit from
     * parallel mapping. Compatible with parallel streams.
     *
     * @param <E>      the error type
     * @param <A>      the input element type
     * @param <B>      the mapped value type
     * @param mapper   a function mapping each element to a {@code Validated}; must not return {@code null}
     * @param errMerge a function to merge two errors; must not return {@code null}
     * @return a {@code Collector} producing {@code Valid(List<B>)} if all mappings succeed,
     *         or {@code Invalid(accumulatedError)} otherwise
     * @throws NullPointerException if {@code mapper} or {@code errMerge} is null
     */
    static <E, A, B> Collector<A, ?, Validated<E, List<B>>> traverseCollector(
        Function<? super A, Validated<E, B>> mapper,
        BinaryOperator<E> errMerge
    ) {
        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(errMerge, "errMerge");
        return Collector.of(
            () -> new ArrayList<Validated<E, B>>(),
            (list, a) -> list.add(Objects.requireNonNull(mapper.apply(a), "traverseCollector mapper must not return null")),
            (left, right) -> { left.addAll(right); return left; },
            list -> accumulate(list, errMerge)
        );
    }

    private static <E, A> Validated<E, List<A>> accumulate(
        Iterable<Validated<E, A>> items,
        BinaryOperator<E> errMerge
    ) {
        ArrayList<A> values = new ArrayList<>();
        @Nullable E acc = null;
        for (Validated<E, A> v : items) {
            Objects.requireNonNull(v, "validated contains null element");
            switch (v) {
                case Valid<E, A> ok -> { if (acc == null) values.add(ok.value()); }
                case Invalid<E, A> err -> acc = Objects.requireNonNull(
                    (acc == null) ? err.error() : errMerge.apply(acc, err.error()),
                    "errMerge returned null");
            }
        }
        return acc != null ? Validated.invalid(acc) : Validated.valid(List.copyOf(values));
    }
}
