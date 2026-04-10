package dmx.fun.assertj;

import dmx.fun.Result;
import java.util.Objects;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

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
        if (!actual.isOk()) {
            failWithMessage("Expected Result to be Ok but was Err<%s>", actual.getError());
        }
        return this;
    }

    /**
     * Verifies that the {@code Result} is {@code Err}.
     *
     * @return this assertion for chaining
     */
    public ResultAssert<V, E> isErr() {
        isNotNull();
        if (!actual.isError()) {
            failWithMessage("Expected Result to be Err but was Ok<%s>", actual.get());
        }
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
        if (!Objects.equals(actual.get(), expected)) {
            failWithMessage("Expected Result to contain value <%s> but contained <%s>", expected, actual.get());
        }
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
        if (!Objects.equals(actual.getError(), expected)) {
            failWithMessage("Expected Result to contain error <%s> but contained <%s>", expected, actual.getError());
        }
        return this;
    }
}
