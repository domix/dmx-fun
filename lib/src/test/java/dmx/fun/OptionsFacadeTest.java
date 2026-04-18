package dmx.fun;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OptionsFacadeTest {

    // ── Options.presentToList() ───────────────────────────────────────────────

    @Test
    void presentToList_collectsPresentValues() {
        List<String> result = Stream.<Option<String>>of(
                Option.some("a"), Option.none(), Option.some("b"), Option.none())
            .collect(Options.presentToList());

        assertThat(result).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void presentToList_allNone_returnsEmpty() {
        List<String> result = Stream.<Option<String>>of(Option.none(), Option.none())
            .collect(Options.presentToList());

        assertThat(result).isEmpty();
    }

    @Test
    void presentToList_allPresent_returnsAll() {
        List<Integer> result = Stream.of(Option.some(1), Option.some(2), Option.some(3))
            .collect(Options.presentToList());

        assertThat(result).containsExactlyInAnyOrder(1, 2, 3);
    }

    // ── Options.sequence() ────────────────────────────────────────────────────

    @Test
    void sequence_allPresent_returnsOptionalOfList() {
        Optional<List<String>> result = Stream.of(Option.some("x"), Option.some("y"))
            .collect(Options.sequence());

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("x", "y");
    }

    @Test
    void sequence_anyNone_returnsEmpty() {
        Optional<List<String>> result = Stream.<Option<String>>of(Option.some("x"), Option.none())
            .collect(Options.sequence());

        assertThat(result).isEmpty();
    }

    @Test
    void sequence_emptyStream_returnsOptionalOfEmptyList() {
        Optional<List<String>> result = Stream.<Option<String>>of()
            .collect(Options.sequence());

        assertThat(result).isPresent();
        assertThat(result.get()).isEmpty();
    }

    // ── Options.toNonEmptyList() ──────────────────────────────────────────────

    @Test
    void toNonEmptyList_nonEmptyStream_returnsSome() {
        Option<NonEmptyList<String>> result = Stream.of("a", "b", "c")
            .collect(Options.toNonEmptyList());

        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().toList()).containsExactly("a", "b", "c");
    }

    @Test
    void toNonEmptyList_emptyStream_returnsNone() {
        Option<NonEmptyList<String>> result = Stream.<String>of()
            .collect(Options.toNonEmptyList());

        assertThat(result.isEmpty()).isTrue();
    }
}
