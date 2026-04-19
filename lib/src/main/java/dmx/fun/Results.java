package dmx.fun;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;

/**
 * Collector facade for {@code Stream<Result<V, E>>}.
 *
 * <p>Centralises all {@link Result} collector factories as static methods, mirroring the role
 * of {@link java.util.stream.Collectors}. The underlying factories on {@link Result} are kept
 * for backward compatibility; this class simply delegates to them.
 *
 * <p>Usage:
 * <pre>{@code
 * import dmx.fun.Results;
 *
 * Result<List<Integer>, String> r  = stream.collect(Results.toList());
 * Results.Partition<Integer, String> p = stream.collect(Results.partitioning());
 * Map<String, NonEmptyList<Item>>  m  = stream.collect(Results.groupingBy(Item::category));
 * }</pre>
 *
 * @see Result
 */
@NullMarked
public final class Results {

    private Results() {}

    /**
     * Transparent alias for {@link Result.Partition} so callers can use {@code Results.Partition}
     * without importing {@link Result} directly.
     *
     * @param <V> the value type of the {@code Ok} elements
     * @param <E> the error type of the {@code Err} elements
     */
    public record Partition<V, E>(List<V> oks, List<E> errors) {
        /** Null-checks and defensively copies both lists. */
        public Partition {
            Objects.requireNonNull(oks,    "oks");
            Objects.requireNonNull(errors, "errors");
            oks    = List.copyOf(oks);
            errors = List.copyOf(errors);
        }

        /** Converts to the underlying {@link Result.Partition}. */
        public Result.Partition<V, E> toResultPartition() {
            return new Result.Partition<>(oks, errors);
        }
    }

    /**
     * Returns a {@link Collector} that accumulates a {@code Stream<Result<V, E>>} into a single
     * {@code Result<List<V>, E>}, failing on the first {@code Err} encountered.
     *
     * <p>Delegates to {@link Result#toList()}.
     *
     * @param <V> the value type
     * @param <E> the error type
     * @return a collector producing {@code Result<List<V>, E>}
     */
    public static <V, E> Collector<Result<V, E>, ?, Result<List<V>, E>> toList() {
        return Result.toList();
    }

    /**
     * Returns a {@link Collector} that partitions a {@code Stream<Result<V, E>>} into ok-values
     * and errors.
     *
     * <p>Delegates to {@link Result#partitioningBy()}.
     *
     * @param <V> the value type
     * @param <E> the error type
     * @return a collector producing a {@link Results.Partition}
     */
    public static <V, E> Collector<Result<V, E>, ?, Results.Partition<V, E>> partitioning() {
        return Collectors.collectingAndThen(
            Result.partitioningBy(),
            p -> new Results.Partition<>(p.oks(), p.errors()));
    }

    /**
     * Returns a {@link Collector} that groups stream elements by a key derived from each element.
     *
     * <p>Delegates to {@link Result#groupingBy(Function)}.
     *
     * @param <V>        the element type
     * @param <K>        the key type
     * @param classifier a function mapping each element to a grouping key
     * @return a collector producing a {@code Map<K, NonEmptyList<V>>}
     */
    public static <V, K> Collector<V, ?, Map<K, NonEmptyList<V>>> groupingBy(
            Function<? super V, ? extends K> classifier) {
        return Result.groupingBy(classifier);
    }

    /**
     * Returns a {@link Collector} that groups stream elements by a key derived from each element,
     * then applies a finishing function to each group's {@link NonEmptyList}.
     *
     * <p>Delegates to {@link Result#groupingBy(Function, Function)}.
     *
     * @param <V>        the element type
     * @param <K>        the key type
     * @param <R>        the result type of the downstream function
     * @param classifier a function mapping each element to a grouping key
     * @param downstream a function applied to each group's {@code NonEmptyList}
     * @return a collector producing a {@code Map<K, R>}
     */
    public static <V, K, R> Collector<V, ?, Map<K, R>> groupingBy(
            Function<? super V, ? extends K> classifier,
            Function<? super NonEmptyList<V>, ? extends R> downstream) {
        return Result.groupingBy(classifier, downstream);
    }
}
