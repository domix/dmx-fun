package dmx.fun.assertj;

import dmx.fun.Option;
import java.util.Objects;
import java.util.function.Consumer;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

/**
 * AssertJ assertions for {@link Option}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Option)}.
 *
 * @param <V> the type of the optional value
 */
@NullMarked
public final class OptionAssert<V> extends AbstractAssert<OptionAssert<V>, Option<V>> {

    OptionAssert(Option<V> actual) {
        super(actual, OptionAssert.class);
    }

    /**
     * Verifies that the {@code Option} is {@code Some}.
     *
     * @return this assertion for chaining
     */
    public OptionAssert<V> isSome() {
        isNotNull();
        if (!actual.isDefined()) {
            failWithMessage("Expected Option to be Some but was None");
        }
        return this;
    }

    /**
     * Verifies that the {@code Option} is {@code None}.
     *
     * @return this assertion for chaining
     */
    public OptionAssert<V> isNone() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected Option to be None but was Some<%s>", actual.get());
        }
        return this;
    }

    /**
     * Verifies that the {@code Option} is {@code Some} and contains the given value.
     *
     * @param expected the expected value
     * @return this assertion for chaining
     */
    public OptionAssert<V> containsValue(V expected) {
        isSome();
        if (!Objects.equals(actual.get(), expected)) {
            failWithMessage("Expected Option to contain <%s> but contained <%s>", expected, actual.get());
        }
        return this;
    }

    /**
     * Verifies that the {@code Option} is {@code Some} and that its value satisfies the given requirement.
     *
     * @param requirement a consumer that performs assertions on the value
     * @return this assertion for chaining
     */
    public OptionAssert<V> hasValueSatisfying(Consumer<V> requirement) {
        isSome();
        requirement.accept(actual.get());
        return this;
    }
}
