package dmx.fun.assertj;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dmx.fun.NonEmptyMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class NonEmptyMapAssertTest {

    private static final NonEmptyMap<String, Integer> SCORES = NonEmptyMap.of("ana", 10, Map.of("bo", 7));

    @Test
    void hasSize_shouldPass_whenSizeMatches() {
        assertThat(SCORES).hasSize(2);
        assertThat(NonEmptyMap.singleton("k", 1)).hasSize(1);
    }

    @Test
    void hasSize_shouldFail_whenSizeDiffers() {
        assertThatThrownBy(() -> assertThat(SCORES).hasSize(3))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("3")
            .hasMessageContaining("2");
    }

    @Test
    void containsKey_shouldPass_whenKeyPresent() {
        assertThat(SCORES).containsKey("bo");
    }

    @Test
    void containsKey_shouldFail_whenKeyMissing() {
        assertThatThrownBy(() -> assertThat(SCORES).containsKey("cy"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("cy");
    }

    @Test
    void containsEntry_shouldPass_whenKeyMapsToValue() {
        assertThat(SCORES).containsEntry("ana", 10);
    }

    @Test
    void containsEntry_shouldFail_whenValueDiffers() {
        assertThatThrownBy(() -> assertThat(SCORES).containsEntry("ana", 99))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("99")
            .hasMessageContaining("10");
    }

    @Test
    void containsEntry_shouldFail_whenKeyMissing() {
        assertThatThrownBy(() -> assertThat(SCORES).containsEntry("cy", 1))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("cy");
    }

    @Test
    void hasValueSatisfying_shouldPass_whenRequirementHolds() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        assertThat(SCORES).hasValueSatisfying("bo", v -> {
            invoked.set(true);
            org.assertj.core.api.Assertions.assertThat(v).isLessThan(10);
        });
        org.assertj.core.api.Assertions.assertThat(invoked.get())
            .as("consumer must be invoked")
            .isTrue();
    }

    @Test
    void hasValueSatisfying_shouldFail_whenKeyMissing() {
        assertThatThrownBy(() -> assertThat(SCORES).hasValueSatisfying("cy", v -> {}))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("cy");
    }

    @Test
    void hasValueSatisfying_shouldPropagateFailure_fromRequirement() {
        assertThatThrownBy(() -> assertThat(SCORES)
            .hasValueSatisfying("ana", v -> org.assertj.core.api.Assertions.assertThat(v).isEqualTo(0)))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void isFluent_shouldChain() {
        assertThat(SCORES).hasSize(2).containsKey("ana").containsEntry("bo", 7);
    }

    @Test
    void hasSize_shouldIncludeDescription_inFailureMessage() {
        assertThatThrownBy(() -> assertThat(SCORES).as("my map").hasSize(9))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[my map]");
    }
}
