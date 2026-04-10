package dmx.fun.assertj;

import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Tuple2;
import dmx.fun.Tuple3;
import dmx.fun.Tuple4;
import dmx.fun.Validated;
import org.jspecify.annotations.NullMarked;

/**
 * Entry point for AssertJ custom assertions for all dmx-fun types.
 *
 * <p>Use static imports for fluent syntax:
 * <pre>{@code
 * import static dmx.fun.assertj.DmxFunAssertions.assertThat;
 *
 * assertThat(Option.some(42)).isSome().containsValue(42);
 * assertThat(Result.ok("hello")).isOk().containsValue("hello");
 * assertThat(Try.success(1)).isSuccess().containsValue(1);
 * assertThat(Validated.valid("ok")).isValid().containsValue("ok");
 * }</pre>
 */
@NullMarked
public final class DmxFunAssertions {

    private DmxFunAssertions() {}

    /**
     * Creates an assertion for an {@link Option}.
     *
     * @param <V>    the value type
     * @param actual the Option to assert on
     * @return a new {@link OptionAssert}
     */
    public static <V> OptionAssert<V> assertThat(Option<V> actual) {
        return new OptionAssert<>(actual);
    }

    /**
     * Creates an assertion for a {@link Result}.
     *
     * @param <V>    the success value type
     * @param <E>    the error type
     * @param actual the Result to assert on
     * @return a new {@link ResultAssert}
     */
    public static <V, E> ResultAssert<V, E> assertThat(Result<V, E> actual) {
        return new ResultAssert<>(actual);
    }

    /**
     * Creates an assertion for a {@link Try}.
     *
     * @param <V>    the success value type
     * @param actual the Try to assert on
     * @return a new {@link TryAssert}
     */
    public static <V> TryAssert<V> assertThat(Try<V> actual) {
        return new TryAssert<>(actual);
    }

    /**
     * Creates an assertion for a {@link Validated}.
     *
     * @param <E>    the error type
     * @param <A>    the value type
     * @param actual the Validated to assert on
     * @return a new {@link ValidatedAssert}
     */
    public static <E, A> ValidatedAssert<E, A> assertThat(Validated<E, A> actual) {
        return new ValidatedAssert<>(actual);
    }

    /**
     * Creates an assertion for a {@link Tuple2}.
     *
     * @param <A>    the first element type
     * @param <B>    the second element type
     * @param actual the Tuple2 to assert on
     * @return a new {@link Tuple2Assert}
     */
    public static <A, B> Tuple2Assert<A, B> assertThat(Tuple2<A, B> actual) {
        return new Tuple2Assert<>(actual);
    }

    /**
     * Creates an assertion for a {@link Tuple3}.
     *
     * @param <A>    the first element type
     * @param <B>    the second element type
     * @param <C>    the third element type
     * @param actual the Tuple3 to assert on
     * @return a new {@link Tuple3Assert}
     */
    public static <A, B, C> Tuple3Assert<A, B, C> assertThat(Tuple3<A, B, C> actual) {
        return new Tuple3Assert<>(actual);
    }

    /**
     * Creates an assertion for a {@link Tuple4}.
     *
     * @param <A>    the first element type
     * @param <B>    the second element type
     * @param <C>    the third element type
     * @param <D>    the fourth element type
     * @param actual the Tuple4 to assert on
     * @return a new {@link Tuple4Assert}
     */
    public static <A, B, C, D> Tuple4Assert<A, B, C, D> assertThat(Tuple4<A, B, C, D> actual) {
        return new Tuple4Assert<>(actual);
    }
}
