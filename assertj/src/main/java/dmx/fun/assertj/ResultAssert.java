package dmx.fun.assertj;

import dmx.fun.Result;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ assertions for {@link Result}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Result)}.
 *
 * @param <V> the success value type
 * @param <E> the error type
 */
@NullMarked
public final class ResultAssert<V, E> extends AbstractAssert<ResultAssert<V, E>, Result<V, E>> {

    ResultAssert(Result<V, E> actual) {
        super(actual, ResultAssert.class);
    }

    /**
     * Verifies that the {@code Result} is {@code Ok}.
     *
     * @return this assertion for chaining
     */
    public ResultAssert<V, E> isOk() {
        isNotNull();
        assertThat(actual.isOk())
            .withFailMessage(() -> String.format("Expected Result to be Ok but was Err<%s>", actual.getError()))
            .isTrue();
        return this;
    }

    /**
     * Verifies that the {@code Result} is {@code Err}.
     *
     * @return this assertion for chaining
     */
    public ResultAssert<V, E> isErr() {
        isNotNull();
        assertThat(actual.isError())
            .withFailMessage(() -> String.format("Expected Result to be Err but was Ok<%s>", actual.get()))
            .isTrue();
        return this;
    }

    /**
     * Verifies that the {@code Result} is {@code Ok} and contains the given value.
     *
     * @param expected the expected success value
     * @return this assertion for chaining
     */
    public ResultAssert<V, E> containsValue(V expected) {
        isOk();
        assertThat(actual.get())
            .withFailMessage(() -> String.format("Expected Result to contain value <%s> but contained <%s>", expected, actual.get()))
            .isEqualTo(expected);
        return this;
    }

    /**
     * Verifies that the {@code Result} is {@code Err} and contains the given error.
     *
     * @param expected the expected error value
     * @return this assertion for chaining
     */
    public ResultAssert<V, E> containsError(E expected) {
        isErr();
        assertThat(actual.getError())
            .withFailMessage(() -> String.format("Expected Result to contain error <%s> but contained <%s>", expected, actual.getError()))
            .isEqualTo(expected);
        return this;
    }
}
