package codes.domix.fun;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


/**
 * A monadic type that represents a computation that may either result in a value
 * (Success) or throw an exception (Failure).
 *
 * <p>This interface is {@link NullMarked}: all types are non-null by default.
 * The only nullable return value is {@link #getOrNull()}, which explicitly signals
 * the absence of a value on failure.
 *
 * @param <Value> the type of the successful value
 */
@NullMarked
public sealed interface Try<Value> permits Try.Success, Try.Failure {

    /**
     * Represents a successful computation result within the Try monad pattern.
     * Encapsulates a value of type {@code Value}, indicating the computation succeeded
     * without any errors or exceptions.
     *
     * @param value   the successful result of the computation
     * @param <Value> the type of the value being encapsulated
     */
    record Success<Value>(Value value) implements Try<Value> {
    }

    /**
     * Represents a computational failure within a context that implements the
     * {@link Try} interface. This class encapsulates a {@link Throwable} indicating
     * the cause of the failure.
     *
     * @param cause   the exception that caused the failure; must not be null
     * @param <Value> the type of the value that would have been produced if the
     *                computation had succeeded
     */
    record Failure<Value>(Throwable cause) implements Try<Value> {
        /**
         * Compact canonical constructor — validates that {@code cause} is non-null.
         *
         * @throws NullPointerException if {@code cause} is {@code null}
         */
        public Failure {
            Objects.requireNonNull(cause, "Failure cause must not be null");
        }
    }

    /**
     * A functional interface representing a supplier that can produce a value and may throw a checked exception.
     * This is a variation of the standard {@code Supplier} interface, intended for use in scenarios where
     * the operation may involve checked exceptions.
     *
     * @param <T> the type of result supplied by this supplier
     */
    @FunctionalInterface
    interface CheckedSupplier<T> {
        /**
         * Retrieves and returns a value, potentially throwing a checked exception during execution.
         *
         * @return the value retrieved by this method
         * @throws Exception if an error occurs during the retrieval
         */
        T get() throws Exception;
    }

    /**
     * Functional interface for runnables that can throw checked exceptions.
     */
    @FunctionalInterface
    interface CheckedRunnable {
        /**
         * Executes an operation that may throw a checked exception.
         * <p>
         * This method is intended to provide a functional interface for use
         * in scenarios where a runnable operation might need to handle checked exceptions.
         *
         * @throws Exception if an error occurs during execution
         */
        void run() throws Exception;
    }

    /**
     * Creates a successful {@code Try} instance containing the provided value.
     *
     * @param <V>   the type of the value contained in the {@code Try} instance
     * @param value the value to be wrapped in a successful {@code Try} instance
     * @return a {@code Try} instance representing a successful computation containing the given value
     */
    static <V> Try<V> success(V value) {
        return new Success<>(value);
    }

    /**
     * Creates a {@code Try} instance representing a failure with the given cause.
     *
     * @param <V>   the type of the value that would have been returned in case of success
     * @param cause the throwable that caused the failure; must not be null
     * @return a {@code Try} instance representing the failure
     */
    static <V> Try<V> failure(Throwable cause) {
        return new Failure<>(cause);
    }

    /**
     * Creates a {@code Try} instance by executing the given {@code CheckedSupplier}.
     * If the supplier executes successfully, a {@code Try} containing the result is returned.
     * If an exception is thrown during execution, a {@code Try} containing the throwable is returned.
     *
     * @param supplier the {@code CheckedSupplier} to execute
     * @param <V>      the type of the result supplied by the {@code CheckedSupplier}
     * @return a {@code Try} instance representing either a success with the supplied value
     * or a failure with the thrown exception
     */
    static <V> Try<V> of(CheckedSupplier<? extends V> supplier) {
        try {
            return success(supplier.get());
        } catch (Throwable t) {
            return failure(t);
        }
    }

    /**
     * Executes the provided {@code CheckedRunnable} and returns a {@code Try} instance
     * representing the outcome of the execution.
     *
     * <p><b>Note on null value:</b> On success the returned {@code Try<Void>} wraps {@code null}
     * as its value (since {@code Void} has no instances). This means that converting the result
     * via {@link #toOption()} will always return {@link Option#none()} — use {@link #isSuccess()}
     * or {@link #fold(java.util.function.Function, java.util.function.Function) fold()} instead
     * to observe the outcome of a {@code run()} call.
     *
     * @param runnable the {@code CheckedRunnable} to be executed
     * @return a {@code Try<Void>} representing the success or failure of the execution.
     * On success, its value is {@code null}; on failure, it contains the thrown exception.
     */
    static Try<Void> run(CheckedRunnable runnable) {
        try {
            runnable.run();
            return success(null);
        } catch (Throwable t) {
            return failure(t);
        }
    }

    /**
     * Determines if the current instance represents a successful state.
     *
     * @return true if the current instance is of type Success, false otherwise
     */
    default boolean isSuccess() {
        return this instanceof Success<?>;
    }

    /**
     * Determines if the current instance represents a failure state.
     *
     * @return {@code true} if the current instance is of type {@code Failure}, otherwise {@code false}.
     */
    default boolean isFailure() {
        return this instanceof Failure<?>;
    }

    /**
     * Retrieves the value encapsulated in this instance if it represents a success.
     * Throws a NoSuchElementException if this instance represents a failure.
     *
     * @return the value of this instance if it is a success
     * @throws NoSuchElementException if this instance is a failure
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
     * Retrieves the cause of failure if the instance represents a failed state.
     * For a successful state, this method throws a NoSuchElementException.
     *
     * @return the throwable cause of the failure if the instance is a Failure
     * @throws NoSuchElementException if the instance is a Success as there is no cause
     */
    default Throwable getCause() {
        return switch (this) {
            case Failure<Value> f -> f.cause();
            case Success<Value> _ -> throw new NoSuchElementException(
                "No cause present. Try is a Success."
            );
        };
    }

    /**
     * Transforms the value held by this instance using the provided mapping function.
     * If this instance represents a successful result, the mapping function is applied
     * to its value. If this instance represents a failure, the failure is propagated.
     *
     * @param <NewValue> the type of the resulting value after applying the mapping function
     * @param mapper     the function to apply to the value if this instance represents a success
     * @return a new instance of {@code Try} containing the mapped value if successful,
     * or the original failure if unsuccessful
     */
    default <NewValue> Try<NewValue> map(Function<? super Value, ? extends NewValue> mapper) {
        return switch (this) {
            case Success<Value> s -> {
                try {
                    yield success(mapper.apply(s.value()));
                } catch (Throwable t) {
                    yield failure(t);
                }
            }
            case Failure<Value> f -> failure(f.cause());
        };
    }

    /**
     * Applies the provided mapping function to the value contained within this `Try` instance
     * if it represents a successful outcome. The mapping function may produce a new `Try` instance
     * representing either success or failure. If this instance is already a failure, it
     * will return a failure with the same cause.
     *
     * @param <NewValue> the type of the result contained in the new `Try` instance
     * @param mapper     the mapping function to apply to the value, which produces a `Try` instance
     * @return a new `Try` instance produced by applying the mapping function to the value,
     * or a failure if either this instance is a failure or the function throws an exception
     */
    default <NewValue> Try<NewValue> flatMap(Function<? super Value, Try<NewValue>> mapper) {
        return switch (this) {
            case Success<Value> s -> {
                try {
                    yield mapper.apply(s.value());
                } catch (Throwable t) {
                    yield failure(t);
                }
            }
            case Failure<Value> f -> failure(f.cause());
        };
    }

    /**
     * Performs the given action if the current instance represents a successful outcome.
     *
     * @param action a {@code Consumer} to be executed with the value of a successful outcome
     * @return the current {@code Try} instance
     */
    default Try<Value> onSuccess(Consumer<? super Value> action) {
        if (this instanceof Success<Value>(Value value)) {
            action.accept(value);
        }
        return this;
    }

    /**
     * Executes a specified action if this instance represents a failure.
     *
     * @param action the action to be executed, accepting the throwable cause of the failure
     * @return the current instance of {@code Try<Value>}
     */
    default Try<Value> onFailure(Consumer<? super Throwable> action) {
        if (this instanceof Failure<Value>(Throwable cause)) {
            action.accept(cause);
        }
        return this;
    }

    /**
     * Recovers from a failure by applying a recovery function to the underlying throwable.
     * If the current instance represents a success, it is returned as is.
     * Otherwise, the recovery function is applied to the cause of the failure to produce a new success value.
     * If the recovery function itself throws an exception, the method returns a new failure wrapping the thrown exception.
     *
     * @param recoverFn the recovery function to apply in case of failure, accepting the throwable cause of the failure
     *                  and producing a new value
     * @return a {@code Try} instance representing either the original success, a new success generated by applying
     * the recovery function, or a new failure resulting from an exception thrown by the recovery function
     */
    default Try<Value> recover(Function<? super Throwable, ? extends Value> recoverFn) {
        Objects.requireNonNull(recoverFn, "recoverFn");
        return switch (this) {
            case Success<Value> s -> this;
            case Failure<Value> f -> {
                try {
                    yield success(recoverFn.apply(f.cause()));
                } catch (Throwable t) {
                    yield failure(t);
                }
            }
        };
    }

    /**
     * Recovers from a failure by applying the given recovery function to the cause of the failure.
     *
     * @param recoverFn the function that takes a throwable and returns a new {@code Try} instance
     *                  for recovery. It is applied only in case of a failure.
     * @return the original success instance if this is a success, or the result of the recovery
     * function if this is a failure.
     */
    default Try<Value> recoverWith(Function<? super Throwable, Try<Value>> recoverFn) {
        Objects.requireNonNull(recoverFn, "recoverFn");
        return switch (this) {
            case Success<Value> _ -> this;
            case Failure<Value> f -> {
                try {
                    Try<Value> recovered = recoverFn.apply(f.cause());
                    yield Objects.requireNonNull(recovered, "recoverFn returned null");
                } catch (Throwable t) {
                    yield failure(t);
                }
            }
        };
    }

    /**
     * Returns the value if this instance is a {@code Success}, or the specified fallback value if it is a {@code Failure}.
     *
     * @param fallback the value to return if this instance is a {@code Failure}.
     * @return the value of this instance if it is a {@code Success}, or the specified fallback value if it is a {@code Failure}.
     */
    default Value getOrElse(Value fallback) {
        return this instanceof Success<Value>(Value value) ? value : fallback;
    }

    /**
     * Returns the value of this {@code Try} if it is a {@code Success}, or retrieves a fallback
     * value using the provided {@code fallbackSupplier} if it is a {@code Failure}.
     *
     * @param fallbackSupplier a {@code Supplier} that provides an alternative value to return
     *                         if this {@code Try} is a {@code Failure}.
     * @return the value of this {@code Try} if it is a {@code Success}, or the value supplied
     * by {@code fallbackSupplier} if it is a {@code Failure}.
     */
    default Value getOrElseGet(Supplier<? extends Value> fallbackSupplier) {
        Objects.requireNonNull(fallbackSupplier, "fallbackSupplier");
        return this instanceof Success<Value>(Value value) ? value : fallbackSupplier.get();
    }

    /**
     * Returns the value of this {@code Try} if it is a {@code Success}, or {@code null} if it is a {@code Failure}.
     *
     * @return the successful value if this {@code Try} is a {@code Success}, or {@code null} if it is a {@code Failure}.
     */
    default @Nullable Value getOrNull() {
        return this instanceof Success<Value>(Value value) ? value : null;
    }

    /**
     * Returns the successful value if this instance is a {@code Success}, or throws an exception
     * if this instance is a {@code Failure}.
     *
     * @return the successful value of this {@code Try} if it is a {@code Success}.
     * @throws Exception        if this {@code Try} is a {@code Failure} and the failure cause is an {@code Exception}.
     * @throws RuntimeException if this {@code Try} is a {@code Failure} and the failure cause is not an {@code Exception}.
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
     * Returns the value if this instance is a Success, or throws the exception
     * provided by the exceptionMapper if this instance is a Failure.
     *
     * @param exceptionMapper a function to map the failure cause (Throwable)
     *                        to a RuntimeException that will be thrown.
     * @return the value if this is a Success.
     * @throws RuntimeException if this is a Failure and the exceptionMapper is invoked.
     */
    default Value getOrThrow(Function<? super Throwable, ? extends RuntimeException> exceptionMapper) {
        return switch (this) {
            case Success<Value> s -> s.value();
            case Failure<Value> f -> throw exceptionMapper.apply(f.cause());
        };
    }

    /**
     * Folds the current {@code Try} instance into a single value by applying one of two provided functions.
     * If this {@code Try} is a {@code Success}, the {@code onSuccess} function is applied to the value.
     * If this {@code Try} is a {@code Failure}, the {@code onFailure} function is applied to the cause.
     *
     * @param <Folded>  the type of the result after applying one of the functions
     * @param onSuccess a function to apply to the value if this {@code Try} is a {@code Success}
     * @param onFailure a function to apply to the cause if this {@code Try} is a {@code Failure}
     * @return the result of applying the appropriate function based on the state of this {@code Try}
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
     * Filters the Try based on the given predicate.
     * If this is a Success and the predicate evaluates to true, the result is this Try instance.
     * If the predicate evaluates to false, a Failure is returned using an {@link IllegalArgumentException}.
     * If this is already a Failure, the result remains unchanged.
     *
     * @param predicate the condition to evaluate for the value if this is a Success
     * @return a Success if the predicate evaluates to true, or a Failure otherwise
     */
    default Try<Value> filter(Predicate<? super Value> predicate) {
        return filter(predicate, () -> new IllegalArgumentException("Predicate does not hold for value"));
    }

    /**
     * Filters the Try based on the given predicate.
     * If this is a Success and the predicate evaluates to true, the result is this Try instance.
     * If the predicate evaluates to false, a Failure is returned using the provided throwable supplier.
     * If this is already a Failure, the result remains unchanged.
     *
     * @param predicate         the condition to evaluate for the value if this is a Success
     * @param throwableSupplier a supplier to provide the throwable for the Failure if the predicate evaluates to false
     * @return a Success if the predicate evaluates to true, or a Failure otherwise
     */
    default Try<Value> filter(Predicate<? super Value> predicate, Supplier<? extends Throwable> throwableSupplier) {
        return switch (this) {
            case Success<Value> s -> {
                try {
                    yield predicate.test(s.value()) ? this : failure(throwableSupplier.get());
                } catch (Throwable t) {
                    yield failure(t);
                }
            }
            case Failure<Value> _ -> this;
        };
    }

    /**
     * Filters this {@code Try} using a predicate and a contextual error function that receives
     * the value that failed the test. If this is a {@code Failure}, it is returned unchanged.
     * If this is a {@code Success} and the predicate returns {@code false}, a {@code Failure}
     * is returned with the throwable produced by {@code errorFn} applied to the value.
     *
     * <p>Unlike {@link #filter(Predicate, Supplier)}, the error function can produce a message
     * that includes the offending value:
     * <pre>{@code
     * Try.of(() -> parseAge(input))
     *    .filter(age -> age >= 0, age -> new IllegalArgumentException("negative age: " + age));
     * }</pre>
     *
     * @param predicate the condition to test the value; must not be {@code null}
     * @param errorFn   a function that produces the throwable when the predicate fails;
     *                  must not be {@code null} and must not return {@code null}
     * @return this instance if the predicate holds or this is already a {@code Failure};
     *         otherwise a new {@code Failure} produced by {@code errorFn}
     * @throws NullPointerException if {@code predicate} or {@code errorFn} is {@code null},
     *                              or if {@code errorFn} returns {@code null}
     */
    default Try<Value> filter(Predicate<? super Value> predicate, Function<? super Value, ? extends Throwable> errorFn) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(errorFn, "errorFn");
        return switch (this) {
            case Success<Value> s -> {
                try {
                    if (predicate.test(s.value())) {
                        yield this;
                    }
                    yield failure(Objects.requireNonNull(errorFn.apply(s.value()), "errorFn returned null"));
                } catch (Throwable t) {
                    yield failure(t);
                }
            }
            case Failure<Value> _ -> this;
        };
    }

    /**
     * Converts the current {@code Try} instance into a {@code Result} representation.
     * If this {@code Try} is a {@code Success}, the resulting {@code Result} will be "ok"
     * with the successful value. If this {@code Try} is a {@code Failure}, the resulting
     * {@code Result} will be "err" with the failure cause.
     *
     * @return a {@code Result} instance where the success value is mapped to "ok"
     * and the failure cause is mapped to "err".
     */
    default Result<Value, Throwable> toResult() {
        return switch (this) {
            case Success<Value> s -> Result.ok(s.value());
            case Failure<Value> f -> Result.err(f.cause());
        };
    }

    /**
     * Converts this {@code Try} into a {@code Result<Value, E>} using a custom error mapper.
     * If this is a {@code Success}, returns {@code Ok} with the same value.
     * If this is a {@code Failure}, applies {@code errorMapper} to the cause and returns {@code Err}.
     *
     * <p>This overload is preferred over {@link #toResult()} when a typed error is needed:
     * <pre>{@code
     * Result<Config, String> r = Try.of(this::loadConfig)
     *     .toResult(e -> "load failed: " + e.getMessage());
     * }</pre>
     *
     * @param <E>         the error type of the resulting {@code Result}
     * @param errorMapper a function that maps the failure cause to the error value; must not return {@code null}
     * @return {@code Ok(value)} on success, or {@code Err(errorMapper.apply(cause))} on failure
     * @throws NullPointerException if {@code errorMapper} is {@code null} or returns {@code null}
     */
    default <E> Result<Value, E> toResult(Function<? super Throwable, ? extends E> errorMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper");
        return switch (this) {
            case Success<Value> s -> Result.ok(s.value());
            case Failure<Value> f -> Result.err(
                Objects.requireNonNull(errorMapper.apply(f.cause()), "errorMapper returned null")
            );
        };
    }

    /**
     * Converts a {@link Result} into a {@link Try} instance.
     * If the {@code Result} is successful (contains a value), a successful {@code Try} is returned.
     * If the {@code Result} contains an error, a failed {@code Try} is returned with the corresponding cause.
     *
     * @param <V>    the type of the successful value
     * @param result the {@code Result} to convert into a {@code Try}
     * @return a {@code Try} representing either the successful value or the failure cause contained in the {@code Result}
     */
    static <V> Try<V> fromResult(Result<V, ? extends Throwable> result) {
        Objects.requireNonNull(result, "result");
        if (result.isOk()) {
            return Try.success(result.get());
        }
        return Try.failure(result.getError());
    }


    /**
     * Transforms the failure cause using the given function, leaving a {@code Success} unchanged.
     * If this is a {@code Failure} and {@code mapper} itself throws, the thrown exception becomes
     * the new cause (mirroring the behaviour of {@link #map} on the success channel).
     *
     * <p>Useful for wrapping low-level exceptions into domain exceptions:
     * <pre>{@code
     * Try.of(db::query)
     *    .mapFailure(e -> new RepositoryException("query failed", e));
     * }</pre>
     *
     * @param mapper a function that maps the current cause to a new {@link Throwable}; must not return {@code null}
     * @return this instance if {@code Success}, or a new {@code Failure} with the mapped cause;
     *         if {@code mapper} returns {@code null} or throws, the exception is wrapped as a new {@code Failure}
     * @throws NullPointerException if {@code mapper} itself is {@code null}
     */
    default Try<Value> mapFailure(Function<? super Throwable, ? extends Throwable> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return switch (this) {
            case Success<Value> _ -> this;
            case Failure<Value> f -> {
                try {
                    yield Try.failure(
                        Objects.requireNonNull(mapper.apply(f.cause()), "mapper returned null")
                    );
                } catch (Throwable t) {
                    yield Try.failure(t);
                }
            }
        };
    }

    /**
     * Returns a single-element {@link Stream} containing the success value, or an empty stream on failure.
     * Mirrors {@link Option#stream()} for consistency.
     *
     * <ul>
     *   <li>{@code Success(v) → Stream.of(v)}</li>
     *   <li>{@code Failure    → Stream.empty()}</li>
     * </ul>
     *
     * @return a stream containing the value if this is a {@code Success}, or an empty stream otherwise
     */
    default Stream<Value> stream() {
        return switch (this) {
            case Success<Value> s -> Stream.of(s.value());
            case Failure<Value> _ -> Stream.empty();
        };
    }

    // ---------- Interoperability: Try <-> Option / Result ----------

    /**
     * Converts the current instance to an Option.
     * If the instance is a Success, wraps its value in an Option.
     * If the instance is a Failure, returns an empty Option.
     *
     * @return an Option containing the value if the instance is a Success,
     * or an empty Option if the instance is a Failure.
     */
    default Option<Value> toOption() {
        return switch (this) {
            case Success<Value> s -> Option.ofNullable(s.value()); // null => None
            case Failure<Value> _ -> Option.none();
        };
    }

    /**
     * Converts an {@link Option} to a {@link Try}. If the {@link Option} contains a value, a successful {@link Try}
     * is returned with that value. Otherwise, a failed {@link Try} is created using the supplied exception.
     *
     * @param <V>               the type of the value contained in the {@link Option}.
     * @param opt               the {@link Option} to be converted into a {@link Try}, must not be null.
     * @param exceptionSupplier the supplier of the exception to be used when the {@link Option} is empty, must not be null.
     * @return a {@link Try} representing either a success containing the value of the {@link Option},
     * or a failure containing the exception provided by the supplier.
     */
    static <V> Try<V> fromOption(Option<? extends V> opt, Supplier<? extends Throwable> exceptionSupplier) {
        Objects.requireNonNull(opt, "opt");
        Objects.requireNonNull(exceptionSupplier, "exceptionSupplier");
        return switch (opt) {
            case Option.Some<? extends V> s -> Try.success(s.value());
            case Option.None<? extends V> _ -> Try.failure(
                Objects.requireNonNull(exceptionSupplier.get(), "exceptionSupplier returned null")
            );
        };
    }

    // ---------- sequence / traverse ----------

    /**
     * Transforms an iterable of {@code Try<V>} into a single {@code Try<List<V>>}.
     * If any element is a {@code Failure}, that failure is returned immediately (fail-fast).
     * If all elements are {@code Success}, returns {@code Success} containing an unmodifiable
     * list of values in encounter order.
     *
     * @param <V>   the value type
     * @param tries the iterable of Try values; must not be {@code null} and must not contain {@code null} elements
     * @return {@code Success(List<V>)} if all elements succeed, or the first {@code Failure} encountered
     * @throws NullPointerException if {@code tries} is {@code null} or contains a {@code null} element
     */
    static <V> Try<List<V>> sequence(Iterable<Try<V>> tries) {
        Objects.requireNonNull(tries, "tries");
        ArrayList<V> out = new ArrayList<>();
        for (Try<V> t : tries) {
            if (t == null) {
                throw new NullPointerException("tries contains null element");
            }
            if (t instanceof Failure<V> f) {
                return Try.failure(f.cause());
            }
            out.add(((Success<V>) t).value());
        }
        return Try.success(List.copyOf(out));
    }

    /**
     * Transforms a stream of {@code Try<V>} into a single {@code Try<List<V>>}.
     * If any element is a {@code Failure}, that failure is returned immediately (fail-fast) and
     * the stream is closed. If all elements are {@code Success}, returns {@code Success} containing
     * an unmodifiable list of values in encounter order.
     *
     * @param <V>   the value type
     * @param tries the stream of Try values; must not be {@code null} and must not contain {@code null} elements
     * @return {@code Success(List<V>)} if all elements succeed, or the first {@code Failure} encountered
     * @throws NullPointerException if {@code tries} is {@code null} or contains a {@code null} element
     */
    static <V> Try<List<V>> sequence(Stream<Try<V>> tries) {
        Objects.requireNonNull(tries, "tries");
        ArrayList<V> out = new ArrayList<>();
        try (tries) {
            Iterator<Try<V>> it = tries.iterator();
            while (it.hasNext()) {
                Try<V> t = it.next();
                if (t == null) {
                    throw new NullPointerException("tries contains null element");
                }
                if (t instanceof Failure<V> f) {
                    return Try.failure(f.cause());
                }
                out.add(((Success<V>) t).value());
            }
        }
        return Try.success(List.copyOf(out));
    }

    /**
     * Maps each element of an iterable through {@code mapper} and collects the results into a
     * {@code Try<List<B>>}. Fails fast on the first {@code Failure} returned by the mapper.
     *
     * @param <A>    the input element type
     * @param <B>    the mapped value type
     * @param values the iterable of input values; must not be {@code null}
     * @param mapper a function that maps each value to a {@code Try}; must not be {@code null}
     *               and must not return {@code null}
     * @return {@code Success(List<B>)} if all mappings succeed, or the first {@code Failure} produced by the mapper
     * @throws NullPointerException if {@code values} or {@code mapper} is {@code null},
     *                              or if the mapper returns {@code null}
     */
    static <A, B> Try<List<B>> traverse(Iterable<A> values, Function<? super A, Try<B>> mapper) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mapper, "mapper");
        ArrayList<B> out = new ArrayList<>();
        for (A a : values) {
            Try<B> t = Objects.requireNonNull(mapper.apply(a), "traverse mapper must not return null");
            if (t instanceof Failure<B> f) {
                return Try.failure(f.cause());
            }
            out.add(((Success<B>) t).value());
        }
        return Try.success(List.copyOf(out));
    }

    /**
     * Maps each element of a stream through {@code mapper} and collects the results into a
     * {@code Try<List<B>>}. Fails fast on the first {@code Failure} returned by the mapper;
     * the stream is closed in all cases.
     *
     * @param <A>    the input element type
     * @param <B>    the mapped value type
     * @param values the stream of input values; must not be {@code null}
     * @param mapper a function that maps each value to a {@code Try}; must not be {@code null}
     *               and must not return {@code null}
     * @return {@code Success(List<B>)} if all mappings succeed, or the first {@code Failure} produced by the mapper
     * @throws NullPointerException if {@code values} or {@code mapper} is {@code null},
     *                              or if the mapper returns {@code null}
     */
    static <A, B> Try<List<B>> traverse(Stream<A> values, Function<? super A, Try<B>> mapper) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mapper, "mapper");
        ArrayList<B> out = new ArrayList<>();
        try (values) {
            Iterator<A> it = values.iterator();
            while (it.hasNext()) {
                A a = it.next();
                Try<B> t = Objects.requireNonNull(mapper.apply(a), "traverse mapper must not return null");
                if (t instanceof Failure<B> f) {
                    return Try.failure(f.cause());
                }
                out.add(((Success<B>) t).value());
            }
        }
        return Try.success(List.copyOf(out));
    }

}
