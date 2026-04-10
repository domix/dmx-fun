package dmx.fun.assertj;

import dmx.fun.Option;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionAssertTest {

    @Test
    void isSome_shouldPass_forSome() {
        assertThat(Option.some(42)).isSome();
    }

    @Test
    void isSome_shouldFail_forNone() {
        assertThatThrownBy(() -> assertThat(Option.none()).isSome())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Some");
    }

    @Test
    void isNone_shouldPass_forNone() {
        assertThat(Option.<Integer>none()).isNone();
    }

    @Test
    void isNone_shouldFail_forSome() {
        assertThatThrownBy(() -> assertThat(Option.some(1)).isNone())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("None");
    }

    @Test
    void containsValue_shouldPass_whenValueMatches() {
        assertThat(Option.some(42)).containsValue(42);
    }

    @Test
    void containsValue_shouldFail_whenValueDiffers() {
        assertThatThrownBy(() -> assertThat(Option.some(1)).containsValue(99))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void containsValue_shouldFail_forNone() {
        assertThatThrownBy(() -> assertThat(Option.<Integer>none()).containsValue(1))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void hasValueSatisfying_shouldPass_whenRequirementHolds() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        assertThat(Option.some(42)).hasValueSatisfying(v -> {
            invoked.set(true);
            org.assertj.core.api.Assertions.assertThat(v).isEqualTo(42);
        });
        org.assertj.core.api.Assertions.assertThat(invoked.get())
            .as("consumer must be invoked")
            .isTrue();
    }

    @Test
    void hasValueSatisfying_shouldFail_forNone() {
        assertThatThrownBy(() -> assertThat(Option.<Integer>none()).hasValueSatisfying(v -> {}))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void isFluent_shouldChain() {
        assertThat(Option.some("hello")).isSome().containsValue("hello");
    }
}
