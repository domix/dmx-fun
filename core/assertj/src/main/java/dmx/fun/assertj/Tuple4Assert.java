package dmx.fun.assertj;

import dmx.fun.Tuple4;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ assertions for {@link Tuple4}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Tuple4)}.
 *
 * @param <A> the first element type
 * @param <B> the second element type
 * @param <C> the third element type
 * @param <D> the fourth element type
 */
@NullMarked
public final class Tuple4Assert<A, B, C, D> extends AbstractAssert<Tuple4Assert<A, B, C, D>, Tuple4<A, B, C, D>> {

    Tuple4Assert(Tuple4<A, B, C, D> actual) {
        super(actual, Tuple4Assert.class);
    }

    /**
     * Verifies that the first element equals the given value.
     *
     * @param expected the expected first element
     * @return this assertion for chaining
     */
    public Tuple4Assert<A, B, C, D> hasFirst(A expected) {
        isNotNull();
        assertThat(actual._1())
            .withFailMessage(() -> String.format("Expected Tuple4 first element to be <%s> but was <%s>", expected, actual._1()))
            .isEqualTo(expected);
        return this;
    }

    /**
     * Verifies that the second element equals the given value.
     *
     * @param expected the expected second element
     * @return this assertion for chaining
     */
    public Tuple4Assert<A, B, C, D> hasSecond(B expected) {
        isNotNull();
        assertThat(actual._2())
            .withFailMessage(() -> String.format("Expected Tuple4 second element to be <%s> but was <%s>", expected, actual._2()))
            .isEqualTo(expected);
        return this;
    }

    /**
     * Verifies that the third element equals the given value.
     *
     * @param expected the expected third element
     * @return this assertion for chaining
     */
    public Tuple4Assert<A, B, C, D> hasThird(C expected) {
        isNotNull();
        assertThat(actual._3())
            .withFailMessage(() -> String.format("Expected Tuple4 third element to be <%s> but was <%s>", expected, actual._3()))
            .isEqualTo(expected);
        return this;
    }

    /**
     * Verifies that the fourth element equals the given value.
     *
     * @param expected the expected fourth element
     * @return this assertion for chaining
     */
    public Tuple4Assert<A, B, C, D> hasFourth(D expected) {
        isNotNull();
        assertThat(actual._4())
            .withFailMessage(() -> String.format("Expected Tuple4 fourth element to be <%s> but was <%s>", expected, actual._4()))
            .isEqualTo(expected);
        return this;
    }
}
