package dmx.fun.assertj;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dmx.fun.NonEmptyList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class NonEmptyListAssertTest {

    private static final NonEmptyList<String> ABC = NonEmptyList.of("a", List.of("b", "c"));

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
        assertThat(NonEmptyList.singleton(1)).hasSize(1);
    }

    @Test
    void hasSize_shouldFail_whenSizeDiffers() {
        assertThatThrownBy(() -> assertThat(ABC).hasSize(2))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("2")
            .hasMessageContaining("3");
    }

    @Test
    void contains_shouldPass_whenAllElementsPresent_inAnyOrder() {
        assertThat(ABC).contains("c", "a");
    }

    @Test
    void contains_shouldFail_whenAnElementIsMissing() {
        assertThatThrownBy(() -> assertThat(ABC).contains("a", "x"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("could not find [x]");
    }

    @Test
    void containsExactly_shouldPass_whenElementsAndOrderMatch() {
        assertThat(ABC).containsExactly("a", "b", "c");
    }

    @Test
    void containsExactly_shouldFail_whenOrderDiffers() {
        assertThatThrownBy(() -> assertThat(ABC).containsExactly("c", "b", "a"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[c, b, a]")
            .hasMessageContaining("[a, b, c]");
    }

    @Test
    void containsExactly_shouldFail_whenAnElementIsMissing() {
        assertThatThrownBy(() -> assertThat(ABC).containsExactly("a", "b"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("exactly");
    }

    @Test
    void hasTailSatisfying_shouldPass_whenRequirementHolds() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        assertThat(ABC).hasTailSatisfying(tail -> {
            invoked.set(true);
            org.assertj.core.api.Assertions.assertThat(tail).containsExactly("b", "c");
        });
        org.assertj.core.api.Assertions.assertThat(invoked.get())
            .as("consumer must be invoked")
            .isTrue();
    }

    @Test
    void hasTailSatisfying_shouldPassAnEmptyTail_forSingleton() {
        assertThat(NonEmptyList.singleton("only"))
            .hasTailSatisfying(tail -> org.assertj.core.api.Assertions.assertThat(tail).isEmpty());
    }

    @Test
    void hasTailSatisfying_shouldPropagateFailure_fromRequirement() {
        assertThatThrownBy(() -> assertThat(ABC)
            .hasTailSatisfying(tail -> org.assertj.core.api.Assertions.assertThat(tail).isEmpty()))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void isFluent_shouldChain() {
        assertThat(ABC).hasHead("a").hasSize(3).contains("b").containsExactly("a", "b", "c");
    }

    @Test
    void hasHead_shouldIncludeDescription_inFailureMessage() {
        assertThatThrownBy(() -> assertThat(ABC).as("my list").hasHead("z"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[my list]");
    }
}
