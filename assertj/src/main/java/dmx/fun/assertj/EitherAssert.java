package dmx.fun.assertj;

import dmx.fun.Either;
import java.util.Objects;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

/**
 * AssertJ assertions for {@link Either}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Either)}.
 *
 * @param <L> the left value type
 * @param <R> the right value type
 */
@NullMarked
public final class EitherAssert<L, R> extends AbstractAssert<EitherAssert<L, R>, Either<L, R>> {

    EitherAssert(Either<L, R> actual) {
        super(actual, EitherAssert.class);
    }

    /**
     * Verifies that the {@code Either} is a {@code Right}.
     *
     * @return this assertion for chaining
     */
    public EitherAssert<L, R> isRight() {
        isNotNull();
        if (!actual.isRight()) {
            throw buildError("Expected Either to be Right but was Left<%s>", actual.getLeft());
        }
        return this;
    }

    /**
     * Verifies that the {@code Either} is a {@code Left}.
     *
     * @return this assertion for chaining
     */
    public EitherAssert<L, R> isLeft() {
        isNotNull();
        if (!actual.isLeft()) {
            throw buildError("Expected Either to be Left but was Right<%s>", actual.getRight());
        }
        return this;
    }

    /**
     * Verifies that the {@code Either} is a {@code Right} containing the given value.
     *
     * @param expected the expected right value
     * @return this assertion for chaining
     */
    public EitherAssert<L, R> containsRight(R expected) {
        isRight();
        if (!Objects.equals(actual.getRight(), expected)) {
            throw buildError("Expected Either to contain right <%s> but contained <%s>", expected, actual.getRight());
        }
        return this;
    }

    /**
     * Verifies that the {@code Either} is a {@code Left} containing the given value.
     *
     * @param expected the expected left value
     * @return this assertion for chaining
     */
    public EitherAssert<L, R> containsLeft(L expected) {
        isLeft();
        if (!Objects.equals(actual.getLeft(), expected)) {
            throw buildError("Expected Either to contain left <%s> but contained <%s>", expected, actual.getLeft());
        }
        return this;
    }

    private AssertionError buildError(String template, Object... args) {
        String message = String.format(template, args);
        String description = info.descriptionText();
        return new AssertionError(description.isEmpty() ? message : "[" + description + "] " + message);
    }
}
