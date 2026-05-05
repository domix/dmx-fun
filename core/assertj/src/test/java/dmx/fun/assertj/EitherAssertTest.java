package dmx.fun.assertj;

import dmx.fun.Either;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EitherAssertTest {

    // ── isRight() ─────────────────────────────────────────────────────────────

    @Test
    void isRight_shouldPass_forRight() {
        assertThat(Either.<String, Integer>right(1)).isRight();
    }

    @Test
    void isRight_shouldFail_forLeft() {
        assertThatThrownBy(() -> assertThat(Either.<String, Integer>left("oops")).isRight())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Right")
            .hasMessageContaining("oops");
    }

    @Test
    void isRight_shouldIncludeDescription_inFailureMessage() {
        assertThatThrownBy(() ->
            assertThat(Either.<String, Integer>left("err")).as("payment result").isRight())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[payment result]");
    }

    // ── isLeft() ──────────────────────────────────────────────────────────────

    @Test
    void isLeft_shouldPass_forLeft() {
        assertThat(Either.<String, Integer>left("err")).isLeft();
    }

    @Test
    void isLeft_shouldFail_forRight() {
        assertThatThrownBy(() -> assertThat(Either.<String, Integer>right(42)).isLeft())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Left")
            .hasMessageContaining("42");
    }

    @Test
    void isLeft_shouldIncludeDescription_inFailureMessage() {
        assertThatThrownBy(() ->
            assertThat(Either.<String, Integer>right(1)).as("routing result").isLeft())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[routing result]");
    }

    // ── containsRight() ───────────────────────────────────────────────────────

    @Test
    void containsRight_shouldPass_whenValueMatches() {
        assertThat(Either.<String, Integer>right(42)).containsRight(42);
    }

    @Test
    void containsRight_shouldFail_whenValueDiffers() {
        assertThatThrownBy(() -> assertThat(Either.<String, Integer>right(42)).containsRight(99))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("99")
            .hasMessageContaining("42");
    }

    @Test
    void containsRight_shouldFail_forLeft() {
        assertThatThrownBy(() -> assertThat(Either.<String, Integer>left("e")).containsRight(1))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Right");
    }

    // ── containsLeft() ────────────────────────────────────────────────────────

    @Test
    void containsLeft_shouldPass_whenValueMatches() {
        assertThat(Either.<String, Integer>left("error")).containsLeft("error");
    }

    @Test
    void containsLeft_shouldFail_whenValueDiffers() {
        assertThatThrownBy(() -> assertThat(Either.<String, Integer>left("error")).containsLeft("other"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("other")
            .hasMessageContaining("error");
    }

    @Test
    void containsLeft_shouldFail_forRight() {
        assertThatThrownBy(() -> assertThat(Either.<String, Integer>right(1)).containsLeft("e"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Left");
    }

    // ── Chaining ──────────────────────────────────────────────────────────────

    @Test
    void isFluent_shouldChain_isRight_containsRight() {
        assertThat(Either.<String, Integer>right(5)).isRight().containsRight(5);
    }

    @Test
    void isFluent_shouldChain_isLeft_containsLeft() {
        assertThat(Either.<String, Integer>left("e")).isLeft().containsLeft("e");
    }
}
