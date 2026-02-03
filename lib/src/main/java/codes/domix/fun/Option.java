package codes.domix.fun;

import java.util.ArrayList;
import java.util.Iterator;
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
import java.util.stream.Stream;

/**
 * A sealed interface representing an optional value with two possible states:
 * either a value is present ("Some") or absent ("None").
 * <p>
 * This interface provides a flexible alternative to {@link Optional},
 * integrating functional-style operations for working with optional values in a more
 * expressive and composable manner.
 *
 * @param <Value> The type of the optional value.
 */
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
    }

    /**
     * A record representing a tuple with two elements.
     * The tuple holds two values of potentially different types.
     *
     * @param <A> the type of the first element of the tuple
     * @param <B> the type of the second element of the tuple
     * @param _1  the first element of the tuple
     * @param _2  the second element of the tuple
     */
    record Tuple2<A, B>(A _1, B _2) {
    }

    // ---------- Factories ----------

    /**
     * Creates an Option instance that encapsulates a given non-null value. If the value is null,
     * it returns a None instance instead.
     *
     * @param <V>   the type of the value to encapsulate
     * @param value the value to be encapsulated; if null, a None instance is returned
     * @return an Option containing the provided value if it is non-null, or a None instance if the value is null
     */
    static <V> Option<V> some(V value) {
        return value == null ? none() : new Some<>(value);
    }

    /**
     * Creates an instance of {@link Option} that represents the absence of a value.
     *
     * @param <V> the type of the value that would be held by the {@link Option}, if present
     * @return an {@code Option} instance that signifies no value is present
     */
    static <V> Option<V> none() {
        return new None<>();
    }

    /**
     * Creates an Option instance that encapsulates a given value. If the value is null, it returns a None instance.
     *
     * @param <V>   the type of the value to encapsulate
     * @param value the value to be encapsulated; if null, a None instance is returned
     * @return an Option containing the provided value if it is non-null, or a None instance if the value is null
     */
    static <V> Option<V> ofNullable(V value) {
        return some(value);
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
        ArrayList<V> out = new ArrayList<>();
        for (Option<V> opt : options) {
            if (opt == null) {
                throw new NullPointerException("options contains null element (use Option.none() instead)");
            }
            if (opt instanceof None<V>) {
                return Option.none();
            }
            out.add(((Some<V>) opt).value());
        }
        return Option.some(List.copyOf(out));
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
        ArrayList<V> out = new ArrayList<>();
        try (options) {
            Iterator<Option<V>> it = options.iterator();
            while (it.hasNext()) {
                Option<V> opt = it.next();
                if (opt == null) {
                    throw new NullPointerException("options contains null element (use Option.none() instead)");
                }
                if (opt instanceof None<V>) {
                    return Option.none();
                }
                out.add(((Some<V>) opt).value());
            }
        }
        return Option.some(List.copyOf(out));
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
        ArrayList<B> out = new ArrayList<>();
        for (A a : values) {
            Option<B> ob = Objects.requireNonNull(mapper.apply(a), "traverse mapper must not return null");
            if (ob instanceof None<B>) {
                return Option.none();
            }
            out.add(((Some<B>) ob).value());
        }
        return Option.some(List.copyOf(out));
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
        ArrayList<B> out = new ArrayList<>();
        try (values) {
            Iterator<A> it = values.iterator();
            while (it.hasNext()) {
                A a = it.next();
                Option<B> ob = Objects.requireNonNull(mapper.apply(a), "traverse mapper must not return null");
                if (ob instanceof None<B>) {
                    return Option.none();
                }
                out.add(((Some<B>) ob).value());
            }
        }
        return Option.some(List.copyOf(out));
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
            case None<Value> _ -> Try.failure(exceptionSupplier.get());
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

    /**
     * Converts an {@code Option} into a {@code Result}.
     * If the {@code Option} is defined, the result will contain the value; otherwise,
     * the result will contain the specified error.
     *
     * @param <V>         the type of the success value in the {@code Result}
     * @param <E>         the type of the error value in the {@code Result}
     * @param opt         the {@code Option} to be converted; must not be null
     * @param errorIfNone the error value to be used if the {@code Option} is not defined
     * @return a {@code Result} containing the value of the {@code Option} if defined,
     * or the specified error if the {@code Option} is not defined
     */
    static <V, E> Result<V, E> toResult(Option<? extends V> opt, E errorIfNone) {
        Objects.requireNonNull(opt, "opt");
        return opt.isDefined() ? Result.ok(((Some<? extends V>) opt).value()) : Result.err(errorIfNone);
    }

    /**
     * Converts an {@code Option} to a {@code Try}. If the {@code Option} is defined, a successful {@code Try} is returned
     * containing the value from the {@code Option}. If the {@code Option} is empty, a failed {@code Try} is returned
     * containing the exception supplied by the given {@code Supplier}.
     *
     * @param <V>               the type of the value contained in the {@code Option}
     * @param opt               the {@code Option} to be converted; must not be null
     * @param exceptionSupplier the {@code Supplier} providing the exception for a failed {@code Try}; must not be null
     * @return a {@code Try} representing either the value from the {@code Option} or a failure with the supplied exception
     */
    static <V> Try<V> toTry(Option<? extends V> opt, Supplier<? extends Throwable> exceptionSupplier) {
        Objects.requireNonNull(opt, "opt");
        Objects.requireNonNull(exceptionSupplier, "exceptionSupplier");
        return opt.isDefined()
            ? Try.success(((Some<? extends V>) opt).value())
            : Try.failure(exceptionSupplier.get());
    }

    // ---------- zip / map2 ----------

    /**
     * Combines the current {@code Option} instance with another {@code Option} instance into a single {@code Option}
     * containing a {@code Tuple2} of their values, if both options are non-empty.
     *
     * @param <B>   the type of the value contained in the other {@code Option}
     * @param other the other {@code Option} to combine with
     * @return an {@code Option} containing a {@code Tuple2} of the values from both options if both are non-empty,
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

}
