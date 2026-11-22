package dmx.fun.assertj;

import dmx.fun.NonEmptyMap;
import dmx.fun.Option;
import java.util.Objects;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;

/**
 * AssertJ assertions for {@link NonEmptyMap}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(NonEmptyMap)}.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@NullMarked
public final class NonEmptyMapAssert<K, V> extends AbstractDmxFunAssert<NonEmptyMapAssert<K, V>, NonEmptyMap<K, V>> {

    NonEmptyMapAssert(NonEmptyMap<K, V> actual) {
        super(actual, NonEmptyMapAssert.class);
    }

    /**
     * Verifies that the {@code NonEmptyMap} has exactly the given number of entries.
     *
     * @param expected the expected size, at least 1
     * @return this assertion for chaining
     */
    public NonEmptyMapAssert<K, V> hasSize(int expected) {
        isNotNull();
        if (actual.size() != expected) {
            throw buildError("Expected NonEmptyMap to have size <%d> but had size <%d>: %s",
                expected, actual.size(), actual.toMap());
        }
        return this;
    }

    /**
     * Verifies that the {@code NonEmptyMap} contains the given key.
     *
     * @param key the key that must be present
     * @return this assertion for chaining
     */
    public NonEmptyMapAssert<K, V> containsKey(K key) {
        isNotNull();
        if (!actual.containsKey(key)) {
            throw buildError("Expected NonEmptyMap %s to contain key <%s>", actual.toMap(), key);
        }
        return this;
    }

    /**
     * Verifies that the {@code NonEmptyMap} maps the given key to the given value.
     *
     * @param key   the key that must be present
     * @param value the value expected under that key
     * @return this assertion for chaining
     */
    public NonEmptyMapAssert<K, V> containsEntry(K key, V value) {
        containsKey(key);
        V found = actual.get(key).get();
        if (!Objects.equals(found, value)) {
            throw buildError("Expected NonEmptyMap to map <%s> to <%s> but it was mapped to <%s>", key, value, found);
        }
        return this;
    }

    /**
     * Verifies that the {@code NonEmptyMap} contains the given key and that its value satisfies the
     * given requirement.
     *
     * @param key         the key that must be present
     * @param requirement a consumer that performs assertions on the value
     * @return this assertion for chaining
     */
    public NonEmptyMapAssert<K, V> hasValueSatisfying(K key, Consumer<V> requirement) {
        Objects.requireNonNull(requirement, "requirement must not be null");
        containsKey(key);
        Option<V> value = actual.get(key);
        requirement.accept(value.get());
        return this;
    }
}
