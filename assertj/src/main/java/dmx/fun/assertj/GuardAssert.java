package dmx.fun.assertj;

import dmx.fun.Guard;
import dmx.fun.NonEmptyList;
import dmx.fun.Validated;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

/**
 * AssertJ assertions for {@link Guard}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Guard)}.
 *
 * @param <T> the type of value the guard validates
 */
@NullMarked
public final class GuardAssert<T> extends AbstractAssert<GuardAssert<T>, Guard<T>> {

    GuardAssert(Guard<T> actual) {
        super(actual, GuardAssert.class);
    }

    /**
     * Verifies that the guard accepts (validates successfully) the given value.
     *
     * @param value the value to check
     * @return this assertion for chaining
     */
    public GuardAssert<T> accepts(T value) {
        isNotNull();
        Validated<NonEmptyList<String>, T> result = actual.check(value);
        if (!result.isValid()) {
            throw buildError("Expected Guard to accept <%s> but rejected it with <%s>",
                value, result.getError());
        }
        return this;
    }

    /**
     * Verifies that the guard rejects (fails validation of) the given value.
     *
     * @param value the value to check
     * @return this assertion for chaining
     */
    public GuardAssert<T> rejects(T value) {
        isNotNull();
        Validated<NonEmptyList<String>, T> result = actual.check(value);
        if (!result.isInvalid()) {
            throw buildError("Expected Guard to reject <%s> but accepted it", value);
        }
        return this;
    }

    /**
     * Verifies that the guard rejects the given value and that at least one rejection
     * message contains the expected string.
     *
     * @param value   the value to check
     * @param message the string expected to appear in at least one rejection message
     * @return this assertion for chaining
     */
    public GuardAssert<T> rejectsWithMessage(T value, String message) {
        isNotNull();
        Validated<NonEmptyList<String>, T> result = actual.check(value);
        if (!result.isInvalid()) {
            throw buildError("Expected Guard to reject <%s> but accepted it", value);
        }
        NonEmptyList<String> errors = result.getError();
        if (errors.toList().stream().noneMatch(e -> e.contains(message))) {
            throw buildError("Expected rejection messages <%s> to contain <%s>", errors, message);
        }
        return this;
    }

    /**
     * Verifies that the guard rejects the given value and that for each expected message,
     * at least one rejection message contains it.
     *
     * @param value    the value to check
     * @param messages the strings each expected to appear in at least one rejection message
     * @return this assertion for chaining
     */
    public GuardAssert<T> rejectsWithMessages(T value, String... messages) {
        isNotNull();
        Validated<NonEmptyList<String>, T> result = actual.check(value);
        if (!result.isInvalid()) {
            throw buildError("Expected Guard to reject <%s> but accepted it", value);
        }
        NonEmptyList<String> errors = result.getError();
        for (String message : messages) {
            if (errors.toList().stream().noneMatch(e -> e.contains(message))) {
                throw buildError("Expected rejection messages <%s> to contain <%s>", errors, message);
            }
        }
        return this;
    }

    private AssertionError buildError(String template, Object... args) {
        String message = String.format(template.replace("<%s>", "%s"), args);
        String description = info.descriptionText();
        return new AssertionError(description.isEmpty() ? message : "[" + description + "] " + message);
    }
}
