package dmx.fun.assertj;

import dmx.fun.Tuple2;
import dmx.fun.Tuple3;
import dmx.fun.Tuple4;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Tuple2AssertTest {

    // ---------- Tuple2 ----------

    @Test
    void hasFirst_shouldPass_whenFirstMatches() {
        assertThat(new Tuple2<>("a", 1)).hasFirst("a");
    }

    @Test
    void hasFirst_shouldFail_whenFirstDiffers() {
        assertThatThrownBy(() -> assertThat(new Tuple2<>("a", 1)).hasFirst("b"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("first");
    }

    @Test
    void hasSecond_shouldPass_whenSecondMatches() {
        assertThat(new Tuple2<>("a", 1)).hasSecond(1);
    }

    @Test
    void hasSecond_shouldFail_whenSecondDiffers() {
        assertThatThrownBy(() -> assertThat(new Tuple2<>("a", 1)).hasSecond(99))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("second");
    }

    @Test
    void tuple2_isFluent_shouldChain() {
        assertThat(new Tuple2<>("x", 42)).hasFirst("x").hasSecond(42);
    }

    // ---------- Tuple3 ----------

    @Test
    void tuple3_hasFirst_shouldPass() {
        assertThat(new Tuple3<>("a", 2, true)).hasFirst("a");
    }

    @Test
    void tuple3_hasFirst_shouldFail_whenFirstDiffers() {
        assertThatThrownBy(() -> assertThat(new Tuple3<>("a", 2, true)).hasFirst("z"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("first");
    }

    @Test
    void tuple3_hasSecond_shouldPass() {
        assertThat(new Tuple3<>("a", 2, true)).hasSecond(2);
    }

    @Test
    void tuple3_hasSecond_shouldFail_whenSecondDiffers() {
        assertThatThrownBy(() -> assertThat(new Tuple3<>("a", 2, true)).hasSecond(99))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("second");
    }

    @Test
    void tuple3_hasThird_shouldPass() {
        assertThat(new Tuple3<>("a", 2, true)).hasThird(true);
    }

    @Test
    void tuple3_hasThird_shouldFail_whenThirdDiffers() {
        assertThatThrownBy(() -> assertThat(new Tuple3<>("a", 2, true)).hasThird(false))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("third");
    }

    @Test
    void tuple3_isFluent_shouldChain() {
        assertThat(new Tuple3<>("x", 1, true)).hasFirst("x").hasSecond(1).hasThird(true);
    }

    // ---------- Tuple4 ----------

    @Test
    void tuple4_hasFirst_shouldPass() {
        assertThat(new Tuple4<>("a", 2, true, 3.14)).hasFirst("a");
    }

    @Test
    void tuple4_hasFirst_shouldFail_whenFirstDiffers() {
        assertThatThrownBy(() -> assertThat(new Tuple4<>("a", 2, true, 3.14)).hasFirst("z"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("first");
    }

    @Test
    void tuple4_hasSecond_shouldPass() {
        assertThat(new Tuple4<>("a", 2, true, 3.14)).hasSecond(2);
    }

    @Test
    void tuple4_hasSecond_shouldFail_whenSecondDiffers() {
        assertThatThrownBy(() -> assertThat(new Tuple4<>("a", 2, true, 3.14)).hasSecond(99))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("second");
    }

    @Test
    void tuple4_hasThird_shouldPass() {
        assertThat(new Tuple4<>("a", 2, true, 3.14)).hasThird(true);
    }

    @Test
    void tuple4_hasThird_shouldFail_whenThirdDiffers() {
        assertThatThrownBy(() -> assertThat(new Tuple4<>("a", 2, true, 3.14)).hasThird(false))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("third");
    }

    @Test
    void tuple4_hasFourth_shouldPass() {
        assertThat(new Tuple4<>("a", 2, true, 3.14)).hasFourth(3.14);
    }

    @Test
    void tuple4_hasFourth_shouldFail_whenFourthDiffers() {
        assertThatThrownBy(() -> assertThat(new Tuple4<>("a", 2, true, 3.14)).hasFourth(0.0))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("fourth");
    }

    @Test
    void tuple4_isFluent_shouldChain() {
        assertThat(new Tuple4<>(1, 2, 3, 4)).hasFirst(1).hasSecond(2).hasThird(3).hasFourth(4);
    }
}
