package dmx.fun.assertj;

import dmx.fun.Guard;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardAssertTest {

    private static final Guard<String> ALPHANUMERIC =
        Guard.of(s -> s.matches("[a-zA-Z0-9]+"), "must be alphanumeric");

    private static final Guard<String> LENGTH =
        Guard.of(s -> s.length() >= 3, "must be at least 3 chars");

    private static final Guard<String> BOTH = ALPHANUMERIC.and(LENGTH);

    // ── accepts ───────────────────────────────────────────────────────────────

    @Test
    void accepts_shouldPass_forValidValue() {
        assertThat(ALPHANUMERIC)
            .accepts("alice123");
    }

    @Test
    void accepts_shouldFail_forInvalidValue() {
        assertThatThrownBy(() -> assertThat(ALPHANUMERIC).accepts("!!"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("!!");
    }

    // ── rejects ───────────────────────────────────────────────────────────────

    @Test
    void rejects_shouldPass_forInvalidValue() {
        assertThat(ALPHANUMERIC)
            .rejects("!!");
    }

    @Test
    void rejects_shouldFail_forValidValue() {
        assertThatThrownBy(() -> assertThat(ALPHANUMERIC)
            .rejects("alice"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("alice");
    }

    // ── rejectsWithMessage ────────────────────────────────────────────────────

    @Test
    void rejectsWithMessage_shouldPass_whenMessagePresent() {
        assertThat(ALPHANUMERIC)
            .rejectsWithMessage("!!", "alphanumeric");
    }

    @Test
    void rejectsWithMessage_shouldPass_withPartialMatch() {
        assertThat(ALPHANUMERIC)
            .rejectsWithMessage("!!", "must be");
    }

    @Test
    void rejectsWithMessage_shouldFail_whenMessageAbsent() {
        assertThatThrownBy(() -> assertThat(ALPHANUMERIC)
            .rejectsWithMessage("!!", "database error"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("database error");
    }

    @Test
    void rejectsWithMessage_shouldFail_forValidValue() {
        assertThatThrownBy(() -> assertThat(ALPHANUMERIC)
            .rejectsWithMessage("alice", "alphanumeric"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("alice");
    }

    // ── rejectsWithMessages ───────────────────────────────────────────────────

    @Test
    void rejectsWithMessages_shouldPass_whenAllMessagesPresent() {
        assertThat(BOTH)
            .rejectsWithMessages("!!", "alphanumeric", "at least 3 chars");
    }

    @Test
    void rejectsWithMessages_shouldFail_whenOneMessageAbsent() {
        assertThatThrownBy(() ->
            assertThat(BOTH).rejectsWithMessages("!!", "alphanumeric", "does not exist"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("does not exist");
    }

    // ── fluency and description ───────────────────────────────────────────────

    @Test
    void isFluent_shouldChain() {
        assertThat(ALPHANUMERIC)
            .accepts("alice")
            .rejects("!!")
            .rejectsWithMessage("!!", "alphanumeric");
    }

    @Test
    void shouldIncludeDescription_inFailureMessage() {
        assertThatThrownBy(() -> assertThat(ALPHANUMERIC).as("my guard").accepts("!!"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[my guard]");
    }
}
