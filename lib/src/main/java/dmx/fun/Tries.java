package dmx.fun;

import java.util.List;
import java.util.stream.Collector;
import org.jspecify.annotations.NullMarked;

/**
 * Collector facade for {@code Stream<Try<V>>}.
 *
 * <p>Centralises all {@link Try} collector factories as static methods, mirroring the role of
 * {@link java.util.stream.Collectors}. The underlying factories on {@link Try} are kept for
 * backward compatibility; this class simply delegates to them.
 *
 * <p>Usage:
 * <pre>{@code
 * import static dmx.fun.Tries.*;
 *
 * Try<List<String>>       result = stream.collect(Tries.toList());
 * Tries.Partition<String> p      = stream.collect(Tries.partitioning());
 * p.successes(); // List<String>
 * p.failures();  // List<Throwable>
 * }</pre>
 *
 * @see Try
 */
@NullMarked
public final class Tries {

    private Tries() {}

    /**
     * Transparent alias for {@link Try.Partition} so callers can use {@code Tries.Partition}
     * without importing {@link Try} directly.
     *
     * @param <V> the value type of the {@code Success} elements
     */
    public record Partition<V>(List<V> successes, List<Throwable> failures) {
        /** Delegates to {@link Try.Partition} for consistent null/copy semantics. */
        public Partition {
            var delegate = new Try.Partition<>(successes, failures);
            successes = delegate.successes();
            failures  = delegate.failures();
        }

        /** Converts to the underlying {@link Try.Partition}. */
        public Try.Partition<V> toTryPartition() {
            return new Try.Partition<>(successes, failures);
        }
    }

    /**
     * Returns a {@link Collector} that accumulates a {@code Stream<Try<V>>} into a single
     * {@code Try<List<V>>}, failing on the first {@code Failure} encountered.
     *
     * <p>Delegates to {@link Try#toList()}.
     *
     * @param <V> the success value type
     * @return a collector producing {@code Try<List<V>>}
     */
    public static <V> Collector<Try<V>, ?, Try<List<V>>> toList() {
        return Try.toList();
    }

    /**
     * Returns a {@link Collector} that partitions a {@code Stream<Try<V>>} into successes and
     * failures.
     *
     * <p>Delegates to {@link Try#partitioningBy()}.
     *
     * @param <V> the success value type
     * @return a collector producing a {@link Tries.Partition}
     */
    public static <V> Collector<Try<V>, ?, Tries.Partition<V>> partitioning() {
        return Collector.of(
            () -> new java.util.ArrayList<Try<V>>(),
            java.util.List::add,
            (a, b) -> { a.addAll(b); return a; },
            list -> {
                var p = list.stream().collect(Try.partitioningBy());
                return new Tries.Partition<>(p.successes(), p.failures());
            }
        );
    }
}
