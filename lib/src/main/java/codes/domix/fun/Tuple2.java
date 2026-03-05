package codes.domix.fun;

/**
 * An immutable tuple holding two values of potentially different types.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param _1  the first element
 * @param _2  the second element
 */
public record Tuple2<A, B>(A _1, B _2) {
}
