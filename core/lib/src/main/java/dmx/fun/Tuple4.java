package dmx.fun;

import org.jspecify.annotations.NullMarked;
import java.util.Objects;
import java.util.function.Function;

/**
 * An immutable quadruple holding four values of potentially different types.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param <C> the type of the third element
 * @param <D> the type of the fourth element
 * @param _1  the first element
 * @param _2  the second element
 * @param _3  the third element
 * @param _4  the fourth element
 */
@NullMarked
public record Tuple4<A, B, C, D>(A _1, B _2, C _3, D _4) {
    /** Validates that none of the elements are null. */
    public Tuple4 {
        Objects.requireNonNull(_1, "_1 must not be null");
        Objects.requireNonNull(_2, "_2 must not be null");
        Objects.requireNonNull(_3, "_3 must not be null");
        Objects.requireNonNull(_4, "_4 must not be null");
    }

    /**
     * Creates a new {@code Tuple4} with the given elements.
     *
     * @param <A> the type of the first element
     * @param <B> the type of the second element
     * @param <C> the type of the third element
     * @param <D> the type of the fourth element
     * @param a   the first element
     * @param b   the second element
     * @param c   the third element
     * @param d   the fourth element
     * @return a new {@code Tuple4} containing the four values
     */
    public static <A, B, C, D> Tuple4<A, B, C, D> of(A a, B b, C c, D d) {
        return new Tuple4<>(a, b, c, d);
    }

    /**
     * Returns a new tuple with {@code f} applied to the first element; the other elements are unchanged.
     *
     * @param <R> the type of the new first element
     * @param f   the mapping function
     * @return a new {@code Tuple4} with the transformed first element
     */
    public <R> Tuple4<R, B, C, D> mapFirst(Function<? super A, ? extends R> f) {
        return new Tuple4<>(Objects.requireNonNull(f.apply(_1)), _2, _3, _4);
    }

    /**
     * Returns a new tuple with {@code f} applied to the second element; the other elements are unchanged.
     *
     * @param <R> the type of the new second element
     * @param f   the mapping function
     * @return a new {@code Tuple4} with the transformed second element
     */
    public <R> Tuple4<A, R, C, D> mapSecond(Function<? super B, ? extends R> f) {
        return new Tuple4<>(_1, Objects.requireNonNull(f.apply(_2)), _3, _4);
    }

    /**
     * Returns a new tuple with {@code f} applied to the third element; the other elements are unchanged.
     *
     * @param <R> the type of the new third element
     * @param f   the mapping function
     * @return a new {@code Tuple4} with the transformed third element
     */
    public <R> Tuple4<A, B, R, D> mapThird(Function<? super C, ? extends R> f) {
        return new Tuple4<>(_1, _2, Objects.requireNonNull(f.apply(_3)), _4);
    }

    /**
     * Returns a new tuple with {@code f} applied to the fourth element; the other elements are unchanged.
     *
     * @param <R> the type of the new fourth element
     * @param f   the mapping function
     * @return a new {@code Tuple4} with the transformed fourth element
     */
    public <R> Tuple4<A, B, C, R> mapFourth(Function<? super D, ? extends R> f) {
        return new Tuple4<>(_1, _2, _3, Objects.requireNonNull(f.apply(_4)));
    }

    /**
     * Collapses this quadruple into a single value by applying {@code f} to all four elements.
     *
     * @param <R> the type of the result
     * @param f   the combining function
     * @return the non-null result of applying {@code f} to {@code _1}, {@code _2}, {@code _3}, and {@code _4}
     */
    public <R> R map(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends R> f) {
        return Objects.requireNonNull(f.apply(_1, _2, _3, _4));
    }
}
