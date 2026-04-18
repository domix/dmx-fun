package dmx.fun.assertj;

import dmx.fun.Resource;
import dmx.fun.Try;
import java.util.Objects;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

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
public final class ResourceAssert<T> extends AbstractAssert<ResourceAssert<T>, Resource<T>> {

    ResourceAssert(Resource<T> actual) {
        super(actual, ResourceAssert.class);
    }

    /**
     * Verifies that the resource's acquire-use-release cycle succeeds and the acquired
     * value equals {@code expected}.
     *
     * @param expected the expected value produced by the resource
     * @return this assertion for chaining
     */
    public ResourceAssert<T> succeedsWith(T expected) {
        isNotNull();
        Try<T> result = actual.use(v -> v);
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
        isNotNull();
        Try<T> result = actual.use(v -> v);
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
        isNotNull();
        Try<T> result = actual.use(v -> v);
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

    private AssertionError buildError(String template, Object... args) {
        String message = String.format(template.replace("<%s>", "%s"), args);
        String description = info.descriptionText();
        return new AssertionError(description.isEmpty() ? message : "[" + description + "] " + message);
    }
}
