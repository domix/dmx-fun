package dmx.fun;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TryCollectorTest {

    // ── Try.toList() ──────────────────────────────────────────────────────────

    @Test
    void toList_allSuccess_returnsSuccessList() {
        Try<List<Integer>> result = Stream.of(Try.success(1), Try.success(2), Try.success(3))
            .collect(Try.toList());

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
            .collect(Try.toList());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(boom);
    }

    @Test
    void toList_emptyStream_returnsSuccessEmptyList() {
        Try<List<Integer>> result = Stream.<Try<Integer>>of().collect(Try.toList());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEmpty();
    }

    @Test
    void toList_resultListIsUnmodifiable() {
        Try<List<Integer>> result = Stream.of(Try.success(1)).collect(Try.toList());

        assertThat(result.isSuccess()).isTrue();
        assertThatThrownBy(() -> result.get().add(99))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void toList_nullElement_throwsNPE() {
        assertThatThrownBy(() -> Stream.of(Try.success(1), null)
                .collect(Try.toList()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("null");
    }

    @Test
    void toList_nullAfterFailureReturnsFailureNotNPE() {
        // A null Try element after an earlier Failure must not mask the failure with NPE.
        // The finisher returns the first Failure it encounters before ever reaching null.
        RuntimeException boom = new RuntimeException("first");
        Try<List<Integer>> result = Stream.of(
                Try.success(1),
                Try.<Integer>failure(boom),
                (Try<Integer>) null)
            .collect(Try.toList());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(boom);
    }

    // ── Try.partitioningBy() ─────────────────────────────────────────────────

    @Test
    void partitioningBy_separatesSuccessesAndFailures() {
        IOException err1 = new IOException("e1");
        IOException err2 = new IOException("e2");
        Try.Partition<Integer> p = Stream.of(
                Try.success(1),
                Try.<Integer>failure(err1),
                Try.success(3),
                Try.<Integer>failure(err2))
            .collect(Try.partitioningBy());

        assertThat(p.successes()).containsExactly(1, 3);
        assertThat(p.failures()).containsExactly(err1, err2);
    }

    @Test
    void partitioningBy_allSuccess_emptyFailures() {
        Try.Partition<String> p = Stream.of(Try.success("a"), Try.success("b"))
            .collect(Try.partitioningBy());

        assertThat(p.successes()).containsExactly("a", "b");
        assertThat(p.failures()).isEmpty();
    }

    @Test
    void partitioningBy_allFailure_emptySuccesses() {
        RuntimeException ex = new RuntimeException("x");
        Try.Partition<String> p = Stream.<Try<String>>of(Try.failure(ex))
            .collect(Try.partitioningBy());

        assertThat(p.successes()).isEmpty();
        assertThat(p.failures()).containsExactly(ex);
    }

    @Test
    void partitioningBy_listsAreUnmodifiable() {
        Try.Partition<Integer> p = Stream.<Try<Integer>>of(Try.success(1))
            .collect(Try.partitioningBy());

        assertThatThrownBy(() -> p.successes().add(99))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> p.failures().add(new RuntimeException()))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void partitioningBy_nullElement_throwsNPE() {
        assertThatThrownBy(() -> Stream.of(Try.success(1), null)
                .collect(Try.partitioningBy()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("null");
    }
}
