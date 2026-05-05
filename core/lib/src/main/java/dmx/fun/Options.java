package dmx.fun;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import org.jspecify.annotations.NullMarked;

/**
 * Collector facade for {@code Stream<Option<T>>}.
 *
 * <p>Centralises all {@link Option} and {@link NonEmptyList} collector factories that operate on
 * option streams as static methods, mirroring the role of {@link java.util.stream.Collectors}.
 * The underlying factories on {@link Option} and {@link NonEmptyList} are kept for backward
 * compatibility; this class simply delegates to them.
 *
 * <p>Usage:
 * <pre>{@code
 * import static dmx.fun.Options.*;
 *
 * List<String>              present = stream.collect(Options.presentToList());
 * Optional<List<String>>    seq     = stream.collect(Options.sequence());
 * Option<NonEmptyList<String>> nel  = stream.collect(Options.toNonEmptyList());
 * }</pre>
 *
 * @see Option
 * @see NonEmptyList
 */
@NullMarked
public final class Options {

    private Options() {}

    /**
     * Returns a {@link Collector} that collects all present values from a
     * {@code Stream<Option<V>>} into an unordered {@link List}, discarding empty options.
     *
     * <p>Delegates to {@link Option#presentValuesToList()}.
     *
     * @param <V> the value type
     * @return a collector producing a {@code List<V>} of present values
     */
    public static <V> Collector<Option<? extends V>, ?, List<V>> presentToList() {
        return Option.presentValuesToList();
    }

    /**
     * Returns a {@link Collector} that reduces a {@code Stream<Option<V>>} to a single
     * {@code Optional<List<V>>}: {@code Optional.of(list)} if every element is present,
     * or {@code Optional.empty()} if any element is empty.
     *
     * <p>Delegates to {@link Option#sequenceCollector()}.
     *
     * @param <V> the value type
     * @return a collector producing {@code Optional<List<V>>}
     */
    public static <V> Collector<Option<V>, ?, Optional<List<V>>> sequence() {
        return Option.sequenceCollector();
    }

    /**
     * Returns a {@link Collector} that accumulates a {@code Stream<T>} into an
     * {@code Option<NonEmptyList<T>>}: {@code Option.some(nel)} if the stream is non-empty,
     * or {@code Option.none()} if the stream is empty.
     *
     * <p>Delegates to {@link NonEmptyList#toNonEmptyList()}.
     *
     * @param <T> the element type
     * @return a collector producing {@code Option<NonEmptyList<T>>}
     */
    public static <T> Collector<T, ?, Option<NonEmptyList<T>>> toNonEmptyList() {
        return NonEmptyList.toNonEmptyList();
    }
}
