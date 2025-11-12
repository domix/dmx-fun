package codes.domix.fun;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * A generic sealed interface representing a result type that can either be a successful value
 * ({@link Result.Ok}) or an error ({@link Result.Err}).
 * This is a common functional programming construct that allows for
 * handling both success and error cases in a unified type.
 *
 * @param <Value> the type of the value contained in a successful result
 * @param <Error> the type of the error contained in an erroneous result
 */
public sealed interface Result<Value, Error> permits Result.Ok, Result.Err {
    /**
     * Represents a successful result containing a value of type {@code Value}.
     * <p>
     * This record implements the {@link Result} interface and indicates the "Ok" variant
     * of a result. It encapsulates a value that represents a successful computation or action.
     *
     * @param <Value> the type of the value contained in the successful result
     * @param <Error> the type of the error contained in the erroneous result, unused here
     * @param value   the value that represents the successful result
     */
    record Ok<Value, Error>(Value value) implements Result<Value, Error> {
    }

    /**
     * Represents an erroneous result containing an error of type {@code Error}.
     * <p>
     * This record implements the {@link Result} interface and represents the "Err" variant
     * of a result. It encapsulates an error value that indicates a failed computation or action.
     *
     * @param <Value> the type of the value contained in a successful result, unused here
     * @param <Error> the type of the error contained in the erroneous result
     * @param error   the error value that represents the erroneous result
     */
    record Err<Value, Error>(Error error) implements Result<Value, Error> {
    }

    /**
     * Creates a {@link Result} instance representing a successful result with the given value.
     * This method is used to indicate that a computation or action has succeeded.
     *
     * @param <Value> the type of the value contained in the successful result
     * @param <Error> the type of the error that would be contained in an erroneous result
     * @param value   the value to encapsulate in the successful result
     * @return a {@link Result} instance of type {@code Ok<Value, Error>} containing the provided value
     */
    static <Value, Error> Result<Value, Error> ok(Value value) {
        return new Ok<>(value);
    }

    /**
     * Creates a {@link Result} instance representing an erroneous result with the given error value.
     * This method is used to indicate that a computation or action has failed.
     *
     * @param <Value> the type of the value that would be contained in a successful result, unused here
     * @param <Error> the type of the error contained in the erroneous result
     * @param error   the error value to encapsulate in the erroneous result
     * @return a {@link Result} instance of type {@code Err<Value, Error>} containing the provided error
     */
    static <Value, Error> Result<Value, Error> err(Error error) {
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
     * @return the value contained in the successful result
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
            case Ok<Value, Error> ok -> Result.ok(ok.value);
            case Err<Value, Error> err -> Result.err(mapper.apply(err.error));
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
        return this instanceof Ok<Value, Error>(Value value) ? value : fallbackSupplier.get();
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
    default Value getOrNull() {
        return this instanceof Ok<Value, Error>(Value value) ? value : null;
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
}
