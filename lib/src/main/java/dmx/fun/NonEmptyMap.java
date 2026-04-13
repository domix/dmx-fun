package dmx.fun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
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
     * then tail entries in insertion order).
     *
     * @return a new unmodifiable map with all entries
     */
    public Map<K, V> toMap() {
        Map<K, V> result = new LinkedHashMap<>();
        result.put(headKey, headValue);
        result.putAll(tail);
        return Collections.unmodifiableMap(result);
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
     * Applies {@code mapper} to every key and returns a new {@code NonEmptyMap}
     * with the mapped keys and the original values.
     *
     * <p>If multiple keys map to the same new key, last-write-wins semantics apply
     * (same as building a {@link LinkedHashMap} in encounter order). The head key is
     * always processed first.
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
     * @param mergeFunction function to resolve value conflicts; must not be {@code null}
     * @return a new {@code NonEmptyMap} containing all entries from both maps
     * @throws NullPointerException if {@code other} or {@code mergeFunction} is {@code null}
     */
    public NonEmptyMap<K, V> merge(NonEmptyMap<K, V> other, BinaryOperator<V> mergeFunction) {
        Objects.requireNonNull(other,         "other must not be null");
        Objects.requireNonNull(mergeFunction, "mergeFunction must not be null");
        Map<K, V> combined = new LinkedHashMap<>(this.toMap());
        other.toMap().forEach((k, v) -> combined.merge(k, v, mergeFunction));
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
