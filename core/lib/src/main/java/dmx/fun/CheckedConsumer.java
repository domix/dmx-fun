package dmx.fun;

import org.jspecify.annotations.NullMarked;

/**
 * A functional interface representing an operation that accepts a single input argument,
 * returns no result, and may throw a checked exception.
 * This is a variation of the standard {@code Consumer} interface, intended for use in scenarios where
 * the operation may involve checked exceptions.
 *
 * <p>Used as the {@code release} parameter of {@link Resource#of(CheckedSupplier, CheckedConsumer)}
 * to define the teardown action for a managed resource.
 *
 * <p>The rationale for providing checked variants of the standard functional interfaces
 * (and the choice of {@code throws Exception} over {@code throws Throwable}) is documented in
 * <a href="https://domix.github.io/dmx-fun/adr/adr-019-checked-functional-interfaces/">
 * ADR-019 — CheckedFunction, CheckedSupplier, CheckedRunnable, CheckedConsumer as first-class interfaces</a>.
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
