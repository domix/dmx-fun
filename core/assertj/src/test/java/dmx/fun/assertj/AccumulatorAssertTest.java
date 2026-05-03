package dmx.fun.assertj;

import dmx.fun.Accumulator;
import java.util.List;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccumulatorAssertTest {

    private static final Accumulator<List<String>, Integer> ACC =
        Accumulator.of(42, List.of("step1", "step2"));

    // ── hasValue ──────────────────────────────────────────────────────────────

    @Test
    void hasValue_shouldPass_whenValueMatches() {
        assertThat(ACC).hasValue(42);
    }

    @Test
    void hasValue_shouldFail_whenValueDiffers() {
        assertThatThrownBy(() -> assertThat(ACC).hasValue(99))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("99");
    }

    @Test
    void hasValue_shouldPass_forNullValue() {
        // tell() creates an Accumulator whose value is null (no primary result, only log)
        var acc = Accumulator.tell(List.of("log"));
        assertThat(acc).hasValue(null);
    }

    // ── hasAccumulation ───────────────────────────────────────────────────────

    @Test
    void hasAccumulation_shouldPass_whenAccumulationMatches() {
        assertThat(ACC).hasAccumulation(List.of("step1", "step2"));
    }

    @Test
    void hasAccumulation_shouldFail_whenAccumulationDiffers() {
        assertThatThrownBy(() -> assertThat(ACC).hasAccumulation(List.of("other")))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("other");
    }

    // ── accumulationContains ──────────────────────────────────────────────────

    @Test
    void accumulationContains_shouldPass_whenElementPresent() {
        assertThat(ACC).accumulationContains("step1");
    }

    @Test
    void accumulationContains_shouldFail_whenElementAbsent() {
        assertThatThrownBy(() -> assertThat(ACC).accumulationContains("step3"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("step3");
    }

    @Test
    void accumulationContains_shouldFail_whenAccumulationIsNotCollection() {
        var acc = Accumulator.of(1, "not-a-collection");
        assertThatThrownBy(() -> assertThat(acc).accumulationContains("x"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Collection");
    }

    // ── accumulationHasSize ───────────────────────────────────────────────────

    @Test
    void accumulationHasSize_shouldPass_whenSizeMatches() {
        assertThat(ACC).accumulationHasSize(2);
    }

    @Test
    void accumulationHasSize_shouldFail_whenSizeDiffers() {
        assertThatThrownBy(() -> assertThat(ACC).accumulationHasSize(3))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("3");
    }

    @Test
    void accumulationHasSize_shouldFail_whenAccumulationIsNotCollection() {
        var acc = Accumulator.of(1, "not-a-collection");
        assertThatThrownBy(() -> assertThat(acc).accumulationHasSize(1))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Collection");
    }

    // ── fluency and description ───────────────────────────────────────────────

    @Test
    void isFluent_shouldChain() {
        assertThat(ACC)
            .hasValue(42)
            .hasAccumulation(List.of("step1", "step2"))
            .accumulationContains("step1")
            .accumulationHasSize(2);
    }

    @Test
    void shouldIncludeDescription_inFailureMessage() {
        assertThatThrownBy(() -> assertThat(ACC).as("my acc").hasValue(99))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[my acc]");
    }
}
