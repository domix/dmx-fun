package dmx.fun;

import org.jspecify.annotations.NullMarked;

/**
 * A functional interface representing a function that accepts one argument, produces a result,
 * and may throw a checked exception.
 * This is a variation of the standard {@code Function} interface, intended for use in scenarios where
 * the operation may involve checked exceptions.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@NullMarked
@FunctionalInterface
public interface CheckedFunction<T, R> {

    /**
     * Applies this function to the given argument, potentially throwing a checked exception.
     *
     * @param t the function argument
     * @return the function result
     * @throws Exception if an error occurs during the application
     */
    R apply(T t) throws Exception;
}
