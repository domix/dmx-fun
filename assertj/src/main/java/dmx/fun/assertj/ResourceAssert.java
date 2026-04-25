package dmx.fun.assertj;

import dmx.fun.Resource;
import dmx.fun.Try;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * AssertJ assertions for {@link Resource}.
 *
 * <p>Each assertion method exercises the resource's full acquire-use-release cycle
 * by calling {@link Resource#use(dmx.fun.CheckedFunction) use(v -> v)} (identity body)
 * and inspecting the resulting {@link Try}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Resource)}.
 *
 * @param <T> the resource value type
 */
@NullMarked
public final class ResourceAssert<T> extends AbstractDmxFunAssert<ResourceAssert<T>, Resource<T>> {

    private @Nullable Try<T> cachedResult;

    ResourceAssert(Resource<T> actual) {
        super(actual, ResourceAssert.class);
    }

    private Try<T> evaluate() {
        isNotNull();
        if (cachedResult == null) {
            cachedResult = actual.use(v -> v);
        }
        return cachedResult;
    }

    /**
     * Verifies that the resource's acquire-use-release cycle succeeds and the acquired
     * value equals {@code expected}.
     *
     * @param expected the expected value produced by the resource
     * @return this assertion for chaining
     */
    public ResourceAssert<T> succeedsWith(T expected) {
        var result = evaluate();
        if (result.isFailure()) {
            throw buildError("Expected Resource to succeed with <%s> but failed with <%s>",
                expected, result.getCause());
        }
        if (!Objects.equals(result.get(), expected)) {
            throw buildError("Expected Resource to succeed with <%s> but succeeded with <%s>",
                expected, result.get());
        }
        return this;
    }

    /**
     * Verifies that the resource's acquire-use-release cycle fails with an exception of
     * the given type (or a subtype).
     *
     * @param exceptionType the expected exception type
     * @return this assertion for chaining
     */
    public ResourceAssert<T> failsWith(Class<? extends Throwable> exceptionType) {
        var result = evaluate();
        if (result.isSuccess()) {
            throw buildError("Expected Resource to fail with <%s> but succeeded with <%s>",
                exceptionType.getName(), result.get());
        }
        if (!exceptionType.isInstance(result.getCause())) {
            throw buildError("Expected Resource to fail with <%s> but failed with <%s>",
                exceptionType.getName(), result.getCause().getClass().getName());
        }
        return this;
    }

    /**
     * Verifies that the resource's acquire-use-release cycle fails with an exception whose
     * message contains the given string.
     *
     * @param message the string expected to be contained in the failure message
     * @return this assertion for chaining
     */
    public ResourceAssert<T> failsWithMessage(String message) {
        var result = evaluate();
        if (result.isSuccess()) {
            throw buildError("Expected Resource to fail but succeeded with <%s>", result.get());
        }
        String causeMessage = result.getCause().getMessage();
        if (causeMessage == null || !causeMessage.contains(message)) {
            throw buildError("Expected Resource failure message to contain <%s> but was <%s>",
                message, causeMessage);
        }
        return this;
    }

}
