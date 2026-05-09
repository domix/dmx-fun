package dmx.fun;

import org.jspecify.annotations.NullMarked;

/**
 * A functional interface representing a supplier that can produce a value and may throw a checked exception.
 * This is a variation of the standard {@code Supplier} interface, intended for use in scenarios where
 * the operation may involve checked exceptions.
 *
 * <p>Primary entry point for {@link Try#of(CheckedSupplier)} and
 * {@link Try#withTimeout(java.time.Duration, CheckedSupplier)}: the supplier is executed and any
 * thrown exception is captured as a {@code Failure} without manual wrapping.
 *
 * <p>The rationale for providing checked variants of the standard functional interfaces
 * (and the choice of {@code throws Exception} over {@code throws Throwable}) is documented in
 * <a href="https://domix.github.io/dmx-fun/adr/adr-019-checked-functional-interfaces/">
 * ADR-019 — CheckedFunction, CheckedSupplier, CheckedRunnable, CheckedConsumer as first-class interfaces</a>.
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
