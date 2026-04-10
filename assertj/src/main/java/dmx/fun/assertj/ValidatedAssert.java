package dmx.fun.assertj;

import dmx.fun.Validated;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ assertions for {@link Validated}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Validated)}.
 *
 * @param <E> the error type
 * @param <A> the value type
 */
@NullMarked
public final class ValidatedAssert<E, A> extends AbstractAssert<ValidatedAssert<E, A>, Validated<E, A>> {

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
        assertThat(actual.isValid())
            .withFailMessage(() -> String.format("Expected Validated to be Valid but was Invalid<%s>", actual.getError()))
            .isTrue();
        return this;
    }

    /**
     * Verifies that the {@code Validated} is {@code Invalid}.
     *
     * @return this assertion for chaining
     */
    public ValidatedAssert<E, A> isInvalid() {
        isNotNull();
        assertThat(actual.isInvalid())
            .withFailMessage(() -> String.format("Expected Validated to be Invalid but was Valid<%s>", actual.get()))
            .isTrue();
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
        assertThat(actual.get())
            .withFailMessage(() -> String.format("Expected Validated to contain <%s> but contained <%s>", expected, actual.get()))
            .isEqualTo(expected);
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
        assertThat(actual.getError())
            .withFailMessage(() -> String.format("Expected Validated to have error <%s> but had <%s>", expected, actual.getError()))
            .isEqualTo(expected);
        return this;
    }
}
