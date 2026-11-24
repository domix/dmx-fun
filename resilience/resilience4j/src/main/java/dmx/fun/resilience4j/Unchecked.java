package dmx.fun.resilience4j;

import org.jspecify.annotations.NullMarked;

/**
 * Shared unwrap helper for the {@code executeResultTyped} bridges, which rethrow a checked
 * exception from the call as an unchecked one.
 */
@NullMarked
final class Unchecked {

    /** Backstop against cause cycles and pathological chains. */
    private static final int MAX_CAUSE_DEPTH = 1000;

    private Unchecked() {
    }

    /**
     * Wraps a checked throwable in a {@link RuntimeException}. If the throwable — or one
     * reachable through its cause chain — is an {@link InterruptedException}, the current
     * thread's interrupt flag is restored first, so the wrap does not hide the interruption
     * from cooperative-shutdown checks further up the stack.
     *
     * @param throwable the checked throwable to wrap
     * @return a {@code RuntimeException} whose cause is {@code throwable}
     */
    static RuntimeException wrap(Throwable throwable) {
        var depth = 0;
        for (var t = throwable; t != null && depth < MAX_CAUSE_DEPTH; t = t.getCause(), depth++) {
            if (t instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return new RuntimeException(throwable);
    }
}
