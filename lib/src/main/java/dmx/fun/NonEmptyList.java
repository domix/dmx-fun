package dmx.fun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

/**
 * An immutable, non-empty list: a list guaranteed at construction time to contain
 * at least one element.
 *
 * <p>This type makes the non-emptiness constraint part of the static type system.
 * APIs that require at least one item can declare {@code NonEmptyList<T>} instead of
 * {@code List<T>} and eliminate runtime emptiness checks entirely.
 *
 * <p>{@code NonEmptyList<T>} pairs naturally with {@link Validated}'s error accumulation,
 * which always produces at least one error when invalid.
 *
 * <p>This class is {@code @NullMarked}: all elements and parameters are non-null by default.
 *
 * @param <T> the type of elements in this list
 */
@NullMarked
public final class NonEmptyList<T> implements Iterable<T> {

    private final T head;
    private final List<T> tail;

    /**
     * Internal constructor: stores {@code tail} as-is without copying.
     * Callers must pass an already-unmodifiable list that will not be shared
     * with any other code after this call.
     */
    private NonEmptyList(T head, List<T> tail) {
        this.head = head;
        this.tail = tail;
    }

    // -------------------------------------------------------------------------
    // Smart constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code NonEmptyList} with the given head and tail.
     *
     * @param head the first (mandatory) element; must not be {@code null}
     * @param tail the remaining elements; must not be {@code null}; elements must not be {@code null}
     * @param <T>  the element type
     * @return a new {@code NonEmptyList}
     * @throws NullPointerException if {@code head}, {@code tail}, or any tail element is {@code null}
     */
    public static <T> NonEmptyList<T> of(T head, List<? extends T> tail) {
        Objects.requireNonNull(head, "head must not be null");
        Objects.requireNonNull(tail, "tail must not be null");
        tail.forEach(e -> Objects.requireNonNull(e, "tail elements must not be null"));
        return new NonEmptyList<>(head, List.copyOf(tail));
    }

    /**
     * Creates a {@code NonEmptyList} containing exactly one element.
     *
     * @param head the sole element; must not be {@code null}
     * @param <T>  the element type
     * @return a singleton {@code NonEmptyList}
     * @throws NullPointerException if {@code head} is {@code null}
     */
    public static <T> NonEmptyList<T> singleton(T head) {
        Objects.requireNonNull(head, "head must not be null");
        return new NonEmptyList<>(head, List.of());
    }

    /**
     * Attempts to construct a {@code NonEmptyList} from a plain {@link List}.
     *
     * @param list the source list; must not be {@code null}; elements must not be {@code null}
     * @param <T>  the element type
     * @return {@link Option#some(Object)} wrapping the {@code NonEmptyList} if the list is
     *         non-empty, or {@link Option#none()} if the list is empty
     * @throws NullPointerException if {@code list} or any element is {@code null}
     */
    public static <T> Option<NonEmptyList<T>> fromList(List<? extends T> list) {
        Objects.requireNonNull(list, "list must not be null");
        if (list.isEmpty()) {
            return Option.none();
        }
        list.forEach(e -> Objects.requireNonNull(e, "list elements must not be null"));
        T head = list.get(0);
        List<T> tail = List.copyOf(list.subList(1, list.size()));
        return Option.some(new NonEmptyList<>(head, tail));
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the first element of this list.
     *
     * @return the head element (never {@code null})
     */
    public T head() {
        return head;
    }

    /**
     * Returns an unmodifiable view of all elements after the head.
     * May be empty if this is a singleton list.
     *
     * @return the tail (never {@code null}, may be empty)
     */
    public List<T> tail() {
        return tail;
    }

    /**
     * Returns an unmodifiable {@link List} containing all elements (head followed by tail).
     *
     * @return a new unmodifiable list with all elements
     */
    public List<T> toList() {
        List<T> result = new ArrayList<>(1 + tail.size());
        result.add(head);
        result.addAll(tail);
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the number of elements in this list. Always &ge; 1.
     *
     * @return the size
     */
    public int size() {
        return 1 + tail.size();
    }

    // -------------------------------------------------------------------------
    // Transformations
    // -------------------------------------------------------------------------

    /**
     * Applies {@code mapper} to every element and returns a new {@code NonEmptyList}
     * of the results. The structure (head/tail split) is preserved.
     *
     * @param mapper a non-null function to apply to each element; must not return {@code null}
     * @param <R>    the result element type
     * @return a new {@code NonEmptyList} of mapped values
     * @throws NullPointerException if {@code mapper} is {@code null} or returns {@code null}
     */
    public <R> NonEmptyList<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        R newHead = Objects.requireNonNull(mapper.apply(head), "mapper must not return null");
        List<R> newTail = new ArrayList<>(tail.size());
        for (T element : tail) {
            newTail.add(Objects.requireNonNull(mapper.apply(element), "mapper must not return null"));
        }
        return new NonEmptyList<>(newHead, Collections.unmodifiableList(newTail));
    }

    /**
     * Returns a new {@code NonEmptyList} with {@code element} appended at the end.
     *
     * @param element the element to append; must not be {@code null}
     * @return a new {@code NonEmptyList} with the element appended
     * @throws NullPointerException if {@code element} is {@code null}
     */
    public NonEmptyList<T> append(T element) {
        Objects.requireNonNull(element, "element must not be null");
        List<T> newTail = new ArrayList<>(tail.size() + 1);
        newTail.addAll(tail);
        newTail.add(element);
        return new NonEmptyList<>(head, Collections.unmodifiableList(newTail));
    }

    /**
     * Returns a new {@code NonEmptyList} with {@code element} prepended at the front.
     *
     * @param element the element to prepend; must not be {@code null}
     * @return a new {@code NonEmptyList} with the element prepended
     * @throws NullPointerException if {@code element} is {@code null}
     */
    public NonEmptyList<T> prepend(T element) {
        Objects.requireNonNull(element, "element must not be null");
        List<T> newTail = new ArrayList<>(1 + tail.size());
        newTail.add(head);
        newTail.addAll(tail);
        return new NonEmptyList<>(element, Collections.unmodifiableList(newTail));
    }

    /**
     * Returns a new {@code NonEmptyList} that is the concatenation of this list and {@code other}.
     *
     * @param other the list to append; must not be {@code null}
     * @return a new {@code NonEmptyList} containing all elements of both lists
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public NonEmptyList<T> concat(NonEmptyList<T> other) {
        Objects.requireNonNull(other, "other must not be null");
        List<T> newTail = new ArrayList<>(tail.size() + other.size());
        newTail.addAll(tail);
        newTail.add(other.head);
        newTail.addAll(other.tail);
        return new NonEmptyList<>(head, Collections.unmodifiableList(newTail));
    }

    // -------------------------------------------------------------------------
    // Interop
    // -------------------------------------------------------------------------

    /**
     * Converts this list to a {@link NonEmptySet}, preserving the head element and
     * deduplicating the tail in insertion order.
     * Duplicate elements are silently dropped; the head is always retained.
     *
     * @return a {@code NonEmptySet<T>} with the same distinct elements
     */
    public NonEmptySet<T> toNonEmptySet() {
        Set<T> tailAsSet = new LinkedHashSet<>(tail);
        return NonEmptySet.of(head, tailAsSet);
    }

    /**
     * Returns a sequential {@link Stream} of all elements (head first, then tail).
     * Does not materialize an intermediate list.
     *
     * @return a {@code Stream<T>} over all elements
     */
    public Stream<T> toStream() {
        return Stream.concat(Stream.of(head), tail.stream());
    }

    /**
     * Returns a {@link Collector} that accumulates a {@link Stream}{@code <T>} into an
     * {@link Option}{@code <NonEmptyList<T>>}.
     *
     * <p>Produces {@link Option#some(Object)} if the stream has at least one element,
     * or {@link Option#none()} if the stream is empty.
     *
     * @param <T> the element type
     * @return a {@code Collector} producing {@code Option<NonEmptyList<T>>}
     */
    public static <T> Collector<T, ?, Option<NonEmptyList<T>>> collector() {
        return Collector.of(
            ArrayList<T>::new,
            ArrayList::add,
            (a, b) -> { a.addAll(b); return a; },
            list -> NonEmptyList.<T>fromList(list)
        );
    }

    /**
     * Alias for {@link #collector()}.
     *
     * <p>Returns a {@link Collector} that accumulates a {@link Stream}{@code <T>} into an
     * {@link Option}{@code <NonEmptyList<T>>}. Produces {@link Option#some(Object)} if the stream
     * has at least one element, or {@link Option#none()} if the stream is empty.
     *
     * <pre>{@code
     * Option<NonEmptyList<String>> tags =
     *     Stream.of("java", "fp", "dmx-fun")
     *           .collect(NonEmptyList.toNonEmptyList());
     * // Some(["java", "fp", "dmx-fun"])
     *
     * Option<NonEmptyList<String>> empty =
     *     Stream.<String>empty()
     *           .collect(NonEmptyList.toNonEmptyList());
     * // None
     * }</pre>
     *
     * @param <T> the element type
     * @return a {@code Collector} producing {@code Option<NonEmptyList<T>>}
     */
    public static <T> Collector<T, ?, Option<NonEmptyList<T>>> toNonEmptyList() {
        return collector();
    }

    /**
     * Sequences a {@code NonEmptyList} of {@link Option}s into an {@code Option} of a
     * {@code NonEmptyList}.
     *
     * <p>Returns {@link Option#some(Object)} containing all unwrapped values if every element
     * is {@link Option#some(Object)}; returns {@link Option#none()} as soon as any element is
     * {@link Option#none()}.
     *
     * @param <T> the element type
     * @param nel a {@code NonEmptyList<Option<T>>}; must not be {@code null}
     * @return {@code Some(NonEmptyList<T>)} if all options are present, {@code None} otherwise
     * @throws NullPointerException if {@code nel} is {@code null}
     */
    public static <T> Option<NonEmptyList<T>> sequence(NonEmptyList<Option<T>> nel) {
        Objects.requireNonNull(nel, "nel must not be null");
        List<T> values = new ArrayList<>(nel.size());
        for (Option<T> opt : nel) {
            switch (opt) {
                case Option.None<T> ignored -> { return Option.none(); }
                case Option.Some<T>(T v)   -> values.add(v);
            }
        }
        // values.size() == nel.size() >= 1, so fromList always returns Some
        return NonEmptyList.fromList(values);
    }

    // -------------------------------------------------------------------------
    // Iterable
    // -------------------------------------------------------------------------

    /**
     * Returns an iterator over all elements (head first, then tail).
     * Does not materialize an intermediate list.
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
        if (!(obj instanceof NonEmptyList<?> other)) return false;
        return toList().equals(other.toList());
    }

    @Override
    public int hashCode() {
        return toList().hashCode();
    }

    @Override
    public String toString() {
        return toList().toString();
    }
}
