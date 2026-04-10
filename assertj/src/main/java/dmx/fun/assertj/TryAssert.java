package dmx.fun.assertj;

import dmx.fun.Try;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ assertions for {@link Try}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Try)}.
 *
 * @param <V> the success value type
 */
@NullMarked
public final class TryAssert<V> extends AbstractAssert<TryAssert<V>, Try<V>> {

    TryAssert(Try<V> actual) {
        super(actual, TryAssert.class);
    }

    /**
     * Verifies that the {@code Try} is a {@code Success}.
     *
     * @return this assertion for chaining
     */
    public TryAssert<V> isSuccess() {
        isNotNull();
        assertThat(actual.isSuccess())
            .withFailMessage(() -> String.format("Expected Try to be Success but was Failure<%s>", actual.getCause()))
            .isTrue();
        return this;
    }

    /**
     * Verifies that the {@code Try} is a {@code Failure}.
     *
     * @return this assertion for chaining
     */
    public TryAssert<V> isFailure() {
        isNotNull();
        assertThat(actual.isFailure())
            .withFailMessage(() -> String.format("Expected Try to be Failure but was Success<%s>", actual.get()))
            .isTrue();
        return this;
    }

    /**
     * Verifies that the {@code Try} is a {@code Success} and contains the given value.
     *
     * @param expected the expected value
     * @return this assertion for chaining
     */
    public TryAssert<V> containsValue(V expected) {
        isSuccess();
        assertThat(actual.get())
            .withFailMessage(() -> String.format("Expected Try to contain <%s> but contained <%s>", expected, actual.get()))
            .isEqualTo(expected);
        return this;
    }

    /**
     * Verifies that the {@code Try} is a {@code Failure} caused by an exception of the given type.
     *
     * @param exceptionType the expected exception type
     * @return this assertion for chaining
     */
    public TryAssert<V> failsWith(Class<? extends Throwable> exceptionType) {
        isFailure();
        assertThat(actual.getCause())
            .withFailMessage(() -> String.format("Expected Try to fail with <%s> but failed with <%s>",
                exceptionType.getName(), actual.getCause().getClass().getName()))
            .isInstanceOf(exceptionType);
        return this;
    }
}
