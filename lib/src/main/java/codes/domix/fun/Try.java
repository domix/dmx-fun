package codes.domix.fun;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A monadic type that represents a computation that may either result in a value
 * (Success) or throw an exception (Failure).
 *
 * @param <Value> the type of the successful value
 */
public sealed interface Try<Value> permits Try.Success, Try.Failure {

    /**
     * Represents a successful computation containing a value.
     */
    record Success<Value>(Value value) implements Try<Value> {
    }

    /**
     * Represents a failed computation containing the cause (Throwable).
     */
    record Failure<Value>(Throwable cause) implements Try<Value> {
    }

    /**
     * Functional interface for suppliers that can throw checked exceptions.
     */
    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Functional interface for runnables that can throw checked exceptions.
     */
    @FunctionalInterface
    interface CheckedRunnable {
        void run() throws Exception;
    }

    /**
     * Creates a successful Try with the given value.
     */
    static <V> Try<V> success(V value) {
        return new Success<>(value);
    }

    /**
     * Creates a failed Try with the given throwable as cause.
     */
    static <V> Try<V> failure(Throwable cause) {
        return new Failure<>(cause);
    }

    /**
     * Wraps a computation that may throw into a Try.
     */
    static <V> Try<V> of(CheckedSupplier<? extends V> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        } catch (Throwable t) {
            // Errors are also captured, but kept as-is.
            return failure(t);
        }
    }

    /**
     * Wraps a side-effect that may throw into a Try&lt;Void&gt;.
     */
    static Try<Void> run(CheckedRunnable runnable) {
        try {
            runnable.run();
            return success(null);
        } catch (Exception e) {
            return failure(e);
        } catch (Throwable t) {
            return failure(t);
        }
    }

    /**
     * @return true if this Try is a Success.
     */
    default boolean isSuccess() {
        return this instanceof Success<?>;
    }

    /**
     * @return true if this Try is a Failure.
     */
    default boolean isFailure() {
        return this instanceof Failure<?>;
    }

    /**
     * Returns the successful value or throws if this is a Failure.
     */
    default Value get() {
        return switch (this) {
            case Success<Value> s -> s.value();
            case Failure<Value> f -> throw new NoSuchElementException(
                "No value present. Try is a Failure.", f.cause()
            );
        };
    }

    /**
     * Returns the failure cause or throws if this is a Success.
     */
    default Throwable getCause() {
        return switch (this) {
            case Failure<Value> f -> f.cause();
            case Success<Value> s -> throw new NoSuchElementException(
                "No cause present. Try is a Success for value: " + s.value()
            );
        };
    }

    /**
     * Maps the successful value if this is a Success.
     * If mapper throws, the result becomes a Failure.
     */
    default <NewValue> Try<NewValue> map(Function<? super Value, ? extends NewValue> mapper) {
        return switch (this) {
            case Success<Value> s -> {
                try {
                    yield success(mapper.apply(s.value()));
                } catch (Exception e) {
                    yield failure(e);
                } catch (Throwable t) {
                    yield failure(t);
                }
            }
            case Failure<Value> f -> failure(f.cause());
        };
    }

    /**
     * Flat-maps the successful value if this is a Success.
     * If mapper throws, the result becomes a Failure.
     */
    default <NewValue> Try<NewValue> flatMap(Function<? super Value, Try<NewValue>> mapper) {
        return switch (this) {
            case Success<Value> s -> {
                try {
                    yield mapper.apply(s.value());
                } catch (Exception e) {
                    yield failure(e);
                } catch (Throwable t) {
                    yield failure(t);
                }
            }
            case Failure<Value> f -> failure(f.cause());
        };
    }

    /**
     * Executes the given action if this is a Success.
     */
    default Try<Value> onSuccess(Consumer<? super Value> action) {
        if (this instanceof Success<Value>(Value value)) {
            action.accept(value);
        }
        return this;
    }

    /**
     * Executes the given action if this is a Failure.
     */
    default Try<Value> onFailure(Consumer<? super Throwable> action) {
        if (this instanceof Failure<Value>(Throwable cause)) {
            action.accept(cause);
        }
        return this;
    }

    /**
     * Recovers from a Failure by mapping the cause to an alternative value.
     */
    default Try<Value> recover(Function<? super Throwable, ? extends Value> recoverFn) {
        return switch (this) {
            case Success<Value> s -> this;
            case Failure<Value> f -> {
                try {
                    yield success(recoverFn.apply(f.cause()));
                } catch (Exception e) {
                    yield failure(e);
                } catch (Throwable t) {
                    yield failure(t);
                }
            }
        };
    }

    /**
     * Recovers from a Failure by mapping the cause to another Try.
     */
    default Try<Value> recoverWith(Function<? super Throwable, Try<Value>> recoverFn) {
        return switch (this) {
            case Success<Value> s -> this;
            case Failure<Value> f -> {
                try {
                    yield recoverFn.apply(f.cause());
                } catch (Exception e) {
                    yield failure(e);
                } catch (Throwable t) {
                    yield failure(t);
                }
            }
        };
    }

    /**
     * Returns the successful value or the provided fallback if this is a Failure.
     */
    default Value getOrElse(Value fallback) {
        return this instanceof Success<Value>(Value value) ? value : fallback;
    }

    /**
     * Returns the successful value or the value from the supplier if this is a Failure.
     */
    default Value getOrElseGet(Supplier<? extends Value> fallbackSupplier) {
        return this instanceof Success<Value>(Value value) ? value : fallbackSupplier.get();
    }

    /**
     * Returns the value if Success or null if Failure.
     */
    default Value getOrNull() {
        return this instanceof Success<Value>(Value value) ? value : null;
    }

    /**
     * Returns the value if Success; if Failure, throws either the original cause (if it is an Exception)
     * or wraps it in a RuntimeException.
     */
    default Value getOrThrow() throws Exception {
        return switch (this) {
            case Success<Value> s -> s.value();
            case Failure<Value> f -> {
                Throwable cause = f.cause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                throw new RuntimeException(cause);
            }
        };
    }

    /**
     * Returns the value if Success; if Failure, throws the exception produced
     * by the given mapper.
     */
    default Value getOrThrow(Function<? super Throwable, ? extends RuntimeException> exceptionMapper) {
        return switch (this) {
            case Success<Value> s -> s.value();
            case Failure<Value> f -> throw exceptionMapper.apply(f.cause());
        };
    }

    /**
     * Folds this Try into a single value by applying the corresponding function
     * depending on whether it is a Success or a Failure.
     */
    default <Folded> Folded fold(
        Function<? super Value, ? extends Folded> onSuccess,
        Function<? super Throwable, ? extends Folded> onFailure
    ) {
        return switch (this) {
            case Success<Value> s -> onSuccess.apply(s.value());
            case Failure<Value> f -> onFailure.apply(f.cause());
        };
    }

    /**
     * Converts this Try into a Result&lt;Value, Throwable&gt; using your existing Result monad.
     */
    default Result<Value, Throwable> toResult() {
        return switch (this) {
            case Success<Value> s -> Result.ok(s.value());
            case Failure<Value> f -> Result.err(f.cause());
        };
    }

    /**
     * Creates a Try from a Result&lt;Value, Throwable&gt;.
     */
    static <V> Try<V> fromResult(Result<V, ? extends Throwable> result) {
        if (result.isOk()) {
            return Try.success(result.get());
        }
        return Try.failure(result.getError());
    }
}
