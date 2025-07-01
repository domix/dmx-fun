package domix.fun;

import java.util.function.Function;

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

    default <NewValue> Result<NewValue, Error> map(Function<Value, NewValue> mapper) {
        return switch (this) {
            case Ok<Value, Error> ok -> Result.ok(mapper.apply(ok.value));
            case Err<Value, Error> err -> Result.err(err.error);
        };
    }

    default <NewValue, NewError> Result<NewValue, Error> flatMap(Function<Value, Result<NewValue, Error>> mapper) {
        return switch (this) {
            case Ok<Value, Error> ok -> mapper.apply(ok.value);
            case Err<Value, Error> err -> Result.err(err.error);
        };
    }

    default <NewError> Result<Value, NewError> mapError(Function<Error, NewError> mapper) {
        return switch (this) {
            case Ok<Value, Error> ok -> Result.ok(ok.value);
            case Err<Value, Error> err -> Result.err(mapper.apply(err.error));
        };
    }


}
