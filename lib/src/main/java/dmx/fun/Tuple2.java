package dmx.fun;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * An immutable tuple holding two values of potentially different types.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param _1  the first element
 * @param _2  the second element
 */
@NullMarked
public record Tuple2<A, B>(A _1, B _2) {

    /**
     * Creates a new {@code Tuple2} with the given elements.
     *
     * @param <A> the type of the first element
     * @param <B> the type of the second element
     * @param a   the first element
     * @param b   the second element
     * @return a new {@code Tuple2} containing the two values
     */
    public static <A, B> Tuple2<A, B> of(A a, B b) {
        return new Tuple2<>(a, b);
    }

    /**
     * Returns a new tuple with {@code f} applied to the first element; the second element is unchanged.
     *
     * @param <R> the type of the new first element
     * @param f   the mapping function
     * @return a new {@code Tuple2} with the transformed first element
     */
    public <R> Tuple2<R, B> mapFirst(Function<? super A, ? extends R> f) {
        return new Tuple2<>(Objects.requireNonNull(f.apply(_1)), _2);
    }

    /**
     * Returns a new tuple with {@code f} applied to the second element; the first element is unchanged.
     *
     * @param <R> the type of the new second element
     * @param f   the mapping function
     * @return a new {@code Tuple2} with the transformed second element
     */
    public <R> Tuple2<A, R> mapSecond(Function<? super B, ? extends R> f) {
        return new Tuple2<>(_1, Objects.requireNonNull(f.apply(_2)));
    }

    /**
     * Collapses this pair into a single value by applying {@code f} to both elements.
     *
     * @param <R> the type of the result
     * @param f   the combining function
     * @return the non-null result of applying {@code f} to {@code _1} and {@code _2}
     */
    public <R> R map(BiFunction<? super A, ? super B, ? extends R> f) {
        return Objects.requireNonNull(f.apply(_1, _2));
    }
}
