package dmx.fun.assertj;

import dmx.fun.Result;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultAssertTest {

    @Test
    void isOk_shouldPass_forOk() {
        assertThat(Result.ok("hello")).isOk();
    }

    @Test
    void isOk_shouldFail_forErr() {
        assertThatThrownBy(() -> assertThat(Result.err("oops")).isOk())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Ok");
    }

    @Test
    void isErr_shouldPass_forErr() {
        assertThat(Result.<String, String>err("oops")).isErr();
    }

    @Test
    void isErr_shouldFail_forOk() {
        assertThatThrownBy(() -> assertThat(Result.ok("v")).isErr())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Err");
    }

    @Test
    void containsValue_shouldPass_whenValueMatches() {
        assertThat(Result.ok("hello")).containsValue("hello");
    }

    @Test
    void containsValue_shouldFail_whenValueDiffers() {
        assertThatThrownBy(() -> assertThat(Result.ok("hello")).containsValue("world"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("world")
            .hasMessageContaining("hello");
    }

    @Test
    void containsValue_shouldFail_forErr() {
        assertThatThrownBy(() -> assertThat(Result.<String, String>err("e")).containsValue("v"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Ok");
    }

    @Test
    void containsError_shouldPass_whenErrorMatches() {
        assertThat(Result.<String, String>err("oops")).containsError("oops");
    }

    @Test
    void containsError_shouldFail_whenErrorDiffers() {
        assertThatThrownBy(() -> assertThat(Result.<String, String>err("oops")).containsError("other"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("other")
            .hasMessageContaining("oops");
    }

    @Test
    void containsError_shouldFail_forOk() {
        assertThatThrownBy(() -> assertThat(Result.<String, String>ok("v")).containsError("e"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Err");
    }

    @Test
    void isFluent_shouldChain() {
        assertThat(Result.ok(42)).isOk().containsValue(42);
    }

    @Test
    void isOk_shouldIncludeDescription_inFailureMessage() {
        assertThatThrownBy(() -> assertThat(Result.<Integer, String>err("e")).as("my result").isOk())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[my result]");
    }
}
