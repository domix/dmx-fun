package dmx.fun.assertj;

import dmx.fun.NonEmptySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * AssertJ assertions for {@link NonEmptySet}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(NonEmptySet)}.
 *
 * @param <T> the element type
 */
@NullMarked
public final class NonEmptySetAssert<T> extends AbstractDmxFunAssert<NonEmptySetAssert<T>, NonEmptySet<T>> {

    NonEmptySetAssert(NonEmptySet<T> actual) {
        super(actual, NonEmptySetAssert.class);
    }

    /**
     * Verifies that the head (the element the set was built from, first in iteration order) is equal
     * to the given value.
     *
     * @param expected the expected head
     * @return this assertion for chaining
     */
    public NonEmptySetAssert<T> hasHead(T expected) {
        isNotNull();
        if (!Objects.equals(actual.head(), expected)) {
            throw buildError("Expected NonEmptySet head to be <%s> but was <%s>", expected, actual.head());
        }
        return this;
    }

    /**
     * Verifies that the {@code NonEmptySet} has exactly the given number of elements.
     *
     * @param expected the expected size, at least 1
     * @return this assertion for chaining
     */
    public NonEmptySetAssert<T> hasSize(int expected) {
        isNotNull();
        if (actual.size() != expected) {
            throw buildError("Expected NonEmptySet to have size <%d> but had size <%d>: %s",
                expected, actual.size(), actual.toSet());
        }
        return this;
    }

    /**
     * Verifies that the {@code NonEmptySet} contains every given element.
     *
     * @param elements the elements that must be present
     * @return this assertion for chaining
     */
    @SafeVarargs
    public final NonEmptySetAssert<T> contains(T... elements) {
        Objects.requireNonNull(elements, "elements must not be null");
        isNotNull();
        List<T> missing = new ArrayList<>();
        for (T element : elements) {
            if (!actual.contains(element)) {
                missing.add(element);
            }
        }
        if (!missing.isEmpty()) {
            throw buildError("Expected NonEmptySet %s to contain %s but could not find %s",
                actual.toSet(), Arrays.toString(elements), missing);
        }
        return this;
    }

    /**
     * Verifies that the {@code NonEmptySet} contains exactly the given elements and nothing else,
     * regardless of order. Each expected element must match a distinct element of the set, so
     * repeating an element in {@code elements} can never match a set that holds it once.
     *
     * @param elements the expected elements
     * @return this assertion for chaining
     */
    @SafeVarargs
    public final NonEmptySetAssert<T> containsExactlyInAnyOrder(T... elements) {
        Objects.requireNonNull(elements, "elements must not be null");
        isNotNull();
        List<T> expected = Arrays.asList(elements);
        if (actual.size() != expected.size() || !actual.toSet().containsAll(expected)) {
            throw buildError("Expected NonEmptySet to contain exactly %s in any order but was %s",
                expected, actual.toSet());
        }
        return this;
    }
}
