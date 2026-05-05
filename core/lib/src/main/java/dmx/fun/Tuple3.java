package dmx.fun;

import org.jspecify.annotations.NullMarked;
import java.util.Objects;
import java.util.function.Function;

/**
 * An immutable triple holding three values of potentially different types.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param <C> the type of the third element
 * @param _1  the first element
 * @param _2  the second element
 * @param _3  the third element
 */
@NullMarked
public record Tuple3<A, B, C>(A _1, B _2, C _3) {
    /** Validates that none of the elements are null. */
    public Tuple3 {
        Objects.requireNonNull(_1, "_1 must not be null");
        Objects.requireNonNull(_2, "_2 must not be null");
        Objects.requireNonNull(_3, "_3 must not be null");
    }

    /**
     * Creates a new {@code Tuple3} with the given elements.
     *
     * @param <A> the type of the first element
     * @param <B> the type of the second element
     * @param <C> the type of the third element
     * @param a   the first element
     * @param b   the second element
     * @param c   the third element
     * @return a new {@code Tuple3} containing the three values
     */
    public static <A, B, C> Tuple3<A, B, C> of(A a, B b, C c) {
        return new Tuple3<>(a, b, c);
    }

    /**
     * Returns a new tuple with {@code f} applied to the first element; the other elements are unchanged.
     *
     * @param <R> the type of the new first element
     * @param f   the mapping function
     * @return a new {@code Tuple3} with the transformed first element
     */
    public <R> Tuple3<R, B, C> mapFirst(Function<? super A, ? extends R> f) {
        return new Tuple3<>(Objects.requireNonNull(f.apply(_1)), _2, _3);
    }

    /**
     * Returns a new tuple with {@code f} applied to the second element; the other elements are unchanged.
     *
     * @param <R> the type of the new second element
     * @param f   the mapping function
     * @return a new {@code Tuple3} with the transformed second element
     */
    public <R> Tuple3<A, R, C> mapSecond(Function<? super B, ? extends R> f) {
        return new Tuple3<>(_1, Objects.requireNonNull(f.apply(_2)), _3);
    }

    /**
     * Returns a new tuple with {@code f} applied to the third element; the other elements are unchanged.
     *
     * @param <R> the type of the new third element
     * @param f   the mapping function
     * @return a new {@code Tuple3} with the transformed third element
     */
    public <R> Tuple3<A, B, R> mapThird(Function<? super C, ? extends R> f) {
        return new Tuple3<>(_1, _2, Objects.requireNonNull(f.apply(_3)));
    }

    /**
     * Collapses this triple into a single value by applying {@code f} to all three elements.
     *
     * @param <R> the type of the result
     * @param f   the combining function
     * @return the non-null result of applying {@code f} to {@code _1}, {@code _2}, and {@code _3}
     */
    public <R> R map(TriFunction<? super A, ? super B, ? super C, ? extends R> f) {
        return Objects.requireNonNull(f.apply(_1, _2, _3));
    }
}
