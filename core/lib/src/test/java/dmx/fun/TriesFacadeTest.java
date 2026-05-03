package dmx.fun;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TriesFacadeTest {

    // ── Tries.toList() ────────────────────────────────────────────────────────

    @Test
    void toList_allSuccess_returnsSuccessList() {
        Try<List<Integer>> result = Stream.of(Try.success(1), Try.success(2), Try.success(3))
            .collect(Tries.toList());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).containsExactly(1, 2, 3);
    }

    @Test
    void toList_firstFailureIsReturned() {
        RuntimeException boom = new RuntimeException("first");
        Try<List<Integer>> result = Stream.of(
                Try.success(1),
                Try.<Integer>failure(boom),
                Try.<Integer>failure(new RuntimeException("second")))
            .collect(Tries.toList());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(boom);
    }

    // ── Tries.partitioning() ─────────────────────────────────────────────────

    @Test
    void partitioning_separatesSuccessesAndFailures() {
        IOException err1 = new IOException("e1");
        IOException err2 = new IOException("e2");
        Tries.Partition<Integer> p = Stream.of(
                Try.success(1),
                Try.<Integer>failure(err1),
                Try.success(3),
                Try.<Integer>failure(err2))
            .collect(Tries.partitioning());

        assertThat(p.successes()).containsExactly(1, 3);
        assertThat(p.failures()).containsExactly(err1, err2);
    }

    @Test
    void partitioning_emptyStream() {
        Tries.Partition<String> p = Stream.<Try<String>>of().collect(Tries.partitioning());

        assertThat(p.successes()).isEmpty();
        assertThat(p.failures()).isEmpty();
    }

    @Test
    void partitioning_listsAreUnmodifiable() {
        Tries.Partition<Integer> p = Stream.<Try<Integer>>of(Try.success(1))
            .collect(Tries.partitioning());

        assertThatThrownBy(() -> p.successes().add(99))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> p.failures().add(new RuntimeException()))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void partitioning_toTryPartition_roundTrips() {
        Tries.Partition<Integer> tp = new Tries.Partition<>(List.of(1, 2), List.of(new RuntimeException("x")));
        Try.Partition<Integer> delegate = tp.toTryPartition();

        assertThat(delegate.successes()).isEqualTo(tp.successes());
        assertThat(delegate.failures()).isEqualTo(tp.failures());
    }
}
