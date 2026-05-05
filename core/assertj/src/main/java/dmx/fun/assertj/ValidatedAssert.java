package dmx.fun.assertj;

import dmx.fun.Validated;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * AssertJ assertions for {@link Validated}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Validated)}.
 *
 * @param <E> the error type
 * @param <A> the value type
 */
@NullMarked
public final class ValidatedAssert<E, A> extends AbstractDmxFunAssert<ValidatedAssert<E, A>, Validated<E, A>> {

    ValidatedAssert(Validated<E, A> actual) {
        super(actual, ValidatedAssert.class);
    }

    /**
     * Verifies that the {@code Validated} is {@code Valid}.
     *
     * @return this assertion for chaining
     */
    public ValidatedAssert<E, A> isValid() {
        isNotNull();
        if (!actual.isValid()) {
            throw buildError("Expected Validated to be Valid but was Invalid<%s>", actual.getError());
        }
        return this;
    }

    /**
     * Verifies that the {@code Validated} is {@code Invalid}.
     *
     * @return this assertion for chaining
     */
    public ValidatedAssert<E, A> isInvalid() {
        isNotNull();
        if (!actual.isInvalid()) {
            throw buildError("Expected Validated to be Invalid but was Valid<%s>", actual.get());
        }
        return this;
    }

    /**
     * Verifies that the {@code Validated} is {@code Valid} and contains the given value.
     *
     * @param expected the expected value
     * @return this assertion for chaining
     */
    public ValidatedAssert<E, A> containsValue(A expected) {
        isValid();
        if (!Objects.equals(actual.get(), expected)) {
            throw buildError("Expected Validated to contain <%s> but contained <%s>", expected, actual.get());
        }
        return this;
    }

    /**
     * Verifies that the {@code Validated} is {@code Invalid} and contains the given error.
     *
     * @param expected the expected error
     * @return this assertion for chaining
     */
    public ValidatedAssert<E, A> hasError(E expected) {
        isInvalid();
        if (!Objects.equals(actual.getError(), expected)) {
            throw buildError("Expected Validated to have error <%s> but had <%s>", expected, actual.getError());
        }
        return this;
    }

}
