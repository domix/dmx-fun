package dmx.fun.assertj;

import dmx.fun.Accumulator;
import java.util.Collection;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * AssertJ assertions for {@link Accumulator}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Accumulator)}.
 *
 * @param <E> the accumulated side-channel type
 * @param <A> the primary value type
 */
@NullMarked
public final class AccumulatorAssert<E, A> extends AbstractDmxFunAssert<AccumulatorAssert<E, A>, Accumulator<E, A>> {

    AccumulatorAssert(Accumulator<E, A> actual) {
        super(actual, AccumulatorAssert.class);
    }

    /**
     * Verifies that the accumulator's primary value equals the given expected value.
     *
     * @param expected the expected value (may be {@code null})
     * @return this assertion for chaining
     */
    public AccumulatorAssert<E, A> hasValue(@Nullable A expected) {
        isNotNull();
        if (!Objects.equals(actual.value(), expected)) {
            throw buildError("Expected Accumulator to have value <%s> but had <%s>",
                expected, actual.value());
        }
        return this;
    }

    /**
     * Verifies that the accumulator's side-channel equals the given expected accumulation.
     *
     * @param expected the expected accumulated value
     * @return this assertion for chaining
     */
    public AccumulatorAssert<E, A> hasAccumulation(E expected) {
        isNotNull();
        if (!Objects.equals(actual.accumulated(), expected)) {
            throw buildError("Expected Accumulator to have accumulation <%s> but had <%s>",
                expected, actual.accumulated());
        }
        return this;
    }

    /**
     * Verifies that the accumulator's side-channel (which must be a {@link Collection})
     * contains the given element.
     *
     * @param element the element expected to be present in the accumulation
     * @return this assertion for chaining
     * @throws AssertionError if the accumulated value is not a {@link Collection}
     */
    public AccumulatorAssert<E, A> accumulationContains(Object element) {
        isNotNull();
        var col = requireCollection("accumulationContains");
        if (!col.contains(element)) {
            throw buildError("Expected accumulation <%s> to contain <%s>", col, element);
        }
        return this;
    }

    /**
     * Verifies that the accumulator's side-channel (which must be a {@link Collection})
     * has the given size.
     *
     * @param expected the expected number of elements in the accumulation
     * @return this assertion for chaining
     * @throws AssertionError if the accumulated value is not a {@link Collection}
     */
    public AccumulatorAssert<E, A> accumulationHasSize(int expected) {
        isNotNull();
        var col = requireCollection("accumulationHasSize");
        if (col.size() != expected) {
            throw buildError("Expected accumulation to have size <%s> but had <%s>",
                expected, col.size());
        }
        return this;
    }

    private Collection<?> requireCollection(String operation) {
        E accumulated = actual.accumulated();
        if (accumulated == null) {
            throw buildError(
                "Expected accumulated value to be a Collection for " + operation + ", but was <null>");
        }
        if (!(accumulated instanceof Collection<?> col)) {
            throw buildError(
                "Expected accumulated value to be a Collection for " + operation + ", but was <%s>",
                accumulated.getClass().getName());
        }
        return col;
    }
}
