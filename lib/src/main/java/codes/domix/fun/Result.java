package codes.domix.fun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


/**
 * A generic sealed interface representing a result type that can either be a successful value
 * ({@link Result.Ok}) or an error ({@link Result.Err}).
 * This is a common functional programming construct that allows for
 * handling both success and error cases in a unified type.
 *
 * <p>This interface is {@link NullMarked}: all types are non-null by default.
 * {@code Ok.value} is guaranteed non-null; use {@link Option} inside the value type
 * to model an optional successful result.
 *
 * @param <Value> the type of the value contained in a successful result
 * @param <Error> the type of the error contained in an erroneous result
 */
@NullMarked
public sealed interface Result<Value, Error> permits Result.Ok, Result.Err {
    /**
     * Represents a successful result containing a value of type {@code Value}.
     * <p>
     * This record implements the {@link Result} interface and indicates the "Ok" variant
     * of a result. It encapsulates a non-null value that represents a successful computation
     * or action. To model a successful result with no meaningful value use {@code Result<Void, Error>};
     * for an optional value use {@code Result<Option<Value>, Error>}.
     *
     * @param <Value> the type of the value contained in the successful result
     * @param <Error> the type of the error contained in the erroneous result, unused here
     * @param value   the non-null value that represents the successful result;
     *                passing {@code null} throws {@link NullPointerException}
     */
    record Ok<Value, Error>(Value value) implements Result<Value, Error> {
        /**
         * Compact canonical constructor — validates that {@code value} is non-null.
         *
         * @throws NullPointerException if {@code value} is {@code null}
         */
        public Ok {
            Objects.requireNonNull(value, "Ok value must not be null");
        }
    }

    /**
     * Represents an erroneous result containing an error of type {@code Error}.
     * <p>
     * This record implements the {@link Result} interface and represents the "Err" variant
     * of a result. It encapsulates an error value that indicates a failed computation or action.
     *
     * @param <Value> the type of the value contained in a successful result, unused here
     * @param <Error> the type of the error contained in the erroneous result
     * @param error   the non-null error value that represents the erroneous result;
     *                passing {@code null} throws {@link NullPointerException}
     */
    record Err<Value, Error>(Error error) implements Result<Value, Error> {
        /**
         * Compact canonical constructor — validates that {@code error} is non-null.
         *
         * @throws NullPointerException if {@code error} is {@code null}
         */
        public Err {
            Objects.requireNonNull(error, "Err error must not be null");
        }
    }

    /**
     * A typed container holding the two partitions produced by {@link #partitioningBy()}.
     *
     * @param <V>    the value type of the {@code Ok} elements
     * @param <E>    the error type of the {@code Err} elements
     * @param oks    an unmodifiable list of values extracted from {@code Ok} elements, in encounter order
     * @param errors an unmodifiable list of errors extracted from {@code Err} elements, in encounter order
     */
    record Partition<V, E>(List<V> oks, List<E> errors) {
        /** Defensively copies both lists and null-checks them. */
        public Partition {
            Objects.requireNonNull(oks, "oks");
            Objects.requireNonNull(errors, "errors");
            oks = List.copyOf(oks);
            errors = List.copyOf(errors);
        }
    }


    /**
     * Creates a {@link Result} instance representing a successful result with the given value.
     * This method is used to indicate that a computation or action has succeeded.
     *
     * @param <Value> the type of the value contained in the successful result
     * @param <Error> the type of the error that would be contained in an erroneous result
     * @param value   the non-null value to encapsulate in the successful result
     * @return a {@link Result} instance of type {@code Ok<Value, Error>} containing the provided value
     * @throws NullPointerException if {@code value} is {@code null}
     */
    static <Value, Error> Result<Value, Error> ok(Value value) {
        return new Ok<>(value);
    }

    /**
     * Creates an instance of {@code Ok} representing a successful result.
     *
     * @param <Value>           the type of the value in the success case
     * @param <Error>           the type of the error in the error case
     * @param value             the value to be wrapped in the {@code Ok} instance
     * @param errorClassIgnored the class object of the error type, ignored in this method
     * @return an {@code Ok} instance containing the provided value
     * @deprecated This overload exists only as a type-inference aid and is no longer needed.
     * Use {@link #ok(Object)} with an explicit type witness or a typed variable instead:
     * {@code Result.<Value, Error>ok(value)} or {@code Result<Value, Error> r = Result.ok(value)}
     */
    @Deprecated(forRemoval = true)
    static <Value, Error> Result<Value, Error> ok(Value value, Class<Error> errorClassIgnored) {
        return new Ok<>(value);
    }

    /**
     * Creates a {@link Result} instance representing an erroneous result with the given error value.
     * This method is used to indicate that a computation or action has failed.
     *
     * @param <Value> the type of the value that would be contained in a successful result, unused here
     * @param <Error> the type of the error contained in the erroneous result
     * @param error   the non-null error value to encapsulate in the erroneous result
     * @return a {@link Result} instance of type {@code Err<Value, Error>} containing the provided error
     * @throws NullPointerException if {@code error} is {@code null}
     */
    static <Value, Error> Result<Value, Error> err(Error error) {
        return new Err<>(error);
    }

    /**
     * Creates and returns a new result instance representing an error state.
     *
     * @param error             The error to be encapsulated in the result.
     * @param classValueIgnored The ignored class type of the value, typically used for type inference.
     * @param <Value>           The type of the value that would have been contained in a success state.
     * @param <Error>           The type of the error being represented.
     * @return A result instance representing an error state containing the provided error.
     * @deprecated This overload exists only as a type-inference aid and is no longer needed.
     * Use {@link #err(Object)} with an explicit type witness or a typed variable instead:
     * {@code Result.<Value, Error>err(error)} or {@code Result<Value, Error> r = Result.err(error)}
     */
    @Deprecated(forRemoval = true)
    static <Value, Error> Result<Value, Error> err(Error error, Class<Value> classValueIgnored) {
        return new Err<>(error);
    }

    /**
     * Checks if the current instance represents an erroneous result.
     *
     * @return {@code true} if the current instance is of type {@code Err}, indicating an error;
     * {@code false} otherwise.
     */
    default boolean isError() {
        return this instanceof Err;
    }

    /**
     * Checks if the current instance represents a successful result.
     *
     * @return {@code true} if the current instance is of type {@code Ok}, indicating success;
     * {@code false} otherwise.
     */
    default boolean isOk() {
        return this instanceof Ok;
    }

    /**
     * Retrieves the value contained within this {@code Result} instance if it represents a successful result.
     * If the instance represents an erroneous result, this method throws a {@code NoSuchElementException}.
     *
     * @return the non-null value contained in the successful result
     * @throws NoSuchElementException if the instance represents an erroneous result
     */
    default Value get() {
        return switch (this) {
            case Ok<Value, Error> ok -> ok.value();
            case Err<Value, Error> _ -> throw new NoSuchElementException("No value present. This Result is an Error.");
        };
    }

    /**
     * Retrieves the error value of the current instance if it is an Err.
     * If the instance is an Ok, calling this method will throw a NoSuchElementException.
     *
     * @return the error value of the instance if it is an Err.
     * @throws NoSuchElementException if the instance is an Ok.
     */
    default Error getError() {
        return switch (this) {
            case Err<Value, Error> err -> err.error();
            case Ok<Value, Error> _ -> throw new NoSuchElementException("No error present.");
        };
    }

    /**
     * Transforms the value of a successful {@code Result} instance using the provided mapping function.
     * If the {@code Result} instance represents an error, the error remains unchanged.
     *
     * @param <NewValue> the type of the value after applying the mapping function
     * @param mapper     the function to apply to the value of the successful result
     * @return a new {@code Result} instance, containing the mapped value if the original instance
     * was successful, or the same error if the original instance was an error
     */
    default <NewValue> Result<NewValue, Error> map(Function<Value, NewValue> mapper) {
        return switch (this) {
            case Ok<Value, Error> ok -> Result.ok(mapper.apply(ok.value()));
            case Err<Value, Error> err -> Result.err(err.error());
        };
    }

    /**
     * Transforms the error value of an erroneous {@code Result} instance using the provided mapping function.
     * If the {@code Result} instance represents success, the value remains unchanged.
     *
     * @param <NewError> the type of the error after applying the mapping function
     * @param mapper     the function to apply to the error of the erroneous result
     * @return a new {@code Result} instance, containing the transformed error if the original instance
     * was an error, or the same value if the original instance was successful
     */
    default <NewError> Result<Value, NewError> mapError(Function<Error, NewError> mapper) {
        return switch (this) {
            case Ok<Value, Error> ok -> Result.ok(ok.value());
            case Err<Value, Error> err -> Result.err(mapper.apply(err.error()));
        };
    }

    /**
     * Applies the given mapping function to the value of a successful {@code Result} instance
     * and returns the resulting {@code Result}. If the instance represents an error, the error
     * is propagated without applying the mapping function.
     *
     * @param <NewValue> the type of the value in the resulting {@code Result} after applying the mapping function
     * @param mapper     the function to apply to the value of the successful {@code Result} to produce a new {@code Result}
     * @return a new {@code Result} instance returned by applying the mapping function to the value if the original instance
     * was successful, or the same error if the original instance was an error
     */
    default <NewValue> Result<NewValue, Error> flatMap(Function<Value, Result<NewValue, Error>> mapper) {
        return switch (this) {
            case Ok<Value, Error> ok -> mapper.apply(ok.value());
            case Err<Value, Error> err -> Result.err(err.error());
        };
    }

    /**
     * Executes one of the provided {@link Consumer} functions based on the state of the {@code Result}.
     * If this instance represents a successful result (of type {@code Ok}), the {@code onSuccess}
     * function will be executed with the value contained in the result. If this instance represents
     * an erroneous result (of type {@code Err}), the {@code onError} function will be executed with
     * the error contained in the result.
     *
     * @param onSuccess a {@link Consumer} that accepts the value contained in a successful result
     * @param onError   a {@link Consumer} that accepts the error contained in an erroneous result
     */
    default void match(Consumer<Value> onSuccess, Consumer<Error> onError) {
        switch (this) {
            case Ok<Value, Error> ok -> onSuccess.accept(ok.value());
            case Err<Value, Error> err -> onError.accept(err.error());
        }
    }

    /**
     * Executes the given action if the current {@code Result} instance represents a successful result.
     * If the instance is of type {@code Ok}, the provided action is applied to the contained value.
     * In both cases, the original {@code Result} instance is returned unchanged.
     *
     * @param action a {@link Consumer} that accepts the value contained in a successful result
     * @return the original {@code Result} instance
     */
    default Result<Value, Error> peek(Consumer<Value> action) {
        if (this instanceof Ok<Value, Error>(Value value)) {
            action.accept(value);
        }
        return this;
    }

    /**
     * Executes the given action if the current {@code Result} instance represents an erroneous result.
     * If the instance is of type {@code Err}, the provided action is applied to the contained error value.
     * In both cases, the original {@code Result} instance is returned unchanged.
     *
     * @param action a {@link Consumer} that accepts the error contained in an erroneous result
     * @return the original {@code Result} instance
     */
    default Result<Value, Error> peekError(Consumer<Error> action) {
        if (this instanceof Err<Value, Error>(Error error)) {
            action.accept(error);
        }
        return this;
    }

    /**
     * Returns the value contained in this {@code Result} instance if it represents
     * a successful result. If the instance represents an erroneous result, the provided
     * fallback value is returned instead.
     *
     * @param fallback the fallback value to return if this instance is an erroneous result
     * @return the value contained in a successful result, or the fallback value if this
     * instance represents an error
     */
    default Value getOrElse(Value fallback) {
        return this instanceof Ok<Value, Error>(Value value) ? value : fallback;
    }

    /**
     * Retrieves the value contained within this {@code Result} instance if it represents a successful result.
     * If the instance represents an erroneous result, the value provided by the given {@link Supplier} is returned instead.
     *
     * @param fallbackSupplier a {@link Supplier} that provides the fallback value to use if this instance represents an error
     * @return the value contained in a successful result, or the value provided by {@code fallbackSupplier} if this instance represents an error
     */
    default Value getOrElseGet(Supplier<Value> fallbackSupplier) {
        Objects.requireNonNull(fallbackSupplier, "fallbackSupplier");
        return this instanceof Ok<Value, Error>(Value value)
            ? value
            : Objects.requireNonNull(fallbackSupplier.get(), "fallbackSupplier returned null");
    }

    /**
     * Retrieves the value of the current {@code Result} instance if it represents a successful result,
     * or throws a custom exception mapped from the contained error if it represents an erroneous result.
     *
     * @param exceptionMapper a function that maps the error value of an erroneous result to a
     *                        {@code RuntimeException} to be thrown
     * @return the value contained in the successful result
     * @throws RuntimeException the exception produced by {@code exceptionMapper} if the instance represents an error
     */
    default Value getOrThrow(Function<Error, ? extends RuntimeException> exceptionMapper) {
        return switch (this) {
            case Ok<Value, Error> ok -> ok.value();
            case Err<Value, Error> err -> throw exceptionMapper.apply(err.error());
        };
    }

    /**
     * Retrieves the value contained within this {@code Result} instance if it represents
     * a successful result, or returns {@code null} if it represents an erroneous result.
     *
     * @return the value contained in the successful result, or {@code null} if this
     * instance represents an error
     */
    default @Nullable Value getOrNull() {
        return this instanceof Ok<Value, Error>(Value value) ? value : null;
    }

    /**
     * Returns a single-element {@link Stream} containing the value, or an empty stream on error.
     * Useful for flat-mapping a {@code Stream<Result<V, E>>} to keep only the successful values:
     * <pre>{@code
     * Stream<Integer> values = results.stream().flatMap(Result::stream);
     * }</pre>
     *
     * @return a stream containing the value if this is an {@code Ok}, or an empty stream otherwise
     */
    default Stream<Value> stream() {
        return switch (this) {
            case Ok<Value, Error> ok -> Stream.of(ok.value());
            case Err<Value, Error> __ -> Stream.empty();
        };
    }

    /**
     * Transforms the current {@code Result} instance into a value of type {@code Folded}
     * by applying one of the provided functions based on the state of the instance.
     * If the current instance represents a successful result (of type {@code Ok}),
     * the {@code onSuccess} function is applied to the contained value. If the current instance
     * represents an erroneous result (of type {@code Err}), the {@code onError} function is
     * applied to the contained error.
     *
     * @param <Folded>  the type of the value returned by the provided functions
     * @param onSuccess a function to apply to the value of a successful result
     * @param onError   a function to apply to the error of an erroneous result
     * @return the result of applying the appropriate function, as a value of type {@code Folded}
     */
    default <Folded> Folded fold(Function<Value, Folded> onSuccess, Function<Error, Folded> onError) {
        return switch (this) {
            case Ok<Value, Error> ok -> onSuccess.apply(ok.value());
            case Err<Value, Error> err -> onError.apply(err.error());
        };
    }

    /**
     * Filters the current result based on the specified predicate.
     * If the result is an instance of Ok and the predicate returns false for the value,
     * a new error result is returned with the specified error value.
     * If the current result does not match Ok or the predicate evaluates to true,
     * the current result is returned unchanged.
     *
     * @param predicate    the condition to test the value of the result
     * @param errorIfFalse the error to return if the predicate evaluates to false
     * @return the current result if it is not an Ok instance or the predicate evaluates to true,
     * otherwise a new error result with the specified error value
     */
    default Result<Value, Error> filter(Predicate<Value> predicate, Error errorIfFalse) {
        if (this instanceof Ok<Value, Error>(Value value)) {
            return predicate.test(value) ? this : Result.err(errorIfFalse);
        }
        return this;
    }

    /**
     * Filters the current result by applying a given predicate to its value.
     * If the result is of type Ok and the predicate evaluates to false,
     * the method transforms it into an Err with the error provided by the errorIfFalse function.
     * If the predicate evaluates to true or the result is already of type Err, the original result is returned.
     *
     * @param predicate    the condition to be evaluated on the value wrapped by the Ok result.
     * @param errorIfFalse a function to generate an error if the predicate evaluates to false.
     * @return a filtered result, returning the same instance if the predicate evaluates to true or the result is Err;
     * otherwise, returns an Err with the provided error value.
     */
    default Result<Value, Error> filter(Predicate<Value> predicate, Function<Value, Error> errorIfFalse) {
        if (this instanceof Ok<Value, Error>(Value value)) {
            return predicate.test(value) ? this : Result.err(errorIfFalse.apply(value));
        }
        return this;
    }

    // ---------- Recovery and fallback ----------

    /**
     * Converts an erroneous {@code Result} into a successful one by applying the given rescue function
     * to the contained error. If this instance is already {@code Ok}, it is returned unchanged.
     *
     * @param rescue a function that maps the error value to a recovery value; must not return {@code null}
     * @return an {@code Ok} result with the recovered value, or the original {@code Ok} if already successful
     * @throws NullPointerException if {@code rescue} is null or if {@code rescue} returns {@code null}
     */
    default Result<Value, Error> recover(Function<Error, Value> rescue) {
        Objects.requireNonNull(rescue, "rescue");
        return switch (this) {
            case Ok<Value, Error> ok -> ok;
            case Err<Value, Error> err -> Result.ok(Objects.requireNonNull(rescue.apply(err.error()), "rescue returned null"));
        };
    }

    /**
     * Converts an erroneous {@code Result} into a new {@code Result} by applying the given rescue function
     * to the contained error. Unlike {@link #recover}, the rescue function may itself return an {@code Err}.
     * If this instance is already {@code Ok}, it is returned as an {@code Ok} of the new error type.
     *
     * @param <E2>   the error type of the resulting {@code Result}
     * @param rescue a function that maps the error to a new {@code Result}; must not return {@code null}
     * @return the result of applying {@code rescue} to the error, or an {@code Ok} wrapping the original value
     * @throws NullPointerException if {@code rescue} is null or if {@code rescue} returns {@code null}
     */
    default <E2> Result<Value, E2> recoverWith(Function<Error, Result<Value, E2>> rescue) {
        Objects.requireNonNull(rescue, "rescue");
        return switch (this) {
            case Ok<Value, Error> ok -> Result.ok(ok.value());
            case Err<Value, Error> err -> Objects.requireNonNull(rescue.apply(err.error()), "rescue returned null");
        };
    }

    /**
     * Returns this {@code Result} if it is {@code Ok}; otherwise evaluates the given supplier
     * and returns its result. The supplier is <em>not</em> called when this instance is {@code Ok}.
     *
     * @param fallback a lazy supplier of an alternative {@code Result} evaluated only on {@code Err};
     *                 must not return {@code null}
     * @return this instance if {@code Ok}, or the result of {@code fallback.get()} if {@code Err}
     * @throws NullPointerException if {@code fallback} is null or if {@code fallback} returns {@code null}
     */
    default Result<Value, Error> or(Supplier<Result<Value, Error>> fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return this instanceof Ok ? this : Objects.requireNonNull(fallback.get(), "fallback returned null");
    }

    /**
     * Applies a function to the error of an erroneous result and returns the produced {@code Result}.
     * If this instance is {@code Ok}, it is propagated unchanged (with a potentially different error type).
     * This is the dual of {@link #flatMap}: it operates on the error channel instead of the value channel.
     *
     * @param <E2>   the error type of the resulting {@code Result}
     * @param mapper a function that maps the current error to a new {@code Result}; must not return {@code null}
     * @return the mapped result for {@code Err}, or the original value wrapped as {@code Ok} for {@code Ok}
     * @throws NullPointerException if {@code mapper} is null or if {@code mapper} returns {@code null}
     */
    default <E2> Result<Value, E2> flatMapError(Function<Error, Result<Value, E2>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return switch (this) {
            case Ok<Value, Error> ok -> Result.ok(ok.value());
            case Err<Value, Error> err -> Objects.requireNonNull(mapper.apply(err.error()), "mapper returned null");
        };
    }

    /**
     * Swaps the {@code Ok} and {@code Err} channels of this {@code Result}.
     * An {@code Ok(value)} becomes {@code Err(value)} and an {@code Err(error)} becomes {@code Ok(error)}.
     *
     * @return a {@code Result} with the value and error channels exchanged
     */
    default Result<Error, Value> swap() {
        return switch (this) {
            case Ok<Value, Error> ok -> Result.err(ok.value());
            case Err<Value, Error> err -> Result.ok(err.error());
        };
    }

    /**
     * Returns the value of this {@code Ok}, or applies {@code errorMapper} to the contained error
     * and returns the result. Unlike {@link #getOrElseGet(Supplier)}, the fallback function receives
     * the error value, which is useful for deriving a default from the error itself.
     *
     * <p>This method is intentionally named differently from {@link #getOrElseGet(Supplier)} to avoid
     * overload ambiguity when passing a null literal or a Groovy/Kotlin closure.
     *
     * @param errorMapper a function that maps the error to a fallback value
     * @return the contained value if {@code Ok}, or the result of {@code errorMapper} if {@code Err}
     */
    default Value getOrElseGetWithError(Function<Error, Value> errorMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper");
        return switch (this) {
            case Ok<Value, Error> ok -> ok.value();
            case Err<Value, Error> err -> Objects.requireNonNull(errorMapper.apply(err.error()), "errorMapper returned null");
        };
    }

    // ---------- Interoperability: Result <-> Option / Try ----------

    /**
     * Converts the current instance into an {@code Option<Value>}.
     * If the instance represents a successful result ({@code Ok}), the contained value is wrapped in an {@code Option}.
     * If the instance represents an error ({@code Err}), an empty {@code Option} is returned.
     *
     * @return an {@code Option<Value>} containing the value if the instance is {@code Ok}, or an empty {@code Option} if the instance is {@code Err}.
     */
    default Option<Value> toOption() {
        return switch (this) {
            case Ok<Value, Error> ok -> Option.some(ok.value());
            case Err<Value, Error> _ -> Option.none();
        };
    }

    /**
     * Converts the current result into a {@code Try} instance. If the current
     * result represents a success, the returned {@code Try} will contain the
     * success value. If the current result represents an error, the specified
     * function is used to transform the error into a {@link Throwable}, and
     * the resulting {@code Try} will represent a failure.
     *
     * @param errorToThrowable a {@link Function} that converts the error type
     *                         to a {@link Throwable}. Must not return null.
     * @return a {@code Try} instance representing a success or failure
     * based on the state of the current result.
     * @throws NullPointerException if {@code errorToThrowable} is null or
     *                              if {@code errorToThrowable.apply()}
     *                              returns null.
     */
    default Try<Value> toTry(Function<? super Error, ? extends Throwable> errorToThrowable) {
        Objects.requireNonNull(errorToThrowable, "errorToThrowable");
        return switch (this) {
            case Ok<Value, Error> ok -> Try.success(ok.value());
            case Err<Value, Error> err -> Try.failure(
                Objects.requireNonNull(errorToThrowable.apply(err.error()), "errorToThrowable returned null")
            );
        };
    }

    /**
     * Converts an {@link Option} to a {@link Result}.
     *
     * @param <V>         The type of the value contained in the {@link Option}.
     * @param <E>         The type of the error value used in the {@link Result}.
     * @param opt         The {@link Option} to convert. Must not be null.
     * @param errorIfNone The error value to return if the {@link Option} is empty.
     * @return A {@link Result} that wraps the value from the {@link Option} if it is defined,
     * or an error containing {@code errorIfNone} if the {@link Option} is empty.
     */
    static <V, E> Result<V, E> fromOption(Option<? extends V> opt, E errorIfNone) {
        Objects.requireNonNull(opt, "opt");
        return switch (opt) {
            case Option.Some<? extends V> s -> Result.ok(s.value());
            case Option.None<? extends V> _ -> Result.err(errorIfNone);
        };
    }

    /**
     * Converts a Try instance into a Result instance.
     *
     * @param <V> the type of the value contained in the Try instance
     * @param t   the Try instance to be converted; must not be null
     * @return a Result instance that represents either the success or failure of the given Try instance
     */
    static <V> Result<V, Throwable> fromTry(Try<V> t) {
        Objects.requireNonNull(t, "t");
        return t.toResult();
    }

    /**
     * Converts a {@link Optional} into a {@link Result}.
     * If the {@code Optional} contains a value, returns {@code Ok} with that value.
     * If the {@code Optional} is empty, returns {@code Err} with a {@link NoSuchElementException}.
     *
     * @param <V>      the value type
     * @param optional the {@code Optional} to convert; must not be {@code null}
     * @return {@code Ok(value)} if the {@code Optional} is present,
     *         or {@code Err(NoSuchElementException)} if empty
     * @throws NullPointerException if {@code optional} is {@code null}
     */
    static <V> Result<V, NoSuchElementException> fromOptional(Optional<? extends V> optional) {
        Objects.requireNonNull(optional, "optional");
        return optional.isPresent()
            ? Result.ok(optional.get())
            : Result.err(new NoSuchElementException("Optional is empty"));
    }

    // ---------- sequence / traverse ----------

    /**
     * Transforms an iterable of {@code Result<V, E>} into a single {@code Result<List<V>, E>}.
     * If any element is an {@code Err}, that error is returned immediately (fail-fast).
     * If all elements are {@code Ok}, returns {@code Ok} containing an unmodifiable list of values
     * in encounter order.
     *
     * @param <V>     the value type
     * @param <E>     the error type
     * @param results the iterable of results; must not be {@code null} and must not contain {@code null} elements
     * @return {@code Ok(List<V>)} if all elements are {@code Ok}, or the first {@code Err} encountered
     * @throws NullPointerException if {@code results} is {@code null} or contains a {@code null} element
     */
    static <V, E> Result<List<V>, E> sequence(Iterable<Result<V, E>> results) {
        Objects.requireNonNull(results, "results");
        ArrayList<V> out = new ArrayList<>();
        for (Result<V, E> r : results) {
            if (r == null) {
                throw new NullPointerException("results contains null element");
            }
            if (r instanceof Err<V, E> err) {
                return Result.err(err.error());
            }
            out.add(((Ok<V, E>) r).value());
        }
        return Result.ok(List.copyOf(out));
    }

    /**
     * Transforms a stream of {@code Result<V, E>} into a single {@code Result<List<V>, E>}.
     * If any element is an {@code Err}, that error is returned immediately (fail-fast) and the
     * stream is closed. If all elements are {@code Ok}, returns {@code Ok} containing an
     * unmodifiable list of values in encounter order.
     *
     * @param <V>     the value type
     * @param <E>     the error type
     * @param results the stream of results; must not be {@code null} and must not contain {@code null} elements
     * @return {@code Ok(List<V>)} if all elements are {@code Ok}, or the first {@code Err} encountered
     * @throws NullPointerException if {@code results} is {@code null} or contains a {@code null} element
     */
    static <V, E> Result<List<V>, E> sequence(Stream<Result<V, E>> results) {
        Objects.requireNonNull(results, "results");
        ArrayList<V> out = new ArrayList<>();
        try (results) {
            Iterator<Result<V, E>> it = results.iterator();
            while (it.hasNext()) {
                Result<V, E> r = it.next();
                if (r == null) {
                    throw new NullPointerException("results contains null element");
                }
                if (r instanceof Err<V, E> err) {
                    return Result.err(err.error());
                }
                out.add(((Ok<V, E>) r).value());
            }
        }
        return Result.ok(List.copyOf(out));
    }

    /**
     * Maps each element of an iterable through {@code mapper} and collects the results into a
     * {@code Result<List<B>, E>}. Fails fast on the first {@code Err} returned by the mapper.
     *
     * @param <A>    the input element type
     * @param <B>    the mapped value type
     * @param <E>    the error type
     * @param values the iterable of input values; must not be {@code null}
     * @param mapper a function that maps each value to a {@code Result}; must not be {@code null}
     *               and must not return {@code null}
     * @return {@code Ok(List<B>)} if all mappings succeed, or the first {@code Err} produced by the mapper
     * @throws NullPointerException if {@code values} or {@code mapper} is {@code null},
     *                              or if the mapper returns {@code null}
     */
    static <A, B, E> Result<List<B>, E> traverse(Iterable<A> values, Function<? super A, Result<B, E>> mapper) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mapper, "mapper");
        ArrayList<B> out = new ArrayList<>();
        for (A a : values) {
            Result<B, E> r = Objects.requireNonNull(mapper.apply(a), "traverse mapper must not return null");
            if (r instanceof Err<B, E> err) {
                return Result.err(err.error());
            }
            out.add(((Ok<B, E>) r).value());
        }
        return Result.ok(List.copyOf(out));
    }

    /**
     * Maps each element of a stream through {@code mapper} and collects the results into a
     * {@code Result<List<B>, E>}. Fails fast on the first {@code Err} returned by the mapper;
     * the stream is closed in all cases.
     *
     * @param <A>    the input element type
     * @param <B>    the mapped value type
     * @param <E>    the error type
     * @param values the stream of input values; must not be {@code null}
     * @param mapper a function that maps each value to a {@code Result}; must not be {@code null}
     *               and must not return {@code null}
     * @return {@code Ok(List<B>)} if all mappings succeed, or the first {@code Err} produced by the mapper
     * @throws NullPointerException if {@code values} or {@code mapper} is {@code null},
     *                              or if the mapper returns {@code null}
     */
    static <A, B, E> Result<List<B>, E> traverse(Stream<A> values, Function<? super A, Result<B, E>> mapper) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mapper, "mapper");
        ArrayList<B> out = new ArrayList<>();
        try (values) {
            Iterator<A> it = values.iterator();
            while (it.hasNext()) {
                A a = it.next();
                Result<B, E> r = Objects.requireNonNull(mapper.apply(a), "traverse mapper must not return null");
                if (r instanceof Err<B, E> err) {
                    return Result.err(err.error());
                }
                out.add(((Ok<B, E>) r).value());
            }
        }
        return Result.ok(List.copyOf(out));
    }

    // ---------- Collectors ----------

    /**
     * Returns a {@link Collector} that accumulates a {@code Stream<Result<V, E>>} into a single
     * {@code Result<List<V>, E>}.
     *
     * <p><strong>Note:</strong> this Collector is <em>not</em> fail-fast. Because the Java
     * {@link Collector} API always feeds every stream element to the accumulator before the
     * finisher runs, <strong>all elements are always consumed</strong> from the stream regardless
     * of whether any {@code Err} is present. The finisher then scans the accumulated list and
     * returns the first {@code Err} found, or {@code Ok} with an unmodifiable list of values in
     * encounter order if none exists. For true fail-fast / short-circuit behaviour use
     * {@link #sequence(Stream)} instead.
     *
     * <p>Example:
     * <pre>{@code
     * Result<List<Integer>, String> r =
     *     Stream.of(Result.ok(1), Result.ok(2), Result.ok(3))
     *           .collect(Result.toList());
     * }</pre>
     *
     * @param <V> the value type
     * @param <E> the error type
     * @return a collector that consumes all stream elements and produces {@code Ok(List<V>)} if
     *         every element is {@code Ok}, or the first {@code Err} found in encounter order
     * @throws NullPointerException if the stream contains a {@code null} element
     */
    static <V, E> Collector<Result<V, E>, ?, Result<List<V>, E>> toList() {
        return Collector.of(
            () -> new ArrayList<Result<V, E>>(),
            List::add,
            (a, b) -> { a.addAll(b); return a; },
            list -> {
                List<V> values = new ArrayList<>(list.size());
                for (Result<V, E> r : list) {
                    if (r == null) throw new NullPointerException("toList stream contains a null element");
                    if (r instanceof Err<V, E> err) return Result.err(err.error());
                    values.add(((Ok<V, E>) r).value());
                }
                return Result.ok(Collections.unmodifiableList(values));
            }
        );
    }

    /**
     * Returns a {@link Collector} that partitions a {@code Stream<Result<V, E>>} into two typed
     * lists: {@link Partition#oks()} for values from {@code Ok} elements, and
     * {@link Partition#errors()} for errors from {@code Err} elements. Both lists are unmodifiable
     * and maintain encounter order.
     *
     * <p>Example:
     * <pre>{@code
     * Result.Partition<Integer, String> p =
     *     Stream.of(Result.ok(1), Result.err("bad"), Result.ok(3))
     *           .collect(Result.partitioningBy());
     * // p.oks()    == [1, 3]
     * // p.errors() == ["bad"]
     * }</pre>
     *
     * @param <V> the value type of the {@code Ok} elements
     * @param <E> the error type of the {@code Err} elements
     * @return a collector producing a {@link Partition} of ok-values and errors
     * @throws NullPointerException if the stream contains a {@code null} element
     */
    static <V, E> Collector<Result<V, E>, ?, Partition<V, E>> partitioningBy() {
        class Acc {
            final ArrayList<V> oks = new ArrayList<>();
            final ArrayList<E> errors = new ArrayList<>();
        }
        return Collector.of(
            Acc::new,
            (acc, r) -> {
                if (r == null) throw new NullPointerException("partitioningBy stream contains a null element");
                if (r instanceof Ok<V, E> ok) acc.oks.add(ok.value());
                else acc.errors.add(((Err<V, E>) r).error());
            },
            (a, b) -> { a.oks.addAll(b.oks); a.errors.addAll(b.errors); return a; },
            acc -> new Partition<>(acc.oks, acc.errors)
        );
    }

}
