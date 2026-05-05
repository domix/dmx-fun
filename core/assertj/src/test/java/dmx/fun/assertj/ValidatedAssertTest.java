package dmx.fun.assertj;

import dmx.fun.Validated;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatedAssertTest {

    @Test
    void isValid_shouldPass_forValid() {
        assertThat(Validated.valid("ok")).isValid();
    }

    @Test
    void isValid_shouldFail_forInvalid() {
        assertThatThrownBy(() -> assertThat(Validated.invalid("err")).isValid())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Valid");
    }

    @Test
    void isInvalid_shouldPass_forInvalid() {
        assertThat(Validated.<String, String>invalid("err")).isInvalid();
    }

    @Test
    void isInvalid_shouldFail_forValid() {
        assertThatThrownBy(() -> assertThat(Validated.valid("v")).isInvalid())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Invalid");
    }

    @Test
    void containsValue_shouldPass_whenValueMatches() {
        assertThat(Validated.valid(42)).containsValue(42);
    }

    @Test
    void containsValue_shouldFail_whenValueDiffers() {
        assertThatThrownBy(() -> assertThat(Validated.valid(1)).containsValue(99))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("99")
            .hasMessageContaining("1");
    }

    @Test
    void containsValue_shouldFail_forInvalid() {
        assertThatThrownBy(() -> assertThat(Validated.<String, Integer>invalid("e")).containsValue(1))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Valid");
    }

    @Test
    void hasError_shouldPass_whenErrorMatches() {
        assertThat(Validated.<String, Integer>invalid("name must not be blank")).hasError("name must not be blank");
    }

    @Test
    void hasError_shouldFail_whenErrorDiffers() {
        assertThatThrownBy(() -> assertThat(Validated.<String, Integer>invalid("err")).hasError("other"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("other")
            .hasMessageContaining("err");
    }

    @Test
    void hasError_shouldFail_forValid() {
        assertThatThrownBy(() -> assertThat(Validated.<String, Integer>valid(1)).hasError("e"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Invalid");
    }

    @Test
    void isFluent_shouldChain() {
        assertThat(Validated.<String, String>invalid("bad")).isInvalid().hasError("bad");
    }

    @Test
    void isValid_shouldIncludeDescription_inFailureMessage() {
        assertThatThrownBy(() -> assertThat(Validated.invalid("err")).as("my validated").isValid())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[my validated]");
    }
}
