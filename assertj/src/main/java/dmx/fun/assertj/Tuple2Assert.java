package dmx.fun.assertj;

import dmx.fun.Tuple2;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ assertions for {@link Tuple2}.
 *
 * <p>Obtain instances via {@link DmxFunAssertions#assertThat(Tuple2)}.
 *
 * @param <A> the first element type
 * @param <B> the second element type
 */
@NullMarked
public final class Tuple2Assert<A, B> extends AbstractAssert<Tuple2Assert<A, B>, Tuple2<A, B>> {

    Tuple2Assert(Tuple2<A, B> actual) {
        super(actual, Tuple2Assert.class);
    }

    /**
     * Verifies that the first element equals the given value.
     *
     * @param expected the expected first element
     * @return this assertion for chaining
     */
    public Tuple2Assert<A, B> hasFirst(A expected) {
        isNotNull();
        assertThat(actual._1())
            .withFailMessage(() -> String.format("Expected Tuple2 first element to be <%s> but was <%s>", expected, actual._1()))
            .isEqualTo(expected);
        return this;
    }

    /**
     * Verifies that the second element equals the given value.
     *
     * @param expected the expected second element
     * @return this assertion for chaining
     */
    public Tuple2Assert<A, B> hasSecond(B expected) {
        isNotNull();
        assertThat(actual._2())
            .withFailMessage(() -> String.format("Expected Tuple2 second element to be <%s> but was <%s>", expected, actual._2()))
            .isEqualTo(expected);
        return this;
    }
}
