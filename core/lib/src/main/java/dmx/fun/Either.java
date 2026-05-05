package dmx.fun;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * A disjoint union that is either a {@link Left} or a {@link Right}.
 *
 * <p>{@code Either<L,R>} is a neutral two-track type: neither side carries error
 * semantics. Use it when a computation legitimately returns one of two value types
 * without implying that one side is a failure. For error-handling use cases prefer
 * {@link Result}, which is semantically opinionated ({@code Ok} = success,
 * {@code Err} = failure) and offers richer recovery operations.
 *
 * <p>By convention, {@code map}, {@code flatMap}, and related operations act on
 * the <em>right</em> side. Use {@link #swap()} to flip the sides when you need
 * to operate on the left.
 *
 * <p>This interface is {@code @NullMarked}: all values — left and right — are
 * non-null by default.
 *
 * @param <L> the type of the left value
 * @param <R> the type of the right value
 */
@NullMarked
public sealed interface Either<L, R> permits Either.Left, Either.Right {

    /**
     * The left variant of {@link Either}.
     *
     * @param <L>   the type of the left value
     * @param <R>   the type of the right value
     * @param value the non-null left value
     */
    record Left<L, R>(L value) implements Either<L, R> {
        /** Validates that {@code value} is non-null. */
        public Left {
            Objects.requireNonNull(value, "Left value must not be null");
        }
    }

    /**
     * The right variant of {@link Either}.
     *
     * @param <L>   the type of the left value
     * @param <R>   the type of the right value
     * @param value the non-null right value
     */
    record Right<L, R>(R value) implements Either<L, R> {
        /** Validates that {@code value} is non-null. */
        public Right {
            Objects.requireNonNull(value, "Right value must not be null");
        }
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link Left} instance wrapping the given value.
     *
     * @param <L>   the left type
     * @param <R>   the right type
     * @param value the non-null left value
     * @return an {@code Either} containing the left value
     * @throws NullPointerException if {@code value} is {@code null}
     */
    static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }

    /**
     * Creates a {@link Right} instance wrapping the given value.
     *
     * @param <L>   the left type
     * @param <R>   the right type
     * @param value the non-null right value
     * @return an {@code Either} containing the right value
     * @throws NullPointerException if {@code value} is {@code null}
     */
    static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }

    // -------------------------------------------------------------------------
    // Predicates
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this is a {@link Left}.
     *
     * @return {@code true} for {@code Left}, {@code false} for {@code Right}
     */
    default boolean isLeft() {
        return this instanceof Left;
    }

    /**
     * Returns {@code true} if this is a {@link Right}.
     *
     * @return {@code true} for {@code Right}, {@code false} for {@code Left}
     */
    default boolean isRight() {
        return this instanceof Right;
    }

    // -------------------------------------------------------------------------
    // Extraction
    // -------------------------------------------------------------------------

    /**
     * Returns the left value.
     *
     * @return the left value
     * @throws NoSuchElementException if this is a {@link Right}
     */
    default L getLeft() {
        return switch (this) {
            case Left<L, R> left -> left.value();
            case Right<L, R> _ -> throw new NoSuchElementException("No left value present. This Either is Right.");
        };
    }

    /**
     * Returns the right value.
     *
     * @return the right value
     * @throws NoSuchElementException if this is a {@link Left}
     */
    default R getRight() {
        return switch (this) {
            case Right<L, R> right -> right.value();
            case Left<L, R> _ -> throw new NoSuchElementException("No right value present. This Either is Left.");
        };
    }

    // -------------------------------------------------------------------------
    // Transformation
    // -------------------------------------------------------------------------

    /**
     * Applies {@code onLeft} if this is {@link Left}, or {@code onRight} if this is
     * {@link Right}, and returns the result.
     *
     * @param <T>     the result type
     * @param onLeft  function applied to the left value
     * @param onRight function applied to the right value
     * @return the result of whichever function was applied
     */
    default <T> T fold(
            Function<? super L, ? extends T> onLeft,
            Function<? super R, ? extends T> onRight) {
        Objects.requireNonNull(onLeft, "onLeft");
        Objects.requireNonNull(onRight, "onRight");
        return switch (this) {
            case Left<L, R> left   -> onLeft.apply(left.value());
            case Right<L, R> right -> onRight.apply(right.value());
        };
    }

    /**
     * Maps the right value using {@code mapper}, leaving a {@link Left} unchanged.
     *
     * @param <R2>   the new right type
     * @param mapper function applied to the right value
     * @return a new {@code Either} with the mapped right value, or the original {@code Left}
     */
    default <R2> Either<L, R2> map(Function<? super R, ? extends R2> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return switch (this) {
            case Left<L, R> left   -> Either.left(left.value());
            case Right<L, R> right -> Either.right(mapper.apply(right.value()));
        };
    }

    /**
     * Maps the left value using {@code mapper}, leaving a {@link Right} unchanged.
     *
     * @param <L2>   the new left type
     * @param mapper function applied to the left value
     * @return a new {@code Either} with the mapped left value, or the original {@code Right}
     */
    default <L2> Either<L2, R> mapLeft(Function<? super L, ? extends L2> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return switch (this) {
            case Left<L, R> left   -> Either.left(mapper.apply(left.value()));
            case Right<L, R> right -> Either.right(right.value());
        };
    }

    /**
     * Applies {@code mapper} to the right value and returns the resulting {@code Either},
     * leaving a {@link Left} unchanged. This is the monadic bind for the right track.
     *
     * @param <R2>   the new right type
     * @param mapper function that returns an {@code Either} for the right value
     * @return the result of {@code mapper} applied to the right value, or the original {@code Left}
     */
    default <R2> Either<L, R2> flatMap(
            Function<? super R, ? extends Either<? extends L, ? extends R2>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return switch (this) {
            case Left<L, R> left   -> Either.left(left.value());
            case Right<L, R> right -> {
                @SuppressWarnings("unchecked")
                Either<L, R2> result = (Either<L, R2>) mapper.apply(right.value());
                yield Objects.requireNonNull(result, "mapper returned null");
            }
        };
    }

    /**
     * Swaps the two sides: {@link Left} becomes {@link Right} and vice versa.
     *
     * @return an {@code Either<R,L>} with the sides swapped
     */
    default Either<R, L> swap() {
        return switch (this) {
            case Left<L, R> left   -> Either.right(left.value());
            case Right<L, R> right -> Either.left(right.value());
        };
    }

    // -------------------------------------------------------------------------
    // Side effects
    // -------------------------------------------------------------------------

    /**
     * Executes {@code action} if this is a {@link Right}, then returns {@code this} unchanged.
     *
     * @param action consumer applied to the right value
     * @return this {@code Either}
     */
    default Either<L, R> peek(Consumer<? super R> action) {
        Objects.requireNonNull(action, "action");
        if (this instanceof Right<L, R>(R value)) {
            action.accept(value);
        }
        return this;
    }

    /**
     * Executes {@code action} if this is a {@link Left}, then returns {@code this} unchanged.
     *
     * @param action consumer applied to the left value
     * @return this {@code Either}
     */
    default Either<L, R> peekLeft(Consumer<? super L> action) {
        Objects.requireNonNull(action, "action");
        if (this instanceof Left<L, R>(L value)) {
            action.accept(value);
        }
        return this;
    }

    // -------------------------------------------------------------------------
    // Conversion
    // -------------------------------------------------------------------------

    /**
     * Converts this {@code Either} to an {@link Option}: {@link Right} becomes
     * {@link Option#some(Object)}, {@link Left} becomes {@link Option#none()}.
     *
     * @return an {@code Option} containing the right value, or empty
     */
    default Option<R> toOption() {
        return switch (this) {
            case Right<L, R> right -> Option.some(right.value());
            case Left<L, R> _      -> Option.none();
        };
    }

    /**
     * Converts this {@code Either} to a {@link Result}.
     *
     * <p>{@link Right} maps to {@link Result.Ok}; {@link Left} maps to {@link Result.Err}.
     * This is the inverse of {@link Result#toEither()}.
     *
     * @return a {@code Result<R, L>} equivalent of this {@code Either}
     */
    default Result<R, L> toResult() {
        return switch (this) {
            case Right<L, R> right -> Result.ok(right.value());
            case Left<L, R> left   -> Result.err(left.value());
        };
    }

    /**
     * Converts this {@code Either} to a {@link Validated}.
     *
     * <p>{@link Right} maps to {@link Validated#valid}; {@link Left} maps to
     * {@link Validated#invalid}. Useful for bringing a neutral {@code Either} into
     * the error-accumulating validation world.
     *
     * @return a {@code Validated<L, R>} equivalent of this {@code Either}
     */
    default Validated<L, R> toValidated() {
        return switch (this) {
            case Right<L, R> right -> Validated.valid(right.value());
            case Left<L, R> left   -> Validated.invalid(left.value());
        };
    }
}
