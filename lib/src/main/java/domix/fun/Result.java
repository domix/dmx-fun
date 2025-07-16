package domix.fun;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public sealed interface Result<Value, Error> permits Result.Ok, Result.Err {
    record Ok<Value, Error>(Value value) implements Result<Value, Error> {
    }

    record Err<Value, Error>(Error error) implements Result<Value, Error> {
    }

    static <Value, Error> Result<Value, Error> ok(Value value) {
        return new Ok<>(value);
    }

    static <Value, Error> Result<Value, Error> err(Error error) {
        return new Err<>(error);
    }

    default boolean isError() {
        return this instanceof Err;
    }

    default boolean isOk() {
        return this instanceof Ok;
    }

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

    default <NewValue> Result<NewValue, Error> map(Function<Value, NewValue> mapper) {
        return switch (this) {
            case Ok<Value, Error> ok -> Result.ok(mapper.apply(ok.value()));
            case Err<Value, Error> err -> Result.err(err.error());
        };
    }

    default <NewError> Result<Value, NewError> mapError(Function<Error, NewError> mapper) {
        return switch (this) {
            case Ok<Value, Error> ok -> Result.ok(ok.value);
            case Err<Value, Error> err -> Result.err(mapper.apply(err.error));
        };
    }

    default <NewValue> Result<NewValue, Error> flatMap(Function<Value, Result<NewValue, Error>> mapper) {
        return switch (this) {
            case Ok<Value, Error> ok -> mapper.apply(ok.value());
            case Err<Value, Error> err -> Result.err(err.error());
        };
    }

    default void match(Consumer<Value> onSuccess, Consumer<Error> onError) {
        switch (this) {
            case Ok<Value, Error> ok -> onSuccess.accept(ok.value());
            case Err<Value, Error> err -> onError.accept(err.error());
        }
    }

    default Result<Value, Error> peek(Consumer<Value> action) {
        if (this instanceof Ok<Value, Error>(Value value)) {
            action.accept(value);
        }
        return this;
    }

    default Result<Value, Error> peekError(Consumer<Error> action) {
        if (this instanceof Err<Value, Error>(Error error)) {
            action.accept(error);
        }
        return this;
    }

    default Value getOrElse(Value fallback) {
        return this instanceof Ok<Value, Error>(Value value) ? value : fallback;
    }

    default Value getOrElseGet(Supplier<Value> fallbackSupplier) {
        return this instanceof Ok<Value, Error>(Value value) ? value : fallbackSupplier.get();
    }

    default Value getOrThrow(Function<Error, ? extends RuntimeException> exceptionMapper) {
        return switch (this) {
            case Ok<Value, Error> ok -> ok.value();
            case Err<Value, Error> err -> throw exceptionMapper.apply(err.error());
        };
    }

    default Value getOrNull() {
        return this instanceof Ok<Value, Error>(Value value) ? value : null;
    }

    default <Folded> Folded fold(Function<Value, Folded> onSuccess, Function<Error, Folded> onError) {
        return switch (this) {
            case Ok<Value, Error> ok -> onSuccess.apply(ok.value());
            case Err<Value, Error> err -> onError.apply(err.error());
        };
    }

    default Result<Value, Error> filter(Predicate<Value> predicate, Error errorIfFalse) {
        if (this instanceof Ok<Value, Error>(Value value)) {
            return predicate.test(value) ? this : Result.err(errorIfFalse);
        }
        return this;
    }

    default Result<Value, Error> filter(Predicate<Value> predicate, Function<Value, Error> errorIfFalse) {
        if (this instanceof Ok<Value, Error>(Value value)) {
            return predicate.test(value) ? this : Result.err(errorIfFalse.apply(value));
        }
        return this;
    }


}
