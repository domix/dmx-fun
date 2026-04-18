package dmx.fun.assertj;

import dmx.fun.Resource;
import dmx.fun.Try;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceAssertTest {

    private static final Resource<Integer> SUCCESSFUL = Resource.of(() -> 42, v -> {});
    private static final Resource<Integer> FAILING    = Resource.of(
        () -> { throw new IOException("file not found"); }, v -> {});
    // Body succeeds, but release throws — the release exception surfaces as the failure.
    private static final Resource<Integer> RELEASE_FAILS = Resource.of(
        () -> 42, v -> { throw new IOException("release failed"); });
    // Acquire succeeds; used with a throwing body in tests so that both body and
    // release throw — body exception is the primary failure, release is suppressed.
    private static final Resource<Integer> BODY_AND_RELEASE_FAIL = Resource.of(
        () -> 99, v -> { throw new IOException("release failed"); });

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

    // ── RELEASE_FAILS: body succeeds, release throws ──────────────────────────

    @Test
    void releaseFails_surfacesReleaseException() {
        assertThat(RELEASE_FAILS)
            .failsWith(IOException.class)
            .failsWithMessage("release failed");
    }

    @Test
    void releaseFails_shouldFail_succeedsWith() {
        assertThatThrownBy(() -> assertThat(RELEASE_FAILS).succeedsWith(42))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("42");
    }

    // ── BODY_AND_RELEASE_FAIL: body throws, release also throws ──────────────

    @Test
    void bodyAndReleaseFail_bodyExceptionIsPrimary_releaseIsSuppressed() {
        // ResourceAssert always uses the identity body (v -> v), so to test the
        // suppression path we drive the lifecycle directly with a throwing body —
        // CheckedFunction allows throwing checked exceptions.
        Try<Integer> result = BODY_AND_RELEASE_FAIL.use(v -> {
            throw new IOException("body failed");
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause())
            .isInstanceOf(IOException.class)
            .hasMessage("body failed");
        assertThat(result.getCause().getSuppressed())
            .hasSize(1)
            .satisfies(suppressed -> {
                assertThat(suppressed[0])
                    .isInstanceOf(IOException.class)
                    .hasMessage("release failed");
            });
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
