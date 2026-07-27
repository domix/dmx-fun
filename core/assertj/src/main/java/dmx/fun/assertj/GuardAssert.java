package dmx.fun.assertj;

import dmx.fun.Guard;
import dmx.fun.NonEmptyList;
import org.jspecify.annotations.NullMarked;

/**
 * AssertJ assertions for {@link Guard}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Guard)}.
 *
 * @param <T> the type of value the guard validates
 */
@NullMarked
public final class GuardAssert<T> extends AbstractDmxFunAssert<GuardAssert<T>, Guard<T>> {

    GuardAssert(Guard<T> actual) {
        super(actual, GuardAssert.class);
    }

    /** Renders the guard's identity for failure messages: "Guard 'name'" or plain "Guard". */
    private String guardLabel() {
        return actual.isNamed() ? "Guard '" + actual.name() + "'" : "Guard";
    }

    /** Shared reject preamble: fails unless the guard rejects {@code value}; returns the errors. */
    private NonEmptyList<String> checkRejected(T value) {
        isNotNull();
        var result = actual.check(value);
        if (!result.isInvalid()) {
            throw buildError("Expected %s to reject <%s> but accepted it", guardLabel(), value);
        }
        return result.getError();
    }

    /**
     * Verifies that the guard accepts (validates successfully) the given value.
     *
     * @param value the value to check
     * @return this assertion for chaining
     */
    public GuardAssert<T> accepts(T value) {
        isNotNull();
        var result = actual.check(value);
        if (!result.isValid()) {
            throw buildError("Expected %s to accept <%s> but rejected it with <%s>",
                guardLabel(), value, result.getError());
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
        checkRejected(value);
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
        return rejectsWithMessages(value, message);
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
        var errors = checkRejected(value);
        for (String message : messages) {
            if (errors.toList().stream().noneMatch(e -> e.contains(message))) {
                throw buildError("Expected %s rejection messages <%s> to contain <%s>",
                    guardLabel(), errors, message);
            }
        }
        return this;
    }

}
