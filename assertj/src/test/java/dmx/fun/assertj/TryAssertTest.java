package dmx.fun.assertj;

import dmx.fun.Try;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TryAssertTest {

    @Test
    void isSuccess_shouldPass_forSuccess() {
        assertThat(Try.success(1)).isSuccess();
    }

    @Test
    void isSuccess_shouldFail_forFailure() {
        assertThatThrownBy(() -> assertThat(Try.failure(new RuntimeException("boom"))).isSuccess())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Success");
    }

    @Test
    void isFailure_shouldPass_forFailure() {
        assertThat(Try.failure(new RuntimeException("boom"))).isFailure();
    }

    @Test
    void isFailure_shouldFail_forSuccess() {
        assertThatThrownBy(() -> assertThat(Try.success(1)).isFailure())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Failure");
    }

    @Test
    void containsValue_shouldPass_whenValueMatches() {
        assertThat(Try.success(42)).containsValue(42);
    }

    @Test
    void containsValue_shouldFail_whenValueDiffers() {
        assertThatThrownBy(() -> assertThat(Try.success(1)).containsValue(99))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void containsValue_shouldFail_forFailure() {
        assertThatThrownBy(() -> assertThat(Try.<Integer>failure(new RuntimeException())).containsValue(1))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void failsWith_shouldPass_whenExceptionTypeMatches() {
        assertThat(Try.failure(new IllegalArgumentException("bad"))).failsWith(IllegalArgumentException.class);
    }

    @Test
    void failsWith_shouldPass_forSupertype() {
        assertThat(Try.failure(new IllegalArgumentException("bad"))).failsWith(RuntimeException.class);
    }

    @Test
    void failsWith_shouldFail_whenExceptionTypeDiffers() {
        assertThatThrownBy(() ->
            assertThat(Try.failure(new RuntimeException())).failsWith(IllegalArgumentException.class))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void failsWith_shouldFail_whenTryIsSuccess() {
        assertThatThrownBy(() ->
            assertThat(Try.success(1)).failsWith(RuntimeException.class))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void isFluent_shouldChain() {
        assertThat(Try.success("ok")).isSuccess().containsValue("ok");
    }

    @Test
    void isSuccess_shouldIncludeDescription_inFailureMessage() {
        assertThatThrownBy(() -> assertThat(Try.failure(new RuntimeException())).as("my try").isSuccess())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[my try]");
    }
}
