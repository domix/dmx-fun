package dmx.fun;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultsFacadeTest {

    // ── Results.toList() ─────────────────────────────────────────────────────

    @Test
    void toList_allOk_returnsOkList() {
        Result<List<Integer>, String> r = Stream.<Result<Integer, String>>of(
                Result.ok(1), Result.ok(2), Result.ok(3))
            .collect(Results.toList());

        assertThat(r.isOk()).isTrue();
        assertThat(r.get()).containsExactly(1, 2, 3);
    }

    @Test
    void toList_firstErrIsReturned() {
        Result<List<Integer>, String> r = Stream.<Result<Integer, String>>of(
                Result.ok(1), Result.err("bad"), Result.ok(3))
            .collect(Results.toList());

        assertThat(r.isError()).isTrue();
        assertThat(r.getError()).isEqualTo("bad");
    }

    // ── Results.partitioning() ───────────────────────────────────────────────

    @Test
    void partitioning_separatesOksAndErrors() {
        Results.Partition<Integer, String> p = Stream.<Result<Integer, String>>of(
                Result.ok(1), Result.err("e"), Result.ok(3))
            .collect(Results.partitioning());

        assertThat(p.oks()).containsExactly(1, 3);
        assertThat(p.errors()).containsExactly("e");
    }

    @Test
    void partitioning_emptyStream() {
        Results.Partition<Integer, String> p =
            Stream.<Result<Integer, String>>of().collect(Results.partitioning());

        assertThat(p.oks()).isEmpty();
        assertThat(p.errors()).isEmpty();
    }

    @Test
    void partitioning_listsAreUnmodifiable() {
        Results.Partition<Integer, String> p = Stream.<Result<Integer, String>>of(Result.ok(1))
            .collect(Results.partitioning());

        assertThatThrownBy(() -> p.oks().add(99))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> p.errors().add("x"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void partitioning_toResultPartition_roundTrips() {
        Results.Partition<Integer, String> rp = new Results.Partition<>(List.of(1), List.of("e"));
        Result.Partition<Integer, String> delegate = rp.toResultPartition();

        assertThat(delegate.oks()).isEqualTo(rp.oks());
        assertThat(delegate.errors()).isEqualTo(rp.errors());
    }

    // ── Results.groupingBy() ─────────────────────────────────────────────────

    @Test
    void groupingBy_classifier_groupsElements() {
        Map<Integer, NonEmptyList<String>> grouped = Stream.of("a", "bb", "cc", "ddd")
            .collect(Results.groupingBy(String::length));

        assertThat(grouped.get(1).toList()).containsExactly("a");
        assertThat(grouped.get(2).toList()).containsExactly("bb", "cc");
        assertThat(grouped.get(3).toList()).containsExactly("ddd");
    }

    @Test
    void groupingBy_withDownstream_appliesFinisher() {
        Map<Integer, Integer> grouped = Stream.of("a", "bb", "cc", "ddd")
            .collect(Results.groupingBy(String::length, NonEmptyList::size));

        assertThat(grouped.get(1)).isEqualTo(1);
        assertThat(grouped.get(2)).isEqualTo(2);
        assertThat(grouped.get(3)).isEqualTo(1);
    }
}
