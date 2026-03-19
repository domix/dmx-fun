package codes.domix.fun;

import org.jspecify.annotations.NullMarked;

/**
 * A functional interface for a function that accepts three arguments and produces a result.
 *
 * @param <A> the type of the first argument
 * @param <B> the type of the second argument
 * @param <C> the type of the third argument
 * @param <R> the type of the result
 */
@NullMarked
@FunctionalInterface
public interface TriFunction<A, B, C, R> {
    /**
     * Applies this function to the given arguments.
     *
     * @param a the first argument
     * @param b the second argument
     * @param c the third argument
     * @return the result of applying this function
     */
    R apply(A a, B b, C c);
}
