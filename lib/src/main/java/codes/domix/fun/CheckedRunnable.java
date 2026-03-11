package codes.domix.fun;

import org.jspecify.annotations.NullMarked;

/**
 * A functional interface representing a runnable that may throw a checked exception.
 * This is a variation of the standard {@code Runnable} interface, intended for use in scenarios where
 * the operation may involve checked exceptions.
 */
@NullMarked
@FunctionalInterface
public interface CheckedRunnable {

    /**
     * Executes an operation that may throw a checked exception.
     *
     * @throws Exception if an error occurs during execution
     */
    void run() throws Exception;
}
