package dmx.fun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

/**
 * An immutable, non-empty map: a map guaranteed at construction time to contain
 * at least one entry.
 *
 * <p>This type makes the non-emptiness constraint part of the static type system.
 * APIs that require at least one entry can declare {@code NonEmptyMap<K, V>} instead of
 * {@code Map<K, V>} and eliminate runtime emptiness checks entirely.
 *
 * <p>Insertion order is preserved (backed by {@link LinkedHashMap}). The first entry
 * inserted is the {@link #headKey()} / {@link #headValue()}.
 *
 * <p>This class is {@code @NullMarked}: all keys, values, and parameters are non-null
 * by default.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
@NullMarked
public final class NonEmptyMap<K, V> {

    private final K headKey;
    private final V headValue;
    private final Map<K, V> tail; // unmodifiable, does NOT include headKey

    private transient volatile Map<K, V> cachedMap;

    private NonEmptyMap(K headKey, V headValue, Map<K, V> tail) {
        this.headKey   = headKey;
        this.headValue = headValue;
        this.tail      = tail;
    }

    // -------------------------------------------------------------------------
    // Smart constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code NonEmptyMap} with the given head entry and additional entries.
     *
     * <p>If {@code rest} contains {@code key}, that duplicate is ignored — the provided
     * {@code value} is always used for the head key.
     *
     * @param key   the head key; must not be {@code null}
     * @param value the head value; must not be {@code null}
     * @param rest  additional entries; must not be {@code null}; keys and values must not be {@code null}
     * @param <K>   the key type
     * @param <V>   the value type
     * @return a new {@code NonEmptyMap}
     * @throws NullPointerException if {@code key}, {@code value}, {@code rest}, or any
     *                              key/value inside {@code rest} is {@code null}
     */
    public static <K, V> NonEmptyMap<K, V> of(K key, V value, Map<? extends K, ? extends V> rest) {
        Objects.requireNonNull(key,   "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(rest,  "rest must not be null");
        Map<K, V> tail = new LinkedHashMap<>();
        rest.forEach((k, v) -> {
            Objects.requireNonNull(k, "rest keys must not be null");
            Objects.requireNonNull(v, "rest values must not be null");
            if (!k.equals(key)) {
                tail.put(k, v);
            }
        });
        return new NonEmptyMap<>(key, value, Collections.unmodifiableMap(tail));
    }

    /**
     * Creates a {@code NonEmptyMap} containing exactly one entry.
     *
     * @param key   the sole key; must not be {@code null}
     * @param value the sole value; must not be {@code null}
     * @param <K>   the key type
     * @param <V>   the value type
     * @return a singleton {@code NonEmptyMap}
     * @throws NullPointerException if {@code key} or {@code value} is {@code null}
     */
    public static <K, V> NonEmptyMap<K, V> singleton(K key, V value) {
        Objects.requireNonNull(key,   "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        return new NonEmptyMap<>(key, value, Map.of());
    }

    /**
     * Attempts to construct a {@code NonEmptyMap} from a plain {@link Map}.
     *
     * @param map  the source map; must not be {@code null}; keys and values must not be {@code null}
     * @param <K>  the key type
     * @param <V>  the value type
     * @return {@link Option#some(Object)} wrapping the {@code NonEmptyMap} if the map is
     *         non-empty, or {@link Option#none()} if the map is empty
     * @throws NullPointerException if {@code map} or any key/value is {@code null}
     */
    public static <K, V> Option<NonEmptyMap<K, V>> fromMap(Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map must not be null");
        if (map.isEmpty()) {
            return Option.none();
        }
        map.forEach((k, v) -> {
            Objects.requireNonNull(k, "map keys must not be null");
            Objects.requireNonNull(v, "map values must not be null");
        });
        return Option.some(fromMapUnsafe(map));
    }

    /**
     * Attempts to construct a {@code NonEmptyMap} from a JDK {@link Optional} wrapping a
     * plain {@link Map}.
     *
     * <p>If the optional is present and the wrapped map is non-empty, returns
     * {@link Option#some(Object)} wrapping the {@code NonEmptyMap}. If the optional is empty,
     * or if the wrapped map is itself empty, returns {@link Option#none()}.
     *
     * @param optional the source optional; must not be {@code null}
     * @param <K>      the key type
     * @param <V>      the value type
     * @return {@link Option#some(Object)} if the optional is present and the map is non-empty,
     *         {@link Option#none()} otherwise
     * @throws NullPointerException if {@code optional} is {@code null}
     */
    public static <K, V> Option<NonEmptyMap<K, V>> fromOptional(
            Optional<? extends Map<? extends K, ? extends V>> optional) {
        Objects.requireNonNull(optional, "optional must not be null");
        return optional.isPresent() ? fromMap(optional.get()) : Option.none();
    }

    /**
     * Returns a {@link Collector} that accumulates a {@code Stream<T>} into an
     * {@code Option<NonEmptyMap<K, V>>} using the provided key and value extractor functions.
     *
     * <p>Produces {@link Option#some(Object)} for a non-empty stream and {@link Option#none()}
     * for an empty stream. If multiple stream elements map to the same key, later elements
     * overwrite earlier ones (last-write-wins semantics).
     *
     * <p>Example:
     * <pre>{@code
     * Option<NonEmptyMap<String, Integer>> result =
     *     employees.stream()
     *         .collect(NonEmptyMap.collector(Employee::name, Employee::score));
     * }</pre>
     *
     * @param <T>         the stream element type
     * @param <K>         the key type
     * @param <V>         the value type
     * @param keyMapper   extracts the key from each element; must not be {@code null} or return
     *                    {@code null}
     * @param valueMapper extracts the value from each element; must not be {@code null} or return
     *                    {@code null}
     * @return a collector producing {@code Option<NonEmptyMap<K, V>>}
     * @throws NullPointerException if {@code keyMapper} or {@code valueMapper} is {@code null}
     */
    public static <T, K, V> Collector<T, ?, Option<NonEmptyMap<K, V>>> collector(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper,   "keyMapper must not be null");
        Objects.requireNonNull(valueMapper, "valueMapper must not be null");
        return Collector.of(
            LinkedHashMap<K, V>::new,
            (map, t) -> map.put(
                Objects.requireNonNull(keyMapper.apply(t),   "keyMapper must not return null"),
                Objects.requireNonNull(valueMapper.apply(t), "valueMapper must not return null")
            ),
            (a, b) -> { a.putAll(b); return a; },
            map -> NonEmptyMap.fromMap(map)
        );
    }

    /** Internal: builds from a known-non-empty map without Option wrapping. */
    private static <K, V> NonEmptyMap<K, V> fromMapUnsafe(Map<? extends K, ? extends V> map) {
        Iterator<? extends Map.Entry<? extends K, ? extends V>> it = map.entrySet().iterator();
        Map.Entry<? extends K, ? extends V> first = it.next();
        K headKey   = first.getKey();
        V headValue = first.getValue();
        Map<K, V> tail = new LinkedHashMap<>();
        while (it.hasNext()) {
            Map.Entry<? extends K, ? extends V> entry = it.next();
            tail.put(entry.getKey(), entry.getValue());
        }
        return new NonEmptyMap<>(headKey, headValue, Collections.unmodifiableMap(tail));
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the guaranteed head key of this map.
     *
     * @return the head key (never {@code null})
     */
    public K headKey() {
        return headKey;
    }

    /**
     * Returns the value associated with the head key.
     *
     * @return the head value (never {@code null})
     */
    public V headValue() {
        return headValue;
    }

    /**
     * Returns the number of entries in this map. Always &ge; 1.
     *
     * @return the size
     */
    public int size() {
        return 1 + tail.size();
    }

    /**
     * Returns the value associated with {@code key}, or {@link Option#none()} if absent.
     *
     * @param key the key to look up; must not be {@code null}
     * @return {@code Some(value)} if the key is present, {@code None} otherwise
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public Option<V> get(K key) {
        Objects.requireNonNull(key, "key must not be null");
        if (headKey.equals(key)) return Option.some(headValue);
        return Option.ofNullable(tail.get(key));
    }

    /**
     * Returns {@code true} if this map contains an entry for {@code key}.
     *
     * @param key the key to test; must not be {@code null}
     * @return {@code true} if the key is present
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "key must not be null");
        return headKey.equals(key) || tail.containsKey(key);
    }

    /**
     * Returns a {@link NonEmptySet} containing all keys of this map in insertion order.
     * The head key of this map is the head of the returned set.
     *
     * @return a {@code NonEmptySet<K>} of all keys (never {@code null})
     */
    public NonEmptySet<K> keySet() {
        return NonEmptySet.of(headKey, tail.keySet());
    }

    /**
     * Returns a {@link NonEmptyList} containing all values of this map in insertion order.
     * The head value of this map is the head of the returned list.
     * Duplicate values are preserved (unlike keys, values need not be unique).
     *
     * @return a {@code NonEmptyList<V>} of all values (never {@code null})
     */
    public NonEmptyList<V> values() {
        return NonEmptyList.of(headValue, new ArrayList<>(tail.values()));
    }

    /**
     * Returns an unmodifiable {@link Map} containing all entries (head entry first,
     * then tail entries in insertion order). The same instance is returned on repeated
     * calls (lazily initialized, thread-safe).
     *
     * @return an unmodifiable map with all entries
     */
    public Map<K, V> toMap() {
        Map<K, V> m = cachedMap;
        if (m == null) {
            synchronized (this) {
                m = cachedMap;
                if (m == null) {
                    Map<K, V> result = new LinkedHashMap<>();
                    result.put(headKey, headValue);
                    result.putAll(tail);
                    cachedMap = m = Collections.unmodifiableMap(result);
                }
            }
        }
        return m;
    }

    /**
     * Returns a sequential {@link Stream} of all entries in insertion order.
     *
     * <p>The stream always contains at least one element (the head entry). Use it to bridge
     * {@code NonEmptyMap} into the standard Java stream API.
     *
     * @return a non-empty stream of {@link Map.Entry} elements in insertion order
     */
    public Stream<Map.Entry<K, V>> toStream() {
        return toMap().entrySet().stream();
    }

    // -------------------------------------------------------------------------
    // Transformations
    // -------------------------------------------------------------------------

    /**
     * Applies {@code mapper} to every value and returns a new {@code NonEmptyMap}
     * with the same keys and mapped values.
     *
     * @param mapper a non-null function to apply to each value; must not return {@code null}
     * @param <R>    the result value type
     * @return a new {@code NonEmptyMap} with mapped values
     * @throws NullPointerException if {@code mapper} is {@code null} or returns {@code null}
     */
    public <R> NonEmptyMap<K, R> mapValues(Function<? super V, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        R newHead = Objects.requireNonNull(mapper.apply(headValue), "mapper must not return null");
        Map<K, R> newTail = new LinkedHashMap<>();
        tail.forEach((k, v) ->
            newTail.put(k, Objects.requireNonNull(mapper.apply(v), "mapper must not return null")));
        return new NonEmptyMap<>(headKey, newHead, Collections.unmodifiableMap(newTail));
    }

    /**
     * Applies {@code mapper} to every key-value pair and returns a new {@code NonEmptyMap}
     * with the same keys and mapped values.
     *
     * <p>Unlike {@link #mapValues(Function)}, the mapper receives both the key and the value,
     * enabling value transformations that depend on the key.
     *
     * @param mapper a non-null function to apply to each key-value pair; must not return
     *               {@code null}
     * @param <R>    the result value type
     * @return a new {@code NonEmptyMap} with mapped values
     * @throws NullPointerException if {@code mapper} is {@code null} or returns {@code null}
     */
    public <R> NonEmptyMap<K, R> mapValuesWithKey(BiFunction<? super K, ? super V, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        R newHead = Objects.requireNonNull(mapper.apply(headKey, headValue), "mapper must not return null");
        Map<K, R> newTail = new LinkedHashMap<>();
        tail.forEach((k, v) ->
            newTail.put(k, Objects.requireNonNull(mapper.apply(k, v), "mapper must not return null")));
        return new NonEmptyMap<>(headKey, newHead, Collections.unmodifiableMap(newTail));
    }

    /**
     * Applies {@code mapper} to every key and returns a new {@code NonEmptyMap}
     * with the mapped keys and the original values.
     *
     * <p>If multiple keys map to the same new key, <strong>head-wins</strong> semantics apply:
     * the head entry is always preserved, and any tail entry whose mapped key collides with
     * the mapped head key is silently dropped.
     *
     * @param mapper a non-null function to apply to each key; must not return {@code null}
     * @param <R>    the result key type
     * @return a new {@code NonEmptyMap} with mapped keys
     * @throws NullPointerException if {@code mapper} is {@code null} or returns {@code null}
     */
    public <R> NonEmptyMap<R, V> mapKeys(Function<? super K, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        R newHeadKey = Objects.requireNonNull(mapper.apply(headKey), "mapper must not return null");
        Map<R, V> newTail = new LinkedHashMap<>();
        tail.forEach((k, v) -> {
            R mapped = Objects.requireNonNull(mapper.apply(k), "mapper must not return null");
            if (!mapped.equals(newHeadKey)) {
                newTail.put(mapped, v);
            }
        });
        return new NonEmptyMap<>(newHeadKey, headValue, Collections.unmodifiableMap(newTail));
    }

    /**
     * Returns a new {@code NonEmptyMap} containing only entries for which {@code predicate}
     * returns {@code true}, wrapped in {@link Option#some(Object)}.
     * Returns {@link Option#none()} if no entries pass the predicate.
     *
     * @param predicate a non-null predicate to test each key-value pair
     * @return {@code Some(filteredMap)} if at least one entry passes, {@code None} otherwise
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    public Option<NonEmptyMap<K, V>> filter(BiPredicate<? super K, ? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        Map<K, V> result = new LinkedHashMap<>();
        if (predicate.test(headKey, headValue)) result.put(headKey, headValue);
        tail.forEach((k, v) -> { if (predicate.test(k, v)) result.put(k, v); });
        return NonEmptyMap.fromMap(result);
    }

    /**
     * Returns a new {@code NonEmptyMap} that is the union of this map and {@code other}.
     * When both maps contain the same key, {@code mergeFunction} is applied to the two values.
     *
     * @param other         the other map; must not be {@code null}
     * @param mergeFunction function to resolve value conflicts; must not be {@code null};
     *                      must not return {@code null} (a null return would violate the
     *                      non-null value contract and is rejected immediately)
     * @return a new {@code NonEmptyMap} containing all entries from both maps
     * @throws NullPointerException if {@code other}, {@code mergeFunction}, or the result
     *                              of {@code mergeFunction} is {@code null}
     */
    public NonEmptyMap<K, V> merge(NonEmptyMap<K, V> other, BinaryOperator<V> mergeFunction) {
        Objects.requireNonNull(other,         "other must not be null");
        Objects.requireNonNull(mergeFunction, "mergeFunction must not be null");
        Map<K, V> combined = new LinkedHashMap<>(this.toMap());
        other.toMap().forEach((k, v) -> combined.merge(k, v,
            (oldVal, newVal) -> Objects.requireNonNull(
                mergeFunction.apply(oldVal, newVal), "mergeFunction must not return null")));
        return fromMapUnsafe(combined);
    }

    /**
     * Converts this map to a {@link NonEmptyList} of its entries in insertion order.
     *
     * @return a {@code NonEmptyList<Map.Entry<K, V>>}
     */
    public NonEmptyList<Map.Entry<K, V>> toNonEmptyList() {
        List<Map.Entry<K, V>> entries = new ArrayList<>(toMap().entrySet());
        Map.Entry<K, V> head = entries.get(0);
        List<Map.Entry<K, V>> tailList = entries.subList(1, entries.size());
        return NonEmptyList.of(head, List.copyOf(tailList));
    }

    // -------------------------------------------------------------------------
    // Interoperability — sequence
    // -------------------------------------------------------------------------

    /**
     * Sequences a {@code NonEmptyMap<K, Option<V>>} into an {@code Option<NonEmptyMap<K, V>>}.
     *
     * <p>Returns {@link Option#some(Object)} containing all unwrapped values if every entry's
     * value is {@code Some}; returns {@link Option#none()} as soon as any entry's value is
     * {@code None} (fail-fast in inspection — the method stops iterating after the first
     * {@code None}; entries are already materialized in the map before sequencing, so later
     * entries are not inspected but were already evaluated).
     *
     * @param <K> the key type
     * @param <V> the unwrapped value type
     * @param nem a {@code NonEmptyMap<K, Option<V>>}; must not be {@code null}
     * @return {@code Some(NonEmptyMap<K, V>)} if all values are present, {@code None} otherwise
     * @throws NullPointerException if {@code nem} is {@code null}
     */
    public static <K, V> Option<NonEmptyMap<K, V>> sequenceOption(NonEmptyMap<K, Option<V>> nem) {
        Objects.requireNonNull(nem, "nem must not be null");
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, Option<V>> entry : nem.toMap().entrySet()) {
            switch (entry.getValue()) {
                case Option.None<V> ignored -> { return Option.none(); }
                case Option.Some<V>(V v)   -> result.put(entry.getKey(), v);
            }
        }
        return fromMap(result); // always Some since nem.size() >= 1 and all values are present
    }

    /**
     * Sequences a {@code NonEmptyMap<K, Try<V>>} into a {@code Try<NonEmptyMap<K, V>>}.
     *
     * <p>Returns {@link Try#success(Object)} if all values succeed; returns
     * {@link Try#failure(Throwable)} from the first failing entry (fail-fast in inspection —
     * the method stops iterating after the first failure; entries are already materialized in
     * the map before sequencing, so later entries are not inspected but were already evaluated).
     *
     * @param <K> the key type
     * @param <V> the success value type
     * @param nem a {@code NonEmptyMap<K, Try<V>>}; must not be {@code null}
     * @return {@code Success(NonEmptyMap<K, V>)} if all succeed, {@code Failure} otherwise
     * @throws NullPointerException if {@code nem} is {@code null}
     */
    public static <K, V> Try<NonEmptyMap<K, V>> sequenceTry(NonEmptyMap<K, Try<V>> nem) {
        Objects.requireNonNull(nem, "nem must not be null");
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, Try<V>> entry : nem.toMap().entrySet()) {
            switch (entry.getValue()) {
                case Try.Failure<V> f -> { return Try.failure(f.cause()); }
                case Try.Success<V> s -> result.put(entry.getKey(), s.value());
            }
        }
        return Try.success(fromMapUnsafe(result)); // always non-empty since nem.size() >= 1
    }

    /**
     * Sequences a {@code NonEmptyMap<K, Either<E, V>>} into an
     * {@code Either<E, NonEmptyMap<K, V>>}.
     *
     * <p>Returns {@link Either#right(Object)} if all values are right; returns
     * {@link Either#left(Object)} from the first left entry (fail-fast in inspection —
     * the method stops iterating after the first left; entries are already materialized in
     * the map before sequencing, so later entries are not inspected but were already evaluated).
     *
     * @param <K> the key type
     * @param <E> the left (error) type
     * @param <V> the right (success) type
     * @param nem a {@code NonEmptyMap<K, Either<E, V>>}; must not be {@code null}
     * @return {@code right(NonEmptyMap<K, V>)} if all are right, {@code left(E)} otherwise
     * @throws NullPointerException if {@code nem} is {@code null}
     */
    public static <K, E, V> Either<E, NonEmptyMap<K, V>> sequenceEither(
            NonEmptyMap<K, Either<E, V>> nem) {
        Objects.requireNonNull(nem, "nem must not be null");
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, Either<E, V>> entry : nem.toMap().entrySet()) {
            switch (entry.getValue()) {
                case Either.Left<E, V> l  -> { return Either.left(l.value()); }
                case Either.Right<E, V> r -> result.put(entry.getKey(), r.value());
            }
        }
        return Either.right(fromMapUnsafe(result)); // always non-empty since nem.size() >= 1
    }

    /**
     * Sequences a {@code NonEmptyMap<K, Result<V, E>>} into a
     * {@code Result<NonEmptyMap<K, V>, E>}.
     *
     * <p>Returns {@link Result#ok(Object)} if all values are ok; returns
     * {@link Result#err(Object)} from the first error entry (fail-fast in inspection —
     * the method stops iterating after the first err; entries are already materialized in
     * the map before sequencing, so later entries are not inspected but were already evaluated).
     *
     * @param <K> the key type
     * @param <V> the ok value type
     * @param <E> the error type
     * @param nem a {@code NonEmptyMap<K, Result<V, E>>}; must not be {@code null}
     * @return {@code ok(NonEmptyMap<K, V>)} if all succeed, {@code err(E)} otherwise
     * @throws NullPointerException if {@code nem} is {@code null}
     */
    public static <K, V, E> Result<NonEmptyMap<K, V>, E> sequenceResult(
            NonEmptyMap<K, Result<V, E>> nem) {
        Objects.requireNonNull(nem, "nem must not be null");
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, Result<V, E>> entry : nem.toMap().entrySet()) {
            switch (entry.getValue()) {
                case Result.Err<V, E> err -> { return Result.err(err.error()); }
                case Result.Ok<V, E> ok   -> result.put(entry.getKey(), ok.value());
            }
        }
        return Result.ok(fromMapUnsafe(result)); // always non-empty since nem.size() >= 1
    }

    // -------------------------------------------------------------------------
    // Object
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NonEmptyMap<?, ?> other)) return false;
        return toMap().equals(other.toMap());
    }

    @Override
    public int hashCode() {
        return toMap().hashCode();
    }

    @Override
    public String toString() {
        return toMap().toString();
    }
}
