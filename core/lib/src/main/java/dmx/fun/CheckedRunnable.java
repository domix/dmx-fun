package dmx.fun;

import org.jspecify.annotations.NullMarked;

/**
 * A functional interface representing a runnable that may throw a checked exception.
 * This is a variation of the standard {@code Runnable} interface, intended for use in scenarios where
 * the operation may involve checked exceptions.
 *
 * <p>Primary entry point for {@link Try#run(CheckedRunnable)}: the runnable is executed and any
 * thrown exception is captured as a {@code Failure}, returning {@code Success(null)} on completion.
 *
 * <p>The rationale for providing checked variants of the standard functional interfaces
 * (and the choice of {@code throws Exception} over {@code throws Throwable}) is documented in
 * <a href="https://domix.github.io/dmx-fun/adr/adr-019-checked-functional-interfaces/">
 * ADR-019 — CheckedFunction, CheckedSupplier, CheckedRunnable, CheckedConsumer as first-class interfaces</a>.
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
