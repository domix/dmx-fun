package dmx.fun.assertj;

import dmx.fun.Resource;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceAssertTest {

    private static final Resource<Integer> SUCCESSFUL = Resource.of(() -> 42, v -> {});
    private static final Resource<Integer> FAILING    = Resource.of(
        () -> { throw new IOException("file not found"); }, v -> {});

    // ── succeedsWith ──────────────────────────────────────────────────────────

    @Test
    void succeedsWith_shouldPass_whenValueMatches() {
        assertThat(SUCCESSFUL).succeedsWith(42);
    }

    @Test
    void succeedsWith_shouldFail_whenValueDiffers() {
        assertThatThrownBy(() -> assertThat(SUCCESSFUL).succeedsWith(99))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("99");
    }

    @Test
    void succeedsWith_shouldFail_whenResourceFails() {
        assertThatThrownBy(() -> assertThat(FAILING).succeedsWith(42))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("42");
    }

    // ── failsWith ─────────────────────────────────────────────────────────────

    @Test
    void failsWith_shouldPass_whenExceptionTypeMatches() {
        assertThat(FAILING).failsWith(IOException.class);
    }

    @Test
    void failsWith_shouldPass_forSupertype() {
        assertThat(FAILING).failsWith(Exception.class);
    }

    @Test
    void failsWith_shouldFail_whenExceptionTypeDiffers() {
        assertThatThrownBy(() -> assertThat(FAILING).failsWith(IllegalArgumentException.class))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("IllegalArgumentException");
    }

    @Test
    void failsWith_shouldFail_whenResourceSucceeds() {
        assertThatThrownBy(() -> assertThat(SUCCESSFUL).failsWith(IOException.class))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("IOException");
    }

    // ── failsWithMessage ──────────────────────────────────────────────────────

    @Test
    void failsWithMessage_shouldPass_whenMessageContained() {
        assertThat(FAILING).failsWithMessage("file not found");
    }

    @Test
    void failsWithMessage_shouldPass_whenPartialMatch() {
        assertThat(FAILING).failsWithMessage("not found");
    }

    @Test
    void failsWithMessage_shouldFail_whenMessageNotContained() {
        assertThatThrownBy(() -> assertThat(FAILING).failsWithMessage("database error"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("database error");
    }

    @Test
    void failsWithMessage_shouldFail_whenResourceSucceeds() {
        assertThatThrownBy(() -> assertThat(SUCCESSFUL).failsWithMessage("anything"))
            .isInstanceOf(AssertionError.class);
    }

    // ── fluency and description ───────────────────────────────────────────────

    @Test
    void isFluent_shouldChain() {
        assertThat(FAILING)
            .failsWith(IOException.class)
            .failsWithMessage("file not found");
    }

    @Test
    void shouldIncludeDescription_inFailureMessage() {
        assertThatThrownBy(() -> assertThat(FAILING).as("my resource").succeedsWith(42))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[my resource]");
    }
}
