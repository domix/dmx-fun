package dmx.fun;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * Predicate that classifies a {@link Throwable} as <em>non-fatal</em> — safe to capture,
 * map, and recover from as an ordinary failure value — or <em>fatal</em>, signaling a
 * condition that should propagate rather than travel through a pipeline.
 *
 * <p>The fatal set is:
 * <ul>
 *   <li>{@link VirtualMachineError} — e.g. {@link OutOfMemoryError},
 *       {@link StackOverflowError}: the JVM itself is compromised.</li>
 *   <li>{@link LinkageError} — the class environment is broken.</li>
 *   <li>{@link InterruptedException} — cancellation, not failure: swallowing it
 *       defeats cooperative shutdown.</li>
 * </ul>
 * Everything else — including other {@link Error} subclasses such as
 * {@link AssertionError} — is considered non-fatal. This follows the spirit of Scala's
 * {@code scala.util.control.NonFatal}, whose fatal set additionally includes
 * {@code ThreadDeath} and {@code ControlThrowable} — the former is deprecated for
 * removal and no longer thrown by the JVM, and the latter has no Java analogue.
 *
 * <p>Note that this checks only the given throwable, not its cause chain. Borders that
 * need chain-aware handling (interruption in particular often arrives wrapped as another
 * exception's cause) should use {@link Try#rethrowFatal()}.
 *
 * @see Try#rethrowFatal()
 */
@NullMarked
public final class NonFatal {

    private NonFatal() {
    }

    /**
     * Returns {@code true} if {@code throwable} is non-fatal.
     *
     * @param throwable the throwable to classify; must not be {@code null}
     * @return {@code true} if non-fatal, {@code false} if fatal
     * @throws NullPointerException if {@code throwable} is {@code null}
     */
    public static boolean check(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable");
        return !(throwable instanceof VirtualMachineError
            || throwable instanceof LinkageError
            || throwable instanceof InterruptedException);
    }
}
