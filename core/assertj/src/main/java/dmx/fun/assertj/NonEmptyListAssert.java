package dmx.fun.assertj;

import dmx.fun.NonEmptyList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;

/**
 * AssertJ assertions for {@link NonEmptyList}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(NonEmptyList)}.
 *
 * @param <T> the element type
 */
@NullMarked
public final class NonEmptyListAssert<T> extends AbstractDmxFunAssert<NonEmptyListAssert<T>, NonEmptyList<T>> {

    NonEmptyListAssert(NonEmptyList<T> actual) {
        super(actual, NonEmptyListAssert.class);
    }

    /**
     * Verifies that the head (first element) of the {@code NonEmptyList} is equal to the given value.
     *
     * @param expected the expected head
     * @return this assertion for chaining
     */
    public NonEmptyListAssert<T> hasHead(T expected) {
        isNotNull();
        if (!Objects.equals(actual.head(), expected)) {
            throw buildError("Expected NonEmptyList head to be <%s> but was <%s>", expected, actual.head());
        }
        return this;
    }

    /**
     * Verifies that the {@code NonEmptyList} has exactly the given number of elements.
     *
     * @param expected the expected size, at least 1
     * @return this assertion for chaining
     */
    public NonEmptyListAssert<T> hasSize(int expected) {
        isNotNull();
        if (actual.size() != expected) {
            throw buildError("Expected NonEmptyList to have size <%d> but had size <%d>: %s",
                expected, actual.size(), actual.toList());
        }
        return this;
    }

    /**
     * Verifies that the {@code NonEmptyList} contains every given element, in any order.
     *
     * @param elements the elements that must be present
     * @return this assertion for chaining
     */
    @SafeVarargs
    public final NonEmptyListAssert<T> contains(T... elements) {
        Objects.requireNonNull(elements, "elements must not be null");
        isNotNull();
        List<T> missing = new ArrayList<>();
        for (T element : elements) {
            if (!actual.contains(element)) {
                missing.add(element);
            }
        }
        if (!missing.isEmpty()) {
            throw buildError("Expected NonEmptyList %s to contain %s but could not find %s",
                actual.toList(), Arrays.toString(elements), missing);
        }
        return this;
    }

    /**
     * Verifies that the {@code NonEmptyList} contains exactly the given elements, in the same order.
     *
     * @param elements the expected elements, in order
     * @return this assertion for chaining
     */
    @SafeVarargs
    public final NonEmptyListAssert<T> containsExactly(T... elements) {
        Objects.requireNonNull(elements, "elements must not be null");
        isNotNull();
        List<T> expected = Arrays.asList(elements);
        if (!actual.toList().equals(expected)) {
            throw buildError("Expected NonEmptyList to contain exactly %s but was %s", expected, actual.toList());
        }
        return this;
    }

    /**
     * Verifies that the tail of the {@code NonEmptyList} (every element after the head, possibly empty)
     * satisfies the given requirement.
     *
     * @param requirement a consumer that performs assertions on the tail
     * @return this assertion for chaining
     */
    public NonEmptyListAssert<T> hasTailSatisfying(Consumer<List<T>> requirement) {
        Objects.requireNonNull(requirement, "requirement must not be null");
        isNotNull();
        requirement.accept(actual.tail());
        return this;
    }
}
