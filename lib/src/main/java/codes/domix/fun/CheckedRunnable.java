package codes.domix.fun;

import org.jspecify.annotations.NullMarked;

/**
 * A functional interface for runnables that can throw checked exceptions.
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
