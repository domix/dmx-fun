package dmx.fun;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import org.jspecify.annotations.NullMarked;

/**
 * The library's fatal-throwable policy: classifies a {@link Throwable} as
 * <em>non-fatal</em> — safe to capture, map, and recover from as an ordinary failure
 * value — or <em>fatal</em>, signaling a condition that should propagate rather than
 * travel through a pipeline.
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
 * <p>{@link java.util.concurrent.CancellationException} is deliberately non-fatal,
 * even though it also signals cancellation: it is the library's own convention to
 * surface a cancelled future as an ordinary failure value
 * ({@link Try#fromFuture(java.util.concurrent.CompletableFuture) Try.fromFuture}
 * manufactures exactly that), and unlike {@link InterruptedException} it carries no
 * thread-local interrupt flag that swallowing would lose.
 *
 * <p>{@link #check(Throwable)} classifies only the given throwable. Borders that need
 * chain-aware handling (interruption in particular often arrives wrapped as another
 * exception's cause) should use {@link #rethrowIfFatal(Throwable)} or
 * {@link Try#rethrowFatal()}.
 *
 * @see Try#rethrowFatal()
 */
@NullMarked
public final class NonFatal {

    /**
     * Backstop against unbounded throwable graphs: cause cycles in the allocation-free
     * linear walk, and {@code getCause()} overrides that synthesize a fresh instance
     * per call.
     */
    private static final int VISIT_CAP = 1000;

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

    /**
     * Rethrows {@code throwable} if it — or any throwable reachable through its cause
     * chain and suppressed exceptions — is fatal per {@link #check(Throwable)};
     * otherwise returns normally.
     *
     * <p>If an {@link InterruptedException} is reachable, the current thread's
     * interrupt flag is set before anything is thrown — note this <em>propagates</em>
     * the interruption to the calling thread, which is not necessarily the thread the
     * exception was raised on. A fatal {@link Error} takes priority and is rethrown
     * as-is. Otherwise an interruption is rethrown unchecked: as {@code throwable}
     * itself when it already is a {@link CompletionException} (not wrapped again), and
     * as {@code new CompletionException(throwable)} otherwise — the full original
     * graph stays reachable through the cause.
     *
     * <p>Traversal is bounded at {@value #VISIT_CAP} throwables — a backstop against
     * cause cycles and hostile {@code getCause()} overrides — so a fatal parked beyond
     * that bound goes undetected. The dominant shapes — a plain cause chain, or one
     * level of suppressed exceptions as parked by {@link Resource} — are traversed
     * without allocating; a worklist is built only when a suppressed throwable
     * carries a graph of its own.
     *
     * @param throwable the throwable to inspect; must not be {@code null}
     * @throws NullPointerException if {@code throwable} is {@code null}
     * @throws Error                if a fatal {@code Error} is reachable within the
     *                              traversal bound
     * @throws CompletionException  if an {@link InterruptedException} is reachable and
     *                              no fatal {@code Error} is present
     */
    public static void rethrowIfFatal(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable");
        Error fatalError = null;
        var interrupted = false;
        // Escalation structures, allocated only when a suppressed throwable has a
        // graph of its own — the plain cause chain and Resource's one-level
        // suppressed pattern stay allocation-free.
        ArrayDeque<Throwable> pending = null;
        var visited = 0;
        var t = throwable;
        while (t != null && visited < VISIT_CAP) {
            visited++;
            if (!check(t)) {
                if (fatalError == null && t instanceof Error error) {
                    fatalError = error;
                }
                if (t instanceof InterruptedException) {
                    interrupted = true;
                }
                if (fatalError != null && interrupted) {
                    break; // no further node can change the outcome
                }
            }
            for (var suppressed : t.getSuppressed()) {
                if (suppressed.getCause() != null || suppressed.getSuppressed().length > 0) {
                    if (pending == null) {
                        pending = new ArrayDeque<>();
                    }
                    pending.push(suppressed);
                } else {
                    visited++;
                    if (!check(suppressed)) {
                        if (fatalError == null && suppressed instanceof Error error) {
                            fatalError = error;
                        }
                        if (suppressed instanceof InterruptedException) {
                            interrupted = true;
                        }
                    }
                }
            }
            var next = t.getCause();
            if (next == null && pending != null) {
                next = pending.poll();
            }
            t = next;
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        if (fatalError != null) {
            throw fatalError;
        }
        if (interrupted) {
            if (throwable instanceof CompletionException completion) {
                throw completion;
            }
            throw new CompletionException(throwable);
        }
    }
}
