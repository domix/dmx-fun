package dmx.fun.assertj;

import dmx.fun.Tuple3;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ assertions for {@link Tuple3}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Tuple3)}.
 *
 * @param <A> the first element type
 * @param <B> the second element type
 * @param <C> the third element type
 */
@NullMarked
public final class Tuple3Assert<A, B, C> extends AbstractAssert<Tuple3Assert<A, B, C>, Tuple3<A, B, C>> {

    Tuple3Assert(Tuple3<A, B, C> actual) {
        super(actual, Tuple3Assert.class);
    }

    /**
     * Verifies that the first element equals the given value.
     *
     * @param expected the expected first element
     * @return this assertion for chaining
     */
    public Tuple3Assert<A, B, C> hasFirst(A expected) {
        isNotNull();
        assertThat(actual._1())
            .withFailMessage(() -> String.format("Expected Tuple3 first element to be <%s> but was <%s>", expected, actual._1()))
            .isEqualTo(expected);
        return this;
    }

    /**
     * Verifies that the second element equals the given value.
     *
     * @param expected the expected second element
     * @return this assertion for chaining
     */
    public Tuple3Assert<A, B, C> hasSecond(B expected) {
        isNotNull();
        assertThat(actual._2())
            .withFailMessage(() -> String.format("Expected Tuple3 second element to be <%s> but was <%s>", expected, actual._2()))
            .isEqualTo(expected);
        return this;
    }

    /**
     * Verifies that the third element equals the given value.
     *
     * @param expected the expected third element
     * @return this assertion for chaining
     */
    public Tuple3Assert<A, B, C> hasThird(C expected) {
        isNotNull();
        assertThat(actual._3())
            .withFailMessage(() -> String.format("Expected Tuple3 third element to be <%s> but was <%s>", expected, actual._3()))
            .isEqualTo(expected);
        return this;
    }
}
