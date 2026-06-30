package dmx.fun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.BinaryOperator;
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
 * <p>The rationale for introducing dedicated non-empty collection types instead of using
 * standard JDK types with runtime checks is documented in
 * <a href="https://domix.github.io/dmx-fun/adr/adr-018-non-empty-collections/">
 * ADR-018 — NonEmptyList&lt;T&gt;, NonEmptySet&lt;T&gt;, NonEmptyMap&lt;K,V&gt; as structural guarantee types</a>.
 *
 * @param <T> the type of elements in this list
 */
@NullMarked
public final class NonEmptyList<T> implements SequencedCollection<T> {

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
        var tail = List.<T>copyOf(list.subList(1, list.size()));
        return Option.some(new NonEmptyList<>(head, tail));
    }

    /**
     * Attempts to construct a singleton {@code NonEmptyList} from a JDK {@link Optional}.
     *
     * @param optional the source optional; must not be {@code null}
     * @param <T>      the element type
     * @return {@link Option#some(Object)} wrapping a singleton {@code NonEmptyList} if the
     *         optional is present, or {@link Option#none()} if the optional is empty
     * @throws NullPointerException if {@code optional} is {@code null}
     */
    public static <T> Option<NonEmptyList<T>> fromOptional(Optional<? extends T> optional) {
        Objects.requireNonNull(optional, "optional must not be null");
        return optional.isPresent()
            ? Option.some(NonEmptyList.singleton(optional.get()))
            : Option.none();
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
     * Returns the first element of this list. Alias for {@link #head()}.
     *
     * <p>Implements {@link SequencedCollection#getFirst()}.
     *
     * @return the first element (never {@code null})
     */
    @Override
    public T getFirst() {
        return head;
    }

    /**
     * Returns the last element of this list.
     *
     * <p>Implements {@link SequencedCollection#getLast()}.
     *
     * @return the last element (never {@code null})
     */
    @Override
    public T getLast() {
        return tail.isEmpty() ? head : tail.get(tail.size() - 1);
    }

    /**
     * Returns a new {@code NonEmptyList} with all elements in reverse order.
     *
     * <p>Implements {@link SequencedCollection#reversed()}.
     *
     * @return a reversed copy of this list
     */
    @Override
    public NonEmptyList<T> reversed() {
        var all = new ArrayList<>(toList());
        Collections.reverse(all);
        T newHead = all.get(0);
        var newTail = List.copyOf(all.subList(1, all.size()));
        return new NonEmptyList<>(newHead, newTail);
    }

    /**
     * Unsupported — {@code NonEmptyList} is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void addFirst(T t) {
        throw new UnsupportedOperationException("NonEmptyList is immutable");
    }

    /**
     * Unsupported — {@code NonEmptyList} is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void addLast(T t) {
        throw new UnsupportedOperationException("NonEmptyList is immutable");
    }

    /**
     * Unsupported — {@code NonEmptyList} is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public T removeFirst() {
        throw new UnsupportedOperationException("NonEmptyList is immutable");
    }

    /**
     * Unsupported — {@code NonEmptyList} is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public T removeLast() {
        throw new UnsupportedOperationException("NonEmptyList is immutable");
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
        var result = new ArrayList<T>(1 + tail.size());
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
        var newTail = new ArrayList<R>(tail.size());
        for (T element : tail) {
            newTail.add(Objects.requireNonNull(mapper.apply(element), "mapper must not return null"));
        }
        return new NonEmptyList<>(newHead, Collections.unmodifiableList(newTail));
    }

    /**
     * Combines all elements into a single value using {@code accumulator}, left to right,
     * starting from the head. Because a {@code NonEmptyList} always has at least one element,
     * this returns a {@code T} directly — there is no {@code Optional} and no emptiness check,
     * unlike {@link Stream#reduce(java.util.function.BinaryOperator)}.
     *
     * <p>For a single-element list the {@code accumulator} is never invoked and the head is
     * returned as-is.
     *
     * <p>Example:
     * <pre>{@code
     * NonEmptyList.of(1, List.of(2, 3)).reduce(Integer::sum);      // 6
     * errors.reduce((a, b) -> a + "; " + b);                       // joined messages
     * }</pre>
     *
     * @param accumulator an associative function combining two elements; must not be {@code null}
     * @return the combined value
     * @throws NullPointerException if {@code accumulator} is {@code null}
     */
    public T reduce(BinaryOperator<T> accumulator) {
        Objects.requireNonNull(accumulator, "accumulator must not be null");
        T result = head;
        for (T element : tail) {
            result = accumulator.apply(result, element);
        }
        return result;
    }

    /**
     * Joins the elements into a single {@link String}, separated by {@code ", "}.
     *
     * <p>Equivalent to {@code joinToString(", ")}.
     *
     * @return the joined string
     */
    public String joinToString() {
        return joinToString(", ");
    }

    /**
     * Joins the elements into a single {@link String}, separated by {@code delimiter}, using
     * {@link String#valueOf(Object)} to render each element.
     *
     * @param delimiter the separator placed between elements; must not be {@code null}
     * @return the joined string
     * @throws NullPointerException if {@code delimiter} is {@code null}
     */
    public String joinToString(CharSequence delimiter) {
        return joinToString(delimiter, String::valueOf);
    }

    /**
     * Joins the elements into a single {@link String}, separated by {@code delimiter}, rendering
     * each element with {@code transform}.
     *
     * <p>Example:
     * <pre>{@code
     * NonEmptyList.of("must not be blank", List.of("too short")).joinToString("; ");
     * // "must not be blank; too short"
     * users.joinToString(",", User::name);
     * }</pre>
     *
     * @param delimiter the separator placed between elements; must not be {@code null}
     * @param transform renders each element as a {@link CharSequence}; must not be {@code null}
     *                  and must not return {@code null}
     * @return the joined string
     * @throws NullPointerException if {@code delimiter} or {@code transform} is {@code null},
     *                              or if {@code transform} returns {@code null} for any element
     */
    public String joinToString(CharSequence delimiter, Function<? super T, ? extends CharSequence> transform) {
        Objects.requireNonNull(delimiter, "delimiter must not be null");
        Objects.requireNonNull(transform, "transform must not be null");
        StringBuilder sb = new StringBuilder()
            .append(Objects.requireNonNull(transform.apply(head), "transform must not return null"));
        for (T element : tail) {
            sb.append(delimiter)
                .append(Objects.requireNonNull(transform.apply(element), "transform must not return null"));
        }
        return sb.toString();
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
        var newTail = new ArrayList<T>(tail.size() + 1);
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
        var newTail = new ArrayList<T>(1 + tail.size());
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
        var newTail = new ArrayList<T>(tail.size() + other.size());
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
        var tailAsSet = new LinkedHashSet<>(tail);
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
            NonEmptyList::fromList
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
        var values = new ArrayList<T>(nel.size());
        for (Option<T> opt : nel) {
            switch (opt) {
                case Option.None<T> ignored -> { return Option.none(); }
                case Option.Some<T>(T v)   -> values.add(v);
            }
        }
        // values.size() == nel.size() >= 1, so fromList always returns Some
        return NonEmptyList.fromList(values);
    }

    /**
     * Sequences a {@code NonEmptyList} of {@link Try}s into a {@code Try} of a
     * {@code NonEmptyList}.
     *
     * <p>Returns {@link Try#success(Object)} containing all unwrapped values if every element
     * is a success; returns {@link Try#failure(Throwable)} from the first failing element
     * (fail-fast in inspection — the method stops iterating after the first failure; elements
     * are already materialized in the list before sequencing, so later elements are not inspected
     * but were already evaluated).
     *
     * @param <T> the success value type
     * @param nel a {@code NonEmptyList<Try<T>>}; must not be {@code null}
     * @return {@code Success(NonEmptyList<T>)} if all succeed, {@code Failure} from the first
     *         failing element otherwise
     * @throws NullPointerException if {@code nel} is {@code null}
     */
    public static <T> Try<NonEmptyList<T>> sequenceTry(NonEmptyList<Try<T>> nel) {
        Objects.requireNonNull(nel, "nel must not be null");
        var values = new ArrayList<T>(nel.size());
        for (Try<T> t : nel) {
            switch (t) {
                case Try.Failure<T> f -> { return Try.failure(f.cause()); }
                case Try.Success<T> s -> values.add(s.value());
            }
        }
        return Try.success(fromList(values).get()); // always Some since nel.size() >= 1
    }

    /**
     * Sequences a {@code NonEmptyList} of {@link Either}s into an {@code Either} of a
     * {@code NonEmptyList}.
     *
     * <p>Returns {@link Either#right(Object)} containing all unwrapped values if every element
     * is right; returns {@link Either#left(Object)} from the first left element
     * (fail-fast in inspection — the method stops iterating after the first left; elements
     * are already materialized in the list before sequencing, so later elements are not inspected
     * but were already evaluated).
     *
     * @param <E> the left (error) type
     * @param <T> the right (success) type
     * @param nel a {@code NonEmptyList<Either<E, T>>}; must not be {@code null}
     * @return {@code right(NonEmptyList<T>)} if all are right, {@code left(E)} from the first
     *         left element otherwise
     * @throws NullPointerException if {@code nel} is {@code null}
     */
    public static <E, T> Either<E, NonEmptyList<T>> sequenceEither(NonEmptyList<Either<E, T>> nel) {
        Objects.requireNonNull(nel, "nel must not be null");
        var values = new ArrayList<T>(nel.size());
        for (Either<E, T> e : nel) {
            switch (e) {
                case Either.Left<E, T> l  -> { return Either.left(l.value()); }
                case Either.Right<E, T> r -> values.add(r.value());
            }
        }
        return Either.right(fromList(values).get()); // always Some since nel.size() >= 1
    }

    /**
     * Sequences a {@code NonEmptyList} of {@link Result}s into a {@code Result} of a
     * {@code NonEmptyList}.
     *
     * <p>Returns {@link Result#ok(Object)} containing all unwrapped values if every element
     * is ok; returns {@link Result#err(Object)} from the first error element
     * (fail-fast in inspection — the method stops iterating after the first err; elements
     * are already materialized in the list before sequencing, so later elements are not inspected
     * but were already evaluated).
     *
     * @param <T> the ok value type
     * @param <E> the error type
     * @param nel a {@code NonEmptyList<Result<T, E>>}; must not be {@code null}
     * @return {@code ok(NonEmptyList<T>)} if all succeed, {@code err(E)} from the first
     *         error element otherwise
     * @throws NullPointerException if {@code nel} is {@code null}
     */
    public static <T, E> Result<NonEmptyList<T>, E> sequenceResult(NonEmptyList<Result<T, E>> nel) {
        Objects.requireNonNull(nel, "nel must not be null");
        var values = new ArrayList<T>(nel.size());
        for (Result<T, E> r : nel) {
            switch (r) {
                case Result.Err<T, E> err -> { return Result.err(err.error()); }
                case Result.Ok<T, E> ok   -> values.add(ok.value());
            }
        }
        return Result.ok(fromList(values).get()); // always Some since nel.size() >= 1
    }

    // -------------------------------------------------------------------------
    // Collection (required by SequencedCollection)
    // -------------------------------------------------------------------------

    /** Always {@code false} — a {@code NonEmptyList} always has at least one element. */
    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return head.equals(o) || tail.contains(o);
    }

    @Override
    public boolean containsAll(java.util.Collection<?> c) {
        return toList().containsAll(c);
    }

    @Override
    public Object[] toArray() {
        return toList().toArray();
    }

    @Override
    public <E> E[] toArray(E[] a) {
        return toList().toArray(a);
    }

    /** @throws UnsupportedOperationException always — {@code NonEmptyList} is immutable */
    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException("NonEmptyList is immutable");
    }

    /** @throws UnsupportedOperationException always — {@code NonEmptyList} is immutable */
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("NonEmptyList is immutable");
    }

    /** @throws UnsupportedOperationException always — {@code NonEmptyList} is immutable */
    @Override
    public boolean addAll(java.util.Collection<? extends T> c) {
        throw new UnsupportedOperationException("NonEmptyList is immutable");
    }

    /** @throws UnsupportedOperationException always — {@code NonEmptyList} is immutable */
    @Override
    public boolean removeAll(java.util.Collection<?> c) {
        throw new UnsupportedOperationException("NonEmptyList is immutable");
    }

    /** @throws UnsupportedOperationException always — {@code NonEmptyList} is immutable */
    @Override
    public boolean retainAll(java.util.Collection<?> c) {
        throw new UnsupportedOperationException("NonEmptyList is immutable");
    }

    /** @throws UnsupportedOperationException always — {@code NonEmptyList} is immutable */
    @Override
    public boolean removeIf(java.util.function.Predicate<? super T> filter) {
        throw new UnsupportedOperationException("NonEmptyList is immutable");
    }

    /** @throws UnsupportedOperationException always — {@code NonEmptyList} is immutable */
    @Override
    public void clear() {
        throw new UnsupportedOperationException("NonEmptyList is immutable");
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
