package dmx.fun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.NullMarked;

/**
 * A sealed interface representing an optional value with two possible states:
 * either a value is present ("Some") or absent ("None").
 * <p>
 * This interface provides a flexible alternative to {@link Optional},
 * integrating functional-style operations for working with optional values in a more
 * expressive and composable manner.
 *
 * <p>This interface is {@link NullMarked}: all types are non-null by default.
 *
 * @param <Value> The type of the optional value.
 */
@NullMarked
public sealed interface Option<Value> permits Option.Some, Option.None {

    /**
     * Represents a container object that holds a non-null value.
     * <p>
     * The {@code Some} class is a concrete implementation of the {@code Option}
     * interface, used to encapsulate a value that is guaranteed to be non-null.
     * Attempting to create an instance of {@code Some} with a {@code null} value
     * will throw a {@code NullPointerException}.
     *
     * @param value   the non-null value encapsulated by this instance
     * @param <Value> the type of the non-null value encapsulated by this instance
     */
    record Some<Value>(Value value) implements Option<Value> {
        /**
         * Constructs a new {@code Some} instance with the specified non-null value.
         *
         * @param value the non-null value to be encapsulated by this {@code Some} instance
         * @throws NullPointerException if the provided value is {@code null}
         */
        public Some {
            Objects.requireNonNull(value, "Option.Some cannot hold null. Use Option.none() or Option.ofNullable().");
        }
    }

    /**
     * Represents a variant of the {@link Option} type that signifies the absence of a value.
     * <p>
     * This record is used to indicate and handle cases where an operation does not produce
     * a meaningful or valid result. It serves as an alternative to using {@code null}.
     *
     * @param <Value> The type of the value that would be held by an {@link Option}, if present.
     */
    record None<Value>() implements Option<Value> {
        @SuppressWarnings("rawtypes")
        static final None INSTANCE = new None<>();
    }

    // ---------- Factories ----------

    /**
     * Creates a {@link Some} instance that encapsulates the given non-null value.
     * Use {@link #ofNullable(Object)} if the value may be {@code null}.
     *
     * @param <V>   the type of the value to encapsulate
     * @param value the non-null value to encapsulate
     * @return a {@code Some<V>} wrapping the provided value
     * @throws NullPointerException if {@code value} is {@code null} (enforced by {@link Some#Some(Object)})
     */
    static <V> Option<V> some(V value) {
        return new Some<>(value);
    }

    /**
     * Creates an instance of {@link Option} that represents the absence of a value.
     *
     * @param <V> the type of the value that would be held by the {@link Option}, if present
     * @return an {@code Option} instance that signifies no value is present
     */
    @SuppressWarnings("unchecked")
    static <V> Option<V> none() {
        return (Option<V>) None.INSTANCE;
    }

    /**
     * Creates an Option instance that encapsulates a given value. If the value is null, it returns a None instance.
     *
     * @param <V>   the type of the value to encapsulate
     * @param value the value to be encapsulated; if null, a None instance is returned
     * @return an Option containing the provided value if it is non-null, or a None instance if the value is null
     */
    static <V> Option<V> ofNullable(V value) {
        return value == null ? none() : new Some<>(value);
    }

    /**
     * Converts a given {@link Optional} instance into an {@link Option} instance.
     *
     * @param <V>      the type of the value that may be present in the {@link Optional}
     * @param optional the {@link Optional} to be converted; if the {@link Optional} contains a value,
     *                 a {@code Some} instance is returned, otherwise a {@code None} instance is returned
     * @return an {@link Option} containing the value from the {@link Optional} if it is present,
     * or an empty {@code None} instance if the {@link Optional} is empty
     */
    static <V> Option<V> fromOptional(Optional<V> optional) {
        return optional.map(Option::some).orElseGet(Option::none);
    }

    // ---------- Predicates ----------

    /**
     * Determines whether this instance represents a defined value.
     *
     * @return {@code true} if this instance is of type {@code Some<?>} and holds a value;
     * {@code false} if it is of type {@code None} and does not hold a value.
     */
    default boolean isDefined() {
        return this instanceof Some<?>;
    }

    /**
     * Checks if this {@link Option} instance represents the absence of a value.
     *
     * @return {@code true} if this instance is of type {@code None<?>}, indicating no value is present;
     * {@code false} if this instance holds a value.
     */
    default boolean isEmpty() {
        return this instanceof None<?>;
    }

    // ---------- Accessors ----------

    /**
     * Retrieves the value held by this {@link Option} instance if it is of type {@code Some}.
     * If the instance is of type {@code None}, a {@link NoSuchElementException} is thrown.
     *
     * @return the encapsulated value if this is an instance of {@code Some}
     * @throws NoSuchElementException if this is an instance of {@code None}, indicating no value is present
     */
    default Value get() {
        return switch (this) {
            case Some<Value> s -> s.value();
            case None<Value> _ -> throw new NoSuchElementException("No value present. Option is None.");
        };
    }

    /**
     * Retrieves the encapsulated value if this {@link Option} instance is of type {@code Some},
     * or returns the provided fallback value if this instance is of type {@code None}.
     *
     * @param fallback the value to return if this instance is {@code None}
     * @return the encapsulated value if this instance is a {@code Some}, or the specified fallback value if this instance is {@code None}
     */
    default Value getOrElse(Value fallback) {
        return this instanceof Some<Value>(Value v) ? v : fallback;
    }

    /**
     * Retrieves the encapsulated value if this {@code Option} instance is of type {@code Some},
     * or computes and returns a fallback value supplied by the given {@code fallbackSupplier}
     * if this instance is of type {@code None}.
     *
     * @param fallbackSupplier a {@code Supplier} that provides a fallback value
     *                         if this instance represents the absence of a value
     * @return the encapsulated value if this instance is a {@code Some},
     * or the fallback value computed by the {@code fallbackSupplier} if this instance is a {@code None}
     */
    default Value getOrElseGet(Supplier<? extends Value> fallbackSupplier) {
        return this instanceof Some<Value>(Value v) ? v : fallbackSupplier.get();
    }

    /**
     * Returns this {@code Option} if it is {@code Some}, otherwise returns {@code alternative}.
     *
     * @param alternative the fallback {@code Option} to return when this is {@code None};
     *                    must not be {@code null}
     * @return this instance if {@code Some}, otherwise {@code alternative}
     * @throws NullPointerException if {@code alternative} is null
     */
    @SuppressWarnings("unchecked")
    default Option<Value> orElse(Option<? extends Value> alternative) {
        Objects.requireNonNull(alternative, "alternative");
        return isDefined() ? this : (Option<Value>) alternative;
    }

    /**
     * Returns this {@code Option} if it is {@code Some}, otherwise evaluates and returns
     * the supplier's result. The supplier is <em>not</em> called when this is {@code Some}.
     *
     * @param alternative a lazy supplier of a fallback {@code Option}; must not return {@code null}
     * @return this instance if {@code Some}, or the result of {@code alternative.get()} if {@code None}
     * @throws NullPointerException if {@code alternative} is null or returns {@code null}
     */
    @SuppressWarnings("unchecked")
    default Option<Value> orElse(Supplier<? extends Option<? extends Value>> alternative) {
        Objects.requireNonNull(alternative, "alternative");
        if (isDefined()) return this;
        return (Option<Value>) Objects.requireNonNull(alternative.get(), "alternative returned null");
    }

    /**
     * Retrieves the encapsulated value if this {@code Option} instance is of type {@code Some},
     * or returns {@code null} if this instance is of type {@code None}.
     *
     * @return the encapsulated value if this instance is a {@code Some}, or {@code null} if this instance is a {@code None}.
     */
    default Value getOrNull() {
        return this instanceof Some<Value>(Value v) ? v : null;
    }

    /**
     * Retrieves the encapsulated value if this {@code Option} instance is of type {@code Some}.
     * If the instance is of type {@code None}, it throws an exception provided by the given {@code exceptionSupplier}.
     *
     * @param exceptionSupplier a {@code Supplier} that provides the exception to be thrown
     *                          if this instance is of type {@code None}
     * @return the encapsulated value if this instance is of type {@code Some}
     * @throws RuntimeException if this instance is of type {@code None},
     *                          using the exception provided by {@code exceptionSupplier}
     */
    default Value getOrThrow(Supplier<? extends RuntimeException> exceptionSupplier) {
        return switch (this) {
            case Some<Value> s -> s.value();
            case None<Value> _ -> throw exceptionSupplier.get();
        };
    }

    // ---------- Monadic operations ----------

    /**
     * Transforms the current Option using the provided mapping function.
     * If the current Option is a Some, applies the provided mapper to its value.
     * If the current Option is a None, returns None without applying the mapper.
     *
     * @param <NewValue> the type of the value in the resulting Option after transformation
     * @param mapper     the function to apply to the value if this Option is a Some
     * @return a new Option containing the mapped value if this is a Some, or None if this is a None
     */
    default <NewValue> Option<NewValue> map(Function<? super Value, ? extends NewValue> mapper) {
        return switch (this) {
            case Some<Value> s -> Option.ofNullable(mapper.apply(s.value()));
            case None<Value> _ -> Option.none();
        };
    }

    /**
     * Transforms the current Option value using the provided mapping function and flattens the result.
     * If the Option is empty (None), it remains empty. Otherwise, it applies the mapping function
     * to the encapsulated value and returns the resulting Option.
     *
     * @param <NewValue> the type of the element contained in the resulting Option
     * @param mapper     the function to apply to the encapsulated value, which produces a new Option
     * @return a new Option resulting from applying the mapping function and flattening
     * @throws NullPointerException if the mapping function returns null
     */
    default <NewValue> Option<NewValue> flatMap(Function<? super Value, Option<NewValue>> mapper) {
        return switch (this) {
            case Some<Value> s -> Objects.requireNonNull(mapper.apply(s.value()),
                "flatMap mapper must not return null");
            case None<Value> _ -> Option.none();
        };
    }

    /**
     * Filters the value of this Option based on the provided predicate.
     * If this Option is a Some and the predicate evaluates to true, the Option is returned as-is.
     * If the predicate evaluates to false, or this Option is a None, an empty Option is returned.
     *
     * @param predicate the predicate used to test the value inside this Option
     * @return an Option containing the value if the predicate evaluates to true, otherwise an empty Option
     */
    default Option<Value> filter(Predicate<? super Value> predicate) {
        return switch (this) {
            case Some<Value> s -> predicate.test(s.value()) ? this : Option.none();
            case None<Value> _ -> this;
        };
    }

    /**
     * Applies the provided action to the value contained in this instance if it is of type Some.
     *
     * @param action a {@code Consumer} that performs an operation on the contained value
     * @return this instance after applying the provided action
     */
    default Option<Value> peek(Consumer<? super Value> action) {
        if (this instanceof Some<Value>(Value v)) {
            action.accept(v);
        }
        return this;
    }

    /**
     * Folds the current option into a single value by applying the appropriate function
     * depending on whether the option is a {@code Some} or a {@code None}.
     *
     * @param <Folded> The type of the resulting value after folding.
     * @param onNone   A supplier to provide a value in case the option is {@code None}.
     * @param onSome   A function to transform the value in case the option is {@code Some}.
     * @return The folded value of type {@code Folded} resulting from applying the appropriate
     * supplier or function.
     */
    default <Folded> Folded fold(
        Supplier<? extends Folded> onNone,
        Function<? super Value, ? extends Folded> onSome
    ) {
        return switch (this) {
            case Some<Value> s -> onSome.apply(s.value());
            case None<Value> _ -> onNone.get();
        };
    }

    /**
     * Executes one of the provided actions based on the state of this value.
     * If the value is "Some", the provided consumer is executed with the inner value.
     * If the value is "None", the provided runnable is executed.
     *
     * @param onNone the action to execute if the value is "None"
     * @param onSome the consumer to execute if the value is "Some", accepting the inner value
     */
    default void match(Runnable onNone, Consumer<? super Value> onSome) {
        switch (this) {
            case Some<Value> s -> onSome.accept(s.value());
            case None<Value> _ -> onNone.run();
        }
    }

    // ---------- Stream interoperability ----------

    /**
     * Returns a stream representation of the current instance.
     * If the instance is of type {@code Some<Value>}, the stream contains the value.
     * If the instance is of type {@code None<Value>}, the stream is empty.
     * <p>
     * Java 9+ Optional has stream(); this mirrors that.
     * - {@code Some(v) -> Stream.of(v)}
     * - {@code None -> Stream.empty()}
     *
     * @return a stream containing the value if present, or an empty stream if no value exists
     */
    default Stream<Value> stream() {
        return switch (this) {
            case Some<Value> s -> Stream.of(s.value());
            case None<Value> _ -> Stream.empty();
        };
    }

    /**
     * Converts the current instance of a value or none container into an {@code Optional}.
     *
     * @return an {@code Optional} containing the value if the instance is of type {@code Some},
     * or an empty {@code Optional} if the instance is of type {@code None}.
     */
    default Optional<Value> toOptional() {
        return switch (this) {
            case Some<Value> s -> Optional.of(s.value());
            case None<Value> _ -> Optional.empty();
        };
    }

    /**
     * Collects all present (non-empty) values from a stream of {@code Option} instances into a list.
     * <p>
     * Equivalent to stream.flatMap(Option::stream).collect(toList()).
     *
     * @param options a stream of {@code Option} instances
     * @param <V>     the type of the values inside the {@code Option} instances
     * @return a list containing all present values from the provided stream
     */
    static <V> List<V> collectPresent(Stream<Option<V>> options) {
        return options.flatMap(Option::stream).collect(Collectors.toList());
    }

    /**
     * Creates a collector that transforms a stream of Option objects into a list of values
     * by extracting the present values and filtering out any absent values.
     *
     * @param <V> the type of the values inside the Option objects
     * @return a Collector that collects present values into a List
     */
    static <V> Collector<Option<? extends V>, ?, List<V>> presentValuesToList() {
        return Collectors.flatMapping(Option::stream, Collectors.toList());
    }

    /**
     * Returns a {@link Collector} that reduces a {@code Stream<Option<V>>} to a single
     * {@code Optional<List<V>>}.
     *
     * <p>The result is {@link Optional#of(Object) present} only if <em>every</em> element in the
     * stream is {@link Option#some(Object) Some}. The first {@link Option#none() None} causes the
     * entire result to be {@link Optional#empty()}. An empty stream yields an empty list wrapped
     * in {@code Optional.of}.
     *
     * <p>This mirrors {@link #sequence(Iterable)} but operates as a stream {@link Collector}.
     *
     * <p>Example:
     * <pre>{@code
     * Optional<List<User>> allFound = ids.stream()
     *     .map(userRepo::findById)             // Stream<Option<User>>
     *     .collect(Option.sequenceCollector()); // Optional<List<User>>
     * }</pre>
     *
     * @param <V> the element type inside each {@code Option}
     * @return a collector producing {@code Optional<List<V>>}
     */
    static <V> Collector<Option<V>, ?, Optional<List<V>>> sequenceCollector() {
        class Acc {
            final ArrayList<V> values = new ArrayList<>();
            boolean hasNone = false;
        }
        return Collector.of(
            Acc::new,
            (acc, opt) -> {
                Objects.requireNonNull(opt, "sequenceCollector stream element must not be null");
                if (!acc.hasNone) {
                    switch (opt) {
                        case None<V> __ -> acc.hasNone = true;
                        case Some<V> s  -> acc.values.add(s.value());
                    }
                }
            },
            (a, b) -> {
                if (a.hasNone || b.hasNone) { a.hasNone = true; return a; }
                a.values.addAll(b.values);
                return a;
            },
            acc -> acc.hasNone
                ? Optional.empty()
                : Optional.of(Collections.unmodifiableList(acc.values))
        );
    }

    // ---------- sequence / traverse ----------

    /**
     * Transforms an iterable of {@code Option<V>} into a single {@code Option<List<V>>}.
     * If any element in the input iterable is {@code None}, this method returns {@code Option.none()}.
     * If all elements are {@code Some}, the result is {@code Option.some()} containing a list of values.
     *
     * @param <V>     The type of the values wrapped in the {@code Option}.
     * @param options An iterable containing {@code Option<V>} elements to be transformed.
     * @return {@code Option.some()} containing a list of values if all elements are {@code Some},
     * or {@code Option.none()} if any element is {@code None}.
     * @throws NullPointerException If the {@code options} iterable is {@code null} or contains {@code null} elements.
     */
    static <V> Option<List<V>> sequence(Iterable<Option<V>> options) {
        Objects.requireNonNull(options, "options");
        return sequence(StreamSupport.stream(options.spliterator(), false));
    }

    /**
     * Converts a stream of {@link Option} objects into a single {@link Option} containing
     * a {@link List} of values, if all {@link Option} instances in the stream are {@link Some}.
     * If the stream contains any {@link None}, the result will be {@link Option#none()}.
     *
     * @param <V>     the type of elements contained in the {@link Option} instances
     * @param options a stream of {@link Option} elements to be sequenced
     * @return an {@link Option} containing a {@link List} of values if all elements are {@link Some},
     * or {@link Option#none()} if any element in the stream is {@link None}
     * @throws NullPointerException if the stream or any of its elements is {@code null}
     */
    static <V> Option<List<V>> sequence(Stream<Option<V>> options) {
        Objects.requireNonNull(options, "options");
        try (var gathered = options.gather(Gatherer.<Option<V>, ArrayList<V>, Option<List<V>>>ofSequential(
                ArrayList::new,
                (state, element, downstream) -> {
                    Objects.requireNonNull(element, "options contains null element (use Option.none() instead)");
                    if (element instanceof None<V>) {
                        downstream.push(Option.none());
                        return false;
                    }
                    state.add(((Some<V>) element).value());
                    return true;
                },
                (state, downstream) -> downstream.push(Option.some(List.copyOf(state)))
        ))) {
            return gathered.findFirst().orElseThrow();
        }
    }

    /**
     * Transforms a collection of values of type A into an optional list of values of type B
     * by applying a given mapping function to each element in the input collection.
     * If the mapping function returns a {@code None} for any element, this method returns {@code Option.none()}.
     *
     * @param <A>    the type of the elements in the input collection
     * @param <B>    the type of the elements in the resulting optional list
     * @param values the collection of input values to be transformed, must not be null
     * @param mapper the mapping function to apply to each element in the input collection, must not return null or {@code Option.none()} for valid outputs
     * @return an {@code Option} containing a list of transformed values if all transformations succeed,
     * or {@code Option.none()} if the mapping function produces a {@code None} for any element
     * @throws NullPointerException if {@code values} or {@code mapper} is null,
     *                              or if the mapping function returns {@code null}
     */
    static <A, B> Option<List<B>> traverse(Iterable<A> values, Function<? super A, Option<B>> mapper) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mapper, "mapper");
        return traverse(StreamSupport.stream(values.spliterator(), false), mapper);
    }

    /**
     * Transforms a stream of values by applying a mapper function that returns an {@code Option} for each input value.
     * If the mapper function returns {@code None} for any value, the entire result is {@code None}.
     * Otherwise, returns a {@code Some} wrapping a list of mapped values.
     *
     * @param <A>    the type of the input elements in the stream
     * @param <B>    the type of the elements in the output {@code List}
     * @param values the stream of input values to traverse
     * @param mapper the mapping function to transform each input value into an {@code Option} of the output type
     * @return an {@code Option} containing a {@code List} of transformed values if all inputs are successfully mapped, or {@code None} if the mapper returns {@code None} for any
     * value
     * @throws NullPointerException if {@code values} or {@code mapper} is null, or if the mapper function returns {@code null} for any input
     */
    static <A, B> Option<List<B>> traverse(Stream<A> values, Function<? super A, Option<B>> mapper) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mapper, "mapper");
        try (var gathered = values.gather(Gatherer.<A, ArrayList<B>, Option<List<B>>>ofSequential(
                ArrayList::new,
                (state, element, downstream) -> {
                    Option<B> ob = Objects.requireNonNull(mapper.apply(element), "traverse mapper must not return null");
                    if (ob instanceof None<B>) {
                        downstream.push(Option.none());
                        return false;
                    }
                    state.add(((Some<B>) ob).value());
                    return true;
                },
                (state, downstream) -> downstream.push(Option.some(List.copyOf(state)))
        ))) {
            return gathered.findFirst().orElseThrow();
        }
    }

    // ---------- Interoperability: Option <-> Result / Try ----------

    /**
     * Converts the current option to a {@code Result} instance.
     *
     * @param errorIfNone the error value to use if the current option is {@code None}.
     * @param <TError>    the type of the error value.
     * @return a {@code Result} containing the value if this is {@code Some}, or an error if this is {@code None}.
     */
    default <TError> Result<Value, TError> toResult(TError errorIfNone) {
        return switch (this) {
            case Some<Value> s -> Result.ok(s.value());
            case None<Value> _ -> Result.err(errorIfNone);
        };
    }

    /**
     * Converts the current instance to a {@code Try} instance.
     *
     * @param exceptionSupplier a supplier that provides the exception to be used
     *                          when the current instance is {@code None}.
     * @return a {@code Try} instance containing the value if the current instance
     * is {@code Some}, or a failed {@code Try} with the supplied exception
     * if the current instance is {@code None}.
     */
    default Try<Value> toTry(Supplier<? extends Throwable> exceptionSupplier) {
        return switch (this) {
            case Some<Value> s -> Try.success(s.value());
            case None<Value> _ -> Try.failure(
                Objects.requireNonNull(exceptionSupplier.get(), "exceptionSupplier returned null")
            );
        };
    }

    /**
     * Converts this {@code Option} to an {@link Either}.
     *
     * <p>{@code Some(v)} maps to {@link Either#right(Object)}; {@code None} maps to
     * {@link Either#left(Object)} using the supplied left value. Mirrors the
     * existing {@link #toResult(Object)} overload for the neutral two-track type.
     *
     * @param <L>        the left type
     * @param leftIfNone the value to use as the {@link Either.Left} when this is {@code None};
     *                   must not be {@code null}
     * @return an {@code Either<L, Value>} equivalent of this {@code Option}
     * @throws NullPointerException if {@code leftIfNone} is {@code null}
     */
    default <L> Either<L, Value> toEither(L leftIfNone) {
        Objects.requireNonNull(leftIfNone, "leftIfNone");
        return switch (this) {
            case Some<Value> s -> Either.right(s.value());
            case None<Value> _ -> Either.left(leftIfNone);
        };
    }

    /**
     * Converts a {@link Result} into an {@link Option}.
     * <p>
     * If the given {@code result} represents a successful value (i.e., {@code isOk()} returns true),
     * the value is wrapped in an {@code Option}. Otherwise, {@code Option.none()} is returned.
     *
     * @param <V>    the type of the value contained in the result
     * @param <E>    the type of the error contained in the result
     * @param result the {@code Result} to be converted, must not be null
     * @return an {@code Option} containing the value from the result if it is successful, or an
     * {@code Option.none()} if the result contains an error
     */
    static <V, E> Option<V> fromResult(Result<? extends V, ? extends E> result) {
        Objects.requireNonNull(result, "result");
        return result.isOk() ? Option.ofNullable(result.get()) : Option.none();
    }

    /**
     * Converts a {@link Try} instance into an {@link Option}.
     * If the {@link Try} is successful, the resulting {@link Option} contains the value.
     * If the {@link Try} is a failure, the resulting {@link Option} is empty.
     *
     * @param t   the {@link Try} instance to convert, must not be null
     * @param <V> the type of the value contained in the {@link Try}
     * @return an {@link Option} containing the value if the {@link Try} is successful, or an empty {@link Option} if it is a failure
     */
    static <V> Option<V> fromTry(Try<? extends V> t) {
        Objects.requireNonNull(t, "t");
        return t.isSuccess() ? Option.ofNullable(t.get()) : Option.none();
    }

    // ---------- zip / map2 ----------

    /**
     * Combines the current {@code Option} instance with another {@code Option} instance into a single {@code Option}
     * containing a {@link Tuple2} of their values, if both options are non-empty.
     *
     * @param <B>   the type of the value contained in the other {@code Option}
     * @param other the other {@code Option} to combine with
     * @return an {@code Option} containing a {@link Tuple2} of the values from both options if both are non-empty,
     * otherwise an empty {@code Option}
     */
    default <B> Option<Tuple2<Value, B>> zip(Option<? extends B> other) {
        Objects.requireNonNull(other, "other");
        return zip(this, other);
    }

    /**
     * Combines the values of this Option with the values of another Option using a provided combining function.
     *
     * @param <B>      the type of the value contained in the other Option
     * @param <R>      the type of the result produced by the combining function
     * @param other    the other Option to combine with
     * @param combiner the function to combine the values from this Option and the other Option
     * @return an Option containing the result of applying the combining function to the values,
     * or an empty Option if either this Option or the other Option is empty
     */
    default <B, R> Option<R> zipWith(Option<? extends B> other,
                                     BiFunction<? super Value, ? super B, ? extends R> combiner) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(combiner, "combiner");
        return map2(this, other, combiner);
    }

    /**
     * Applies {@code mapper} to the value inside this {@code Option} and pairs the original value
     * with the result. Returns {@link Option#none()} if this is empty <em>or</em> if the mapper
     * returns {@link Option#none()}.
     *
     * <p>This is the monadic "dependent zip": the second {@code Option} is computed <em>from</em>
     * the first value, unlike {@link #zip(Option)} which takes an already-evaluated option.
     *
     * <pre>{@code
     * Option<String> name = Option.some("alice");
     * Option<Tuple2<String, Integer>> result =
     *     name.zipWith(n -> lookupAge(n));
     * // Some(Tuple2("alice", 30)) if lookupAge returns Some(30)
     * // None                      if lookupAge returns None
     * }</pre>
     *
     * @param <B>    type of the value produced by {@code mapper}
     * @param mapper function that receives this option's value and returns an {@code Option<B>};
     *               must not be {@code null}, and must not return {@code null}
     * @return {@code Some(Tuple2(thisValue, b))} if both are present, otherwise {@code None}
     * @throws NullPointerException if {@code mapper} is {@code null} or if {@code mapper} returns
     *                              {@code null}
     */
    default <B> Option<Tuple2<Value, B>> zipWith(
            Function<? super Value, ? extends Option<? extends B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return flatMap(v -> {
            Option<? extends B> opt =
                Objects.requireNonNull(mapper.apply(v), "mapper must not return null");
            return opt.map(b -> new Tuple2<>(v, b));
        });
    }

    /**
     * Alias for {@link #zipWith(Function)}.
     *
     * <p>Applies {@code mapper} to the value inside this {@code Option} and pairs the original
     * value with the result. Useful when the name {@code flatZip} better communicates intent
     * at the call site.
     *
     * @param <B>    type of the value produced by {@code mapper}
     * @param mapper function that receives this option's value and returns an {@code Option<B>};
     *               must not be {@code null}, and must not return {@code null}
     * @return {@code Some(Tuple2(thisValue, b))} if both are present, otherwise {@code None}
     * @throws NullPointerException if {@code mapper} is {@code null} or if {@code mapper} returns
     *                              {@code null}
     */
    default <B> Option<Tuple2<Value, B>> flatZip(
            Function<? super Value, ? extends Option<? extends B>> mapper) {
        return zipWith(mapper);
    }

    /**
     * Combines this {@code Option} with two others into an {@code Option<Tuple3>}.
     * Returns {@link Option#none()} if any of the three is empty.
     *
     * @param <B> type of the value in {@code b}
     * @param <C> type of the value in {@code c}
     * @param b   second option; must not be {@code null}
     * @param c   third option; must not be {@code null}
     * @return {@code Some(Tuple3(v1, v2, v3))} if all three are non-empty, otherwise {@code None}
     * @throws NullPointerException if {@code b} or {@code c} is {@code null}
     */
    default <B, C> Option<Tuple3<Value, B, C>> zip3(Option<? extends B> b, Option<? extends C> c) {
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(c, "c");
        return zip3(this, b, c);
    }

    /**
     * Combines this {@code Option} with two others using a {@link TriFunction}.
     * Returns {@link Option#none()} if any of the three is empty.
     *
     * @param <B>      type of the value in {@code b}
     * @param <C>      type of the value in {@code c}
     * @param <R>      result type
     * @param b        second option; must not be {@code null}
     * @param c        third option; must not be {@code null}
     * @param combiner function applied to the three values; must not be {@code null}
     * @return {@code Some(combiner(v1, v2, v3))} if all three are non-empty, otherwise {@code None}
     * @throws NullPointerException if {@code b}, {@code c}, or {@code combiner} is {@code null}
     */
    default <B, C, R> Option<R> zipWith3(
            Option<? extends B> b,
            Option<? extends C> c,
            TriFunction<? super Value, ? super B, ? super C, ? extends R> combiner) {
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(c, "c");
        Objects.requireNonNull(combiner, "combiner");
        return map3(this, b, c, combiner);
    }

    /**
     * Combines this {@code Option} with three others into an {@code Option<Tuple4>}.
     * Returns {@link Option#none()} if any of the four is empty.
     *
     * @param <B> type of the value in {@code b}
     * @param <C> type of the value in {@code c}
     * @param <D> type of the value in {@code d}
     * @param b   second option; must not be {@code null}
     * @param c   third option; must not be {@code null}
     * @param d   fourth option; must not be {@code null}
     * @return {@code Some(Tuple4(v1, v2, v3, v4))} if all four are non-empty, otherwise {@code None}
     * @throws NullPointerException if {@code b}, {@code c}, or {@code d} is {@code null}
     */
    default <B, C, D> Option<Tuple4<Value, B, C, D>> zip4(
            Option<? extends B> b, Option<? extends C> c, Option<? extends D> d) {
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(c, "c");
        Objects.requireNonNull(d, "d");
        return zip4(this, b, c, d);
    }

    /**
     * Combines this {@code Option} with three others using a {@link QuadFunction}.
     * Returns {@link Option#none()} if any of the four is empty.
     *
     * @param <B>      type of the value in {@code b}
     * @param <C>      type of the value in {@code c}
     * @param <D>      type of the value in {@code d}
     * @param <R>      result type
     * @param b        second option; must not be {@code null}
     * @param c        third option; must not be {@code null}
     * @param d        fourth option; must not be {@code null}
     * @param combiner function applied to the four values; must not be {@code null}
     * @return {@code Some(combiner(v1, v2, v3, v4))} if all four are non-empty, otherwise {@code None}
     * @throws NullPointerException if {@code b}, {@code c}, {@code d}, or {@code combiner} is {@code null}
     */
    default <B, C, D, R> Option<R> zipWith4(
            Option<? extends B> b,
            Option<? extends C> c,
            Option<? extends D> d,
            QuadFunction<? super Value, ? super B, ? super C, ? super D, ? extends R> combiner) {
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(c, "c");
        Objects.requireNonNull(d, "d");
        Objects.requireNonNull(combiner, "combiner");
        return map4(this, b, c, d, combiner);
    }

    /**
     * Combines two {@link Option} instances into a single {@link Option} containing a {@link Tuple2} of the values
     * if both options are non-empty. If either option is empty, returns an empty {@link Option}.
     * <p>
     * {@code zip: Option<A> + Option<B> -> Option<Tuple2<A,B>>}
     *
     * @param <A> the type of the value in the first {@link Option}
     * @param <B> the type of the value in the second {@link Option}
     * @param a   the first {@link Option} instance, must not be null
     * @param b   the second {@link Option} instance, must not be null
     * @return an {@link Option} containing a {@link Tuple2} of the values if both options are non-empty,
     * otherwise an empty {@link Option}
     * @throws NullPointerException if either {@code a} or {@code b} is null
     */
    static <A, B> Option<Tuple2<A, B>> zip(Option<? extends A> a, Option<? extends B> b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");

        if (a instanceof None<?> || b instanceof None<?>) {
            return Option.none();
        }

        A av = ((Some<? extends A>) a).value();
        B bv = ((Some<? extends B>) b).value();
        return Option.some(new Tuple2<>(av, bv));
    }

    /**
     * Combines the values of two Option instances using the provided combiner function.
     * If either Option is empty (None), the result is an empty Option.
     * If both Options contain values, the combiner function is applied and the result is wrapped in an Option.
     *
     * @param a        the first Option, which may or may not contain a value
     * @param b        the second Option, which may or may not contain a value
     * @param combiner the function used to combine the values of a and b if both are present
     * @param <A>      the type of the value contained in the first Option
     * @param <B>      the type of the value contained in the second Option
     * @param <R>      the type of the value contained in the resulting Option
     * @return an Option containing the result of applying the combiner function to the values of a and b,
     * or an empty Option if either a or b is empty
     */
    static <A, B, R> Option<R> map2(
        Option<? extends A> a,
        Option<? extends B> b,
        BiFunction<? super A, ? super B, ? extends R> combiner
    ) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(combiner, "combiner");

        if (a instanceof None<?> || b instanceof None<?>) {
            return Option.none();
        }

        A av = ((Some<? extends A>) a).value();
        B bv = ((Some<? extends B>) b).value();
        return Option.ofNullable(combiner.apply(av, bv));
    }

    // ---------- zip3 / map3 ----------

    /**
     * Combines three {@link Option} instances into a single {@link Option} containing a {@link Tuple3}.
     * Returns {@link Option#none()} if any of the three is empty.
     *
     * @param <A> type of the value in {@code a}
     * @param <B> type of the value in {@code b}
     * @param <C> type of the value in {@code c}
     * @param a   first option; must not be {@code null}
     * @param b   second option; must not be {@code null}
     * @param c   third option; must not be {@code null}
     * @return {@code Some(Tuple3(av, bv, cv))} when all three are non-empty, otherwise {@code None}
     * @throws NullPointerException if {@code a}, {@code b}, or {@code c} is {@code null}
     */
    static <A, B, C> Option<Tuple3<A, B, C>> zip3(
            Option<? extends A> a,
            Option<? extends B> b,
            Option<? extends C> c) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(c, "c");
        if (a instanceof None<?> || b instanceof None<?> || c instanceof None<?>) {
            return Option.none();
        }
        A av = ((Some<? extends A>) a).value();
        B bv = ((Some<? extends B>) b).value();
        C cv = ((Some<? extends C>) c).value();
        return Option.some(new Tuple3<>(av, bv, cv));
    }

    /**
     * Combines the values of three {@link Option} instances using the provided {@link TriFunction}.
     * Returns {@link Option#none()} if any of the three is empty.
     *
     * @param <A>      type of the value in {@code a}
     * @param <B>      type of the value in {@code b}
     * @param <C>      type of the value in {@code c}
     * @param <R>      result type
     * @param a        first option; must not be {@code null}
     * @param b        second option; must not be {@code null}
     * @param c        third option; must not be {@code null}
     * @param combiner function applied to the three values; must not be {@code null}
     * @return {@code Some(combiner(av, bv, cv))} when all three are non-empty, otherwise {@code None}
     * @throws NullPointerException if any argument is {@code null}
     */
    static <A, B, C, R> Option<R> map3(
            Option<? extends A> a,
            Option<? extends B> b,
            Option<? extends C> c,
            TriFunction<? super A, ? super B, ? super C, ? extends R> combiner) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(c, "c");
        Objects.requireNonNull(combiner, "combiner");
        if (a instanceof None<?> || b instanceof None<?> || c instanceof None<?>) {
            return Option.none();
        }
        A av = ((Some<? extends A>) a).value();
        B bv = ((Some<? extends B>) b).value();
        C cv = ((Some<? extends C>) c).value();
        return Option.ofNullable(combiner.apply(av, bv, cv));
    }

    // ---------- zip4 / map4 ----------

    /**
     * Combines four {@link Option} instances into a single {@link Option} containing a {@link Tuple4}.
     * Returns {@link Option#none()} if any of the four is empty.
     *
     * @param <A> type of the value in {@code a}
     * @param <B> type of the value in {@code b}
     * @param <C> type of the value in {@code c}
     * @param <D> type of the value in {@code d}
     * @param a   first option; must not be {@code null}
     * @param b   second option; must not be {@code null}
     * @param c   third option; must not be {@code null}
     * @param d   fourth option; must not be {@code null}
     * @return {@code Some(Tuple4(av, bv, cv, dv))} when all four are non-empty, otherwise {@code None}
     * @throws NullPointerException if {@code a}, {@code b}, {@code c}, or {@code d} is {@code null}
     */
    static <A, B, C, D> Option<Tuple4<A, B, C, D>> zip4(
            Option<? extends A> a,
            Option<? extends B> b,
            Option<? extends C> c,
            Option<? extends D> d) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(c, "c");
        Objects.requireNonNull(d, "d");
        if (a instanceof None<?> || b instanceof None<?> || c instanceof None<?> || d instanceof None<?>) {
            return Option.none();
        }
        A av = ((Some<? extends A>) a).value();
        B bv = ((Some<? extends B>) b).value();
        C cv = ((Some<? extends C>) c).value();
        D dv = ((Some<? extends D>) d).value();
        return Option.some(new Tuple4<>(av, bv, cv, dv));
    }

    /**
     * Combines the values of four {@link Option} instances using the provided {@link QuadFunction}.
     * Returns {@link Option#none()} if any of the four is empty.
     *
     * @param <A>      type of the value in {@code a}
     * @param <B>      type of the value in {@code b}
     * @param <C>      type of the value in {@code c}
     * @param <D>      type of the value in {@code d}
     * @param <R>      result type
     * @param a        first option; must not be {@code null}
     * @param b        second option; must not be {@code null}
     * @param c        third option; must not be {@code null}
     * @param d        fourth option; must not be {@code null}
     * @param combiner function applied to the four values; must not be {@code null}
     * @return {@code Some(combiner(av, bv, cv, dv))} when all four are non-empty, otherwise {@code None}
     * @throws NullPointerException if any argument is {@code null}
     */
    static <A, B, C, D, R> Option<R> map4(
            Option<? extends A> a,
            Option<? extends B> b,
            Option<? extends C> c,
            Option<? extends D> d,
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends R> combiner) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(c, "c");
        Objects.requireNonNull(d, "d");
        Objects.requireNonNull(combiner, "combiner");
        if (a instanceof None<?> || b instanceof None<?> || c instanceof None<?> || d instanceof None<?>) {
            return Option.none();
        }
        A av = ((Some<? extends A>) a).value();
        B bv = ((Some<? extends B>) b).value();
        C cv = ((Some<? extends C>) c).value();
        D dv = ((Some<? extends D>) d).value();
        return Option.ofNullable(combiner.apply(av, bv, cv, dv));
    }

}
