package dmx.fun.assertj;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dmx.fun.NonEmptySet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NonEmptySetAssertTest {

    private static final NonEmptySet<String> ABC = NonEmptySet.of("a", Set.of("b", "c"));

    @Test
    void hasHead_shouldPass_whenHeadMatches() {
        assertThat(ABC).hasHead("a");
    }

    @Test
    void hasHead_shouldFail_whenHeadDiffers() {
        assertThatThrownBy(() -> assertThat(ABC).hasHead("z"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("z")
            .hasMessageContaining("a");
    }

    @Test
    void hasSize_shouldPass_whenSizeMatches() {
        assertThat(ABC).hasSize(3);
        assertThat(NonEmptySet.singleton(1)).hasSize(1);
    }

    @Test
    void hasSize_shouldFail_whenSizeDiffers() {
        assertThatThrownBy(() -> assertThat(ABC).hasSize(2))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("2")
            .hasMessageContaining("3");
    }

    @Test
    void contains_shouldPass_whenAllElementsPresent() {
        assertThat(ABC).contains("c", "a");
    }

    @Test
    void contains_shouldFail_whenAnElementIsMissing() {
        assertThatThrownBy(() -> assertThat(ABC).contains("a", "x"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("could not find [x]");
    }

    @Test
    void containsExactlyInAnyOrder_shouldPass_regardlessOfOrder() {
        assertThat(ABC).containsExactlyInAnyOrder("c", "a", "b");
    }

    @Test
    void containsExactlyInAnyOrder_shouldFail_whenAnElementIsMissing() {
        assertThatThrownBy(() -> assertThat(ABC).containsExactlyInAnyOrder("a", "b"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("exactly");
    }

    @Test
    void containsExactlyInAnyOrder_shouldFail_whenAnExtraElementIsExpected() {
        assertThatThrownBy(() -> assertThat(ABC).containsExactlyInAnyOrder("a", "b", "c", "d"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("d");
    }

    @Test
    void isFluent_shouldChain() {
        assertThat(ABC).hasHead("a").hasSize(3).contains("b").containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void hasHead_shouldIncludeDescription_inFailureMessage() {
        assertThatThrownBy(() -> assertThat(ABC).as("my set").hasHead("z"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[my set]");
    }
}
