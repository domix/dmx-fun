package codes.domix.fun;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * Package-private utilities for recurring null-check patterns shared across container types.
 */
@NullMarked
final class Containers {
    private Containers() {}

    /**
     * Requires that {@code value} is non-null, throwing {@link NullPointerException} with the
     * message {@code context + " returned null"} if it is.
     *
     * @param value   the value to check
     * @param context a short description of the producing expression (e.g. {@code "mapper"})
     * @param <T>     the value type
     * @return {@code value}, guaranteed non-null
     * @throws NullPointerException if {@code value} is {@code null}
     */
    static <T> T requireNonNullResult(T value, String context) {
        return Objects.requireNonNull(value, context + " returned null");
    }
}
