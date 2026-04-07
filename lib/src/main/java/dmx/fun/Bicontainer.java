package dmx.fun;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Package-private base interface shared by two-variant sealed types that hold either a
 * <em>success value</em> ({@code V}) or an <em>error</em> ({@code E}).
 *
 * <p>Implementing types must provide three hook methods:
 * <ul>
 *   <li>{@link #isSuccess()} — indicates which variant is active</li>
 *   <li>{@link #get()} — returns the success value (throws on error variant)</li>
 *   <li>{@link #getError()} — returns the error value (throws on success variant)</li>
 * </ul>
 * All other methods are implemented as defaults over those three hooks.
 *
 * <p>This interface is {@code @NullMarked}: all parameters and return values are non-null
 * by default unless annotated with {@code @Nullable}.
 *
 * @param <V> the success value type
 * @param <E> the error type
 */
@NullMarked
interface Bicontainer<V, E> {

    /** Returns {@code true} if this instance represents the success variant. */
    boolean isSuccess();

    /**
     * Returns the success value.
     *
     * @throws java.util.NoSuchElementException if this instance represents the error variant
     */
    V get();

    /**
     * Returns the error value.
     *
     * @throws java.util.NoSuchElementException if this instance represents the success variant
     */
    E getError();

    // ---------- Accessors ----------

    /**
     * Returns the success value if present, or {@code fallback} otherwise.
     *
     * @param fallback the non-null fallback value
     * @return the success value, or {@code fallback} if this is the error variant
     * @throws NullPointerException if {@code fallback} is {@code null}
     */
    default V getOrElse(V fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return isSuccess() ? get() : fallback;
    }

    /**
     * Returns the success value if present, or the value produced by {@code supplier} otherwise.
     *
     * @param supplier supplier of the fallback value; must not return {@code null}
     * @return the success value, or the value produced by {@code supplier}
     * @throws NullPointerException if {@code supplier} is {@code null} or returns {@code null}
     */
    default V getOrElseGet(Supplier<V> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return isSuccess() ? get() : Containers.requireNonNullResult(supplier.get(), "supplier");
    }

    /**
     * Returns the success value if present, or {@code null} otherwise.
     *
     * @return the success value or {@code null}
     */
    default @Nullable V getOrNull() {
        return isSuccess() ? get() : null;
    }

    /**
     * Returns the success value if present, or throws an exception mapped from the error.
     *
     * @param exMapper maps the error to a {@link RuntimeException}; must not return {@code null}
     * @return the success value
     * @throws NullPointerException if {@code exMapper} is {@code null} or returns {@code null}
     * @throws RuntimeException     the exception produced by {@code exMapper} on the error variant
     */
    default V getOrThrow(Function<E, ? extends RuntimeException> exMapper) {
        Objects.requireNonNull(exMapper, "exMapper");
        if (isSuccess()) return get();
        throw Containers.requireNonNullResult(exMapper.apply(getError()), "exMapper");
    }

    // ---------- Stream / fold / match ----------

    /**
     * Returns a single-element stream of the success value, or an empty stream on error.
     *
     * @return a stream of the value or empty
     */
    default Stream<V> stream() {
        return isSuccess() ? Stream.of(get()) : Stream.empty();
    }

    /**
     * Folds this container to a single value by applying the appropriate function.
     *
     * @param <R>       the result type
     * @param onSuccess applied to the success value; must not return {@code null}
     * @param onError   applied to the error value; must not return {@code null}
     * @return the result of the applied function
     * @throws NullPointerException if either function is {@code null} or returns {@code null}
     */
    default <R> R fold(Function<V, R> onSuccess, Function<E, R> onError) {
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onError, "onError");
        return isSuccess()
            ? Containers.requireNonNullResult(onSuccess.apply(get()), "onSuccess")
            : Containers.requireNonNullResult(onError.apply(getError()), "onError");
    }

    /**
     * Executes one of the two consumers based on the active variant.
     *
     * @param onSuccess called with the success value
     * @param onError   called with the error value
     * @throws NullPointerException if either consumer is {@code null}
     */
    default void match(Consumer<V> onSuccess, Consumer<E> onError) {
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onError, "onError");
        if (isSuccess()) onSuccess.accept(get());
        else onError.accept(getError());
    }

    // ---------- Interop ----------

    /**
     * Converts this container to an {@link Option}.
     * The success variant maps to {@link Option.Some}; the error variant maps to {@link Option#none()}.
     *
     * @return an {@code Option} containing the success value, or empty
     */
    default Option<V> toOption() {
        return isSuccess() ? Option.some(get()) : Option.none();
    }
}
