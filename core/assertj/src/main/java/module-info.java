/**
 * AssertJ custom assertions for dmx-fun types.
 *
 * <p>Provides fluent, self-documenting assertions for {@code Either}, {@code Option},
 * {@code Result}, {@code Try}, {@code Validated}, {@code Tuple2}, {@code Tuple3},
 * {@code Tuple4}, {@code Resource}, {@code Guard}, and {@code Accumulator}.
 *
 * <p>Entry point: {@link dmx.fun.assertj.DmxFunAssertions#assertThat}.
 */
module dmx.fun.assertj {
    requires dmx.fun;
    requires org.assertj.core;
    requires org.jspecify;

    exports dmx.fun.assertj;
}
