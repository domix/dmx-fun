package dmx.fun;

import org.jspecify.annotations.NullMarked;

/**
 * A functional interface representing a supplier that can produce a value and may throw a checked exception.
 * This is a variation of the standard {@code Supplier} interface, intended for use in scenarios where
 * the operation may involve checked exceptions.
 *
 * @param <T> the type of result supplied by this supplier
 */
@NullMarked
@FunctionalInterface
public interface CheckedSupplier<T> {

    /**
     * Retrieves and returns a value, potentially throwing a checked exception during execution.
     *
     * @return the value retrieved by this method
     * @throws Exception if an error occurs during the retrieval
     */
    T get() throws Exception;
}
