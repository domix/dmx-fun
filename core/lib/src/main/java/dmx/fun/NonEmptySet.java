package dmx.fun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

/**
 * An immutable, non-empty set: a set guaranteed at construction time to contain
 * at least one element.
 *
 * <p>This type makes the non-emptiness constraint part of the static type system.
 * APIs that require at least one item can declare {@code NonEmptySet<T>} instead of
 * {@code Set<T>} and eliminate runtime emptiness checks entirely.
 *
 * <p>Insertion order is preserved (backed by {@link LinkedHashSet}). The first element
 * inserted is the {@link #head()}.
 *
 * <p>This class is {@code @NullMarked}: all elements and parameters are non-null by default.
 *
 * <p>The rationale for introducing dedicated non-empty collection types instead of using
 * standard JDK types with runtime checks is documented in
 * <a href="https://domix.github.io/dmx-fun/adr/adr-018-non-empty-collections/">
 * ADR-018 — NonEmptyList&lt;T&gt;, NonEmptySet&lt;T&gt;, NonEmptyMap&lt;K,V&gt; as structural guarantee types</a>.
 *
 * @param <T> the type of elements in this set
 */
@NullMarked
public final class NonEmptySet<T> implements Iterable<T> {

    private final T head;
    private final Set<T> tail; // unmodifiable, does NOT include head

    private transient volatile Set<T> cachedSet;

    private NonEmptySet(T head, Set<T> tail) {
        this.head = head;
        this.tail = tail;
    }

    // -------------------------------------------------------------------------
    // Smart constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code NonEmptySet} with the given head element and additional elements.
     *
     * <p>If {@code rest} contains {@code head}, the duplicate is silently ignored.
     *
     * @param head the first (mandatory) element; must not be {@code null}
     * @param rest additional elements; must not be {@code null}; elements must not be {@code null}
     * @param <T>  the element type
     * @return a new {@code NonEmptySet}
     * @throws NullPointerException if {@code head}, {@code rest}, or any element in
     *                              {@code rest} is {@code null}
     */
    public static <T> NonEmptySet<T> of(T head, Set<? extends T> rest) {
        Objects.requireNonNull(head, "head must not be null");
        Objects.requireNonNull(rest, "rest must not be null");
        Set<T> tail = new LinkedHashSet<>();
        for (T element : rest) {
            Objects.requireNonNull(element, "rest elements must not be null");
            if (!element.equals(head)) {
                tail.add(element);
            }
        }
        return new NonEmptySet<>(head, Collections.unmodifiableSet(tail));
    }

    /**
     * Creates a {@code NonEmptySet} containing exactly one element.
     *
     * @param head the sole element; must not be {@code null}
     * @param <T>  the element type
     * @return a singleton {@code NonEmptySet}
     * @throws NullPointerException if {@code head} is {@code null}
     */
    public static <T> NonEmptySet<T> singleton(T head) {
        Objects.requireNonNull(head, "head must not be null");
        return new NonEmptySet<>(head, Set.of());
    }

    /**
     * Attempts to construct a {@code NonEmptySet} from a plain {@link Set}.
     *
     * @param set  the source set; must not be {@code null}; elements must not be {@code null}
     * @param <T>  the element type
     * @return {@link Option#some(Object)} wrapping the {@code NonEmptySet} if the set is
     *         non-empty, or {@link Option#none()} if the set is empty
     * @throws NullPointerException if {@code set} or any element is {@code null}
     */
    public static <T> Option<NonEmptySet<T>> fromSet(Set<? extends T> set) {
        Objects.requireNonNull(set, "set must not be null");
        if (set.isEmpty()) {
            return Option.none();
        }
        set.forEach(e -> Objects.requireNonNull(e, "set elements must not be null"));
        return Option.some(fromSetUnsafe(set));
    }

    /**
     * Attempts to construct a singleton {@code NonEmptySet} from a JDK {@link Optional}.
     *
     * @param optional the source optional; must not be {@code null}
     * @param <T>      the element type
     * @return {@link Option#some(Object)} wrapping a singleton {@code NonEmptySet} if the
     *         optional is present, or {@link Option#none()} if the optional is empty
     * @throws NullPointerException if {@code optional} is {@code null}
     */
    public static <T> Option<NonEmptySet<T>> fromOptional(Optional<? extends T> optional) {
        Objects.requireNonNull(optional, "optional must not be null");
        return optional.isPresent()
            ? Option.some(NonEmptySet.singleton(optional.get()))
            : Option.none();
    }

    /**
     * Returns a {@link Collector} that accumulates a {@code Stream<T>} into an
     * {@code Option<NonEmptySet<T>>}.
     *
     * <p>Produces {@link Option#some(Object)} for a non-empty stream and {@link Option#none()}
     * for an empty stream. Duplicate elements are automatically deduplicated (set semantics).
     *
     * <p>Example:
     * <pre>{@code
     * Option<NonEmptySet<String>> roles =
     *     userRoles.stream()
     *         .collect(NonEmptySet.collector());
     * }</pre>
     *
     * @param <T> the element type
     * @return a collector producing {@code Option<NonEmptySet<T>>}
     */
    public static <T> Collector<T, ?, Option<NonEmptySet<T>>> collector() {
        return Collector.of(
            LinkedHashSet<T>::new,
            (set, t) -> set.add(Objects.requireNonNull(t, "stream elements must not be null")),
            (a, b) -> { a.addAll(b); return a; },
            set -> set.isEmpty() ? Option.none() : Option.some(fromSetUnsafe(set))
        );
    }

    /** Internal: builds from a known-non-empty set without Option wrapping. */
    private static <T> NonEmptySet<T> fromSetUnsafe(Set<? extends T> set) {
        Iterator<? extends T> it = set.iterator();
        T head = it.next();
        Set<T> tail = new LinkedHashSet<>();
        while (it.hasNext()) {
            tail.add(it.next());
        }
        return new NonEmptySet<>(head, Collections.unmodifiableSet(tail));
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the guaranteed head element of this set (the first inserted element).
     *
     * @return the head element (never {@code null})
     */
    public T head() {
        return head;
    }

    /**
     * Returns the number of elements in this set. Always &ge; 1.
     *
     * @return the size
     */
    public int size() {
        return 1 + tail.size();
    }

    /**
     * Returns {@code true} if this set contains {@code element}.
     *
     * @param element the element to test; must not be {@code null}
     * @return {@code true} if the element is present
     * @throws NullPointerException if {@code element} is {@code null}
     */
    public boolean contains(T element) {
        Objects.requireNonNull(element, "element must not be null");
        return head.equals(element) || tail.contains(element);
    }

    /**
     * Returns an unmodifiable {@link Set} containing all elements (head first, then
     * tail in insertion order). The same instance is returned on repeated calls
     * (lazily initialized, thread-safe).
     *
     * @return an unmodifiable set with all elements
     */
    public Set<T> toSet() {
        Set<T> s = cachedSet;
        if (s == null) {
            synchronized (this) {
                s = cachedSet;
                if (s == null) {
                    Set<T> result = new LinkedHashSet<>();
                    result.add(head);
                    result.addAll(tail);
                    cachedSet = s = Collections.unmodifiableSet(result);
                }
            }
        }
        return s;
    }

    /**
     * Returns a sequential {@link Stream} of all elements in insertion order.
     *
     * <p>The stream always contains at least one element (the head). Use it to bridge
     * {@code NonEmptySet} into the standard Java stream API without going through
     * {@link #toSet()}.
     *
     * @return a non-empty stream of elements in insertion order
     */
    public Stream<T> toStream() {
        return toSet().stream();
    }

    // -------------------------------------------------------------------------
    // Transformations
    // -------------------------------------------------------------------------

    /**
     * Applies {@code mapper} to every element and returns a new {@code NonEmptySet}
     * of the results. Duplicate mapped values are deduplicated; the head is always
     * the mapped value of the original head.
     *
     * @param mapper a non-null function to apply to each element; must not return {@code null}
     * @param <R>    the result element type
     * @return a new {@code NonEmptySet} of mapped values
     * @throws NullPointerException if {@code mapper} is {@code null} or returns {@code null}
     */
    public <R> NonEmptySet<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        R newHead = Objects.requireNonNull(mapper.apply(head), "mapper must not return null");
        Set<R> newTail = new LinkedHashSet<>();
        for (T element : tail) {
            R mapped = Objects.requireNonNull(mapper.apply(element), "mapper must not return null");
            if (!mapped.equals(newHead)) {
                newTail.add(mapped);
            }
        }
        return new NonEmptySet<>(newHead, Collections.unmodifiableSet(newTail));
    }

    /**
     * Returns a new {@code NonEmptySet} containing elements that satisfy {@code predicate},
     * wrapped in {@link Option#some(Object)}.
     * Returns {@link Option#none()} if no elements pass the predicate.
     *
     * @param predicate a non-null predicate to test each element
     * @return {@code Some(filteredSet)} if at least one element passes, {@code None} otherwise
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    public Option<NonEmptySet<T>> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        Set<T> result = new LinkedHashSet<>();
        if (predicate.test(head)) result.add(head);
        for (T element : tail) {
            if (predicate.test(element)) result.add(element);
        }
        if (result.isEmpty()) return Option.none();
        return Option.some(fromSetUnsafe(result));
    }

    /**
     * Returns a new {@code NonEmptySet} that is the union of this set and {@code other}.
     * The result is always non-empty since both inputs are non-empty.
     *
     * @param other the other set; must not be {@code null}
     * @return a new {@code NonEmptySet} containing all elements from both sets
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public NonEmptySet<T> union(NonEmptySet<T> other) {
        Objects.requireNonNull(other, "other must not be null");
        Set<T> combined = new LinkedHashSet<>(this.toSet());
        combined.addAll(other.toSet());
        return fromSetUnsafe(combined);
    }

    /**
     * Returns a new {@code NonEmptySet} containing only elements present in both this
     * set and {@code other}, wrapped in {@link Option#some(Object)}.
     * Returns {@link Option#none()} if the intersection is empty.
     *
     * @param other the set to intersect with; must not be {@code null}
     * @return {@code Some(intersection)} if at least one common element exists,
     *         {@code None} otherwise
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public Option<NonEmptySet<T>> intersection(Set<? extends T> other) {
        Objects.requireNonNull(other, "other must not be null");
        Set<T> result = new LinkedHashSet<>();
        if (other.contains(head)) result.add(head);
        for (T element : tail) {
            if (other.contains(element)) result.add(element);
        }
        if (result.isEmpty()) return Option.none();
        return Option.some(fromSetUnsafe(result));
    }

    /**
     * Converts this set to a {@link NonEmptyList} of its elements in insertion order.
     *
     * @return a {@code NonEmptyList<T>} with the same elements
     */
    public NonEmptyList<T> toNonEmptyList() {
        List<T> tailList = new ArrayList<>(tail);
        return NonEmptyList.of(head, tailList);
    }

    /**
     * Returns a {@link NonEmptyMap} by applying {@code valueMapper} to each element of
     * this set. Elements become keys; mapped results become values.
     * The head of this set is the head key of the returned map.
     *
     * @param valueMapper a non-null function to derive a value from each element;
     *                    must not return {@code null}
     * @param <V>         the value type
     * @return a new {@code NonEmptyMap<T, V>}
     * @throws NullPointerException if {@code valueMapper} is {@code null} or returns {@code null}
     */
    public <V> NonEmptyMap<T, V> toNonEmptyMap(Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(valueMapper, "valueMapper must not be null");
        V headVal = Objects.requireNonNull(valueMapper.apply(head), "valueMapper must not return null");
        Map<T, V> tailMap = new LinkedHashMap<>();
        for (T element : tail) {
            V val = Objects.requireNonNull(valueMapper.apply(element), "valueMapper must not return null");
            tailMap.put(element, val);
        }
        return NonEmptyMap.of(head, headVal, tailMap);
    }

    // -------------------------------------------------------------------------
    // Interoperability — sequence
    // -------------------------------------------------------------------------

    /**
     * Sequences a {@code NonEmptySet<Option<T>>} into an {@code Option<NonEmptySet<T>>}.
     *
     * <p>Returns {@link Option#some(Object)} containing all unwrapped values if every element
     * is {@code Some}; returns {@link Option#none()} as soon as any element is {@code None}
     * (fail-fast in inspection — the method stops iterating after the first {@code None};
     * elements are already materialized in the set before sequencing, so later elements are
     * not inspected but were already evaluated).
     *
     * @param <T> the unwrapped element type
     * @param nes a {@code NonEmptySet<Option<T>>}; must not be {@code null}
     * @return {@code Some(NonEmptySet<T>)} if all elements are present, {@code None} otherwise
     * @throws NullPointerException if {@code nes} is {@code null}
     */
    public static <T> Option<NonEmptySet<T>> sequenceOption(NonEmptySet<Option<T>> nes) {
        Objects.requireNonNull(nes, "nes must not be null");
        Set<T> result = new LinkedHashSet<>();
        for (Option<T> opt : nes) {
            switch (opt) {
                case Option.None<T> ignored -> { return Option.none(); }
                case Option.Some<T>(T v)   -> result.add(v);
            }
        }
        return Option.some(fromSetUnsafe(result)); // always non-empty since nes.size() >= 1
    }

    /**
     * Sequences a {@code NonEmptySet<Try<T>>} into a {@code Try<NonEmptySet<T>>}.
     *
     * <p>Returns {@link Try#success(Object)} if all elements succeed; returns
     * {@link Try#failure(Throwable)} from the first failing element (fail-fast in inspection —
     * the method stops iterating after the first failure; elements are already materialized in
     * the set before sequencing, so later elements are not inspected but were already evaluated).
     *
     * @param <T> the success value type
     * @param nes a {@code NonEmptySet<Try<T>>}; must not be {@code null}
     * @return {@code Success(NonEmptySet<T>)} if all succeed, {@code Failure} otherwise
     * @throws NullPointerException if {@code nes} is {@code null}
     */
    public static <T> Try<NonEmptySet<T>> sequenceTry(NonEmptySet<Try<T>> nes) {
        Objects.requireNonNull(nes, "nes must not be null");
        Set<T> result = new LinkedHashSet<>();
        for (Try<T> t : nes) {
            switch (t) {
                case Try.Failure<T> f -> { return Try.failure(f.cause()); }
                case Try.Success<T> s -> result.add(s.value());
            }
        }
        return Try.success(fromSetUnsafe(result)); // always non-empty since nes.size() >= 1
    }

    /**
     * Sequences a {@code NonEmptySet<Either<E, T>>} into an
     * {@code Either<E, NonEmptySet<T>>}.
     *
     * <p>Returns {@link Either#right(Object)} if all elements are right; returns
     * {@link Either#left(Object)} from the first left element (fail-fast in inspection —
     * the method stops iterating after the first left; elements are already materialized in
     * the set before sequencing, so later elements are not inspected but were already evaluated).
     *
     * @param <E> the left (error) type
     * @param <T> the right (success) type
     * @param nes a {@code NonEmptySet<Either<E, T>>}; must not be {@code null}
     * @return {@code right(NonEmptySet<T>)} if all are right, {@code left(E)} otherwise
     * @throws NullPointerException if {@code nes} is {@code null}
     */
    public static <E, T> Either<E, NonEmptySet<T>> sequenceEither(NonEmptySet<Either<E, T>> nes) {
        Objects.requireNonNull(nes, "nes must not be null");
        Set<T> result = new LinkedHashSet<>();
        for (Either<E, T> e : nes) {
            switch (e) {
                case Either.Left<E, T> l  -> { return Either.left(l.value()); }
                case Either.Right<E, T> r -> result.add(r.value());
            }
        }
        return Either.right(fromSetUnsafe(result)); // always non-empty since nes.size() >= 1
    }

    /**
     * Sequences a {@code NonEmptySet<Result<T, E>>} into a
     * {@code Result<NonEmptySet<T>, E>}.
     *
     * <p>Returns {@link Result#ok(Object)} if all elements are ok; returns
     * {@link Result#err(Object)} from the first error element (fail-fast in inspection —
     * the method stops iterating after the first err; elements are already materialized in
     * the set before sequencing, so later elements are not inspected but were already evaluated).
     *
     * @param <T> the ok value type
     * @param <E> the error type
     * @param nes a {@code NonEmptySet<Result<T, E>>}; must not be {@code null}
     * @return {@code ok(NonEmptySet<T>)} if all succeed, {@code err(E)} otherwise
     * @throws NullPointerException if {@code nes} is {@code null}
     */
    public static <T, E> Result<NonEmptySet<T>, E> sequenceResult(NonEmptySet<Result<T, E>> nes) {
        Objects.requireNonNull(nes, "nes must not be null");
        Set<T> result = new LinkedHashSet<>();
        for (Result<T, E> r : nes) {
            switch (r) {
                case Result.Err<T, E> err -> { return Result.err(err.error()); }
                case Result.Ok<T, E> ok   -> result.add(ok.value());
            }
        }
        return Result.ok(fromSetUnsafe(result)); // always non-empty since nes.size() >= 1
    }

    // -------------------------------------------------------------------------
    // Iterable
    // -------------------------------------------------------------------------

    /**
     * Returns an iterator over all elements (head first, then tail in insertion order).
     *
     * @return an iterator
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private boolean headConsumed = false;
            private final Iterator<T> tailIt = tail.iterator();

            @Override
            public boolean hasNext() {
                return !headConsumed || tailIt.hasNext();
            }

            @Override
            public T next() {
                if (!headConsumed) {
                    headConsumed = true;
                    return head;
                }
                return tailIt.next();
            }
        };
    }

    // -------------------------------------------------------------------------
    // Object
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NonEmptySet<?> other)) return false;
        return toSet().equals(other.toSet());
    }

    @Override
    public int hashCode() {
        return toSet().hashCode();
    }

    @Override
    public String toString() {
        return toSet().toString();
    }
}
