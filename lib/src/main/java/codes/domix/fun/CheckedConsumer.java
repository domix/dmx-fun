package codes.domix.fun;

import org.jspecify.annotations.NullMarked;

/**
 * A functional interface representing an operation that accepts a single input argument,
 * returns no result, and may throw a checked exception.
 * This is a variation of the standard {@code Consumer} interface, intended for use in scenarios where
 * the operation may involve checked exceptions.
 *
 * @param <T> the type of the input to the operation
 */
@NullMarked
@FunctionalInterface
public interface CheckedConsumer<T> {

    /**
     * Performs this operation on the given argument, potentially throwing a checked exception.
     *
     * @param t the input argument
     * @throws Exception if an error occurs during the operation
     */
    void accept(T t) throws Exception;
}
