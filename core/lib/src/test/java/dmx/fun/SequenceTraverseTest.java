package dmx.fun;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SequenceTraverseTest {

    // =========================================================================
    // Result
    // =========================================================================

    @Nested
    class ResultSequenceTraverse {

        // ----- sequence(Iterable) -----

        @Test
        void sequence_iterable_allOk_returnsOkList() {
            List<Result<Integer, String>> input = List.of(
                Result.ok(1), Result.ok(2), Result.ok(3)
            );
            Result<List<Integer>, String> result = Result.sequence(input);

            assertThat(result.isOk()).isTrue();
            assertThat(result.get()).containsExactly(1, 2, 3);
            assertThatThrownBy(() -> result.get().add(99))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void sequence_iterable_empty_returnsOkEmptyList() {
            Result<List<Integer>, String> result = Result.sequence(List.of());
            assertThat(result.isOk()).isTrue();
            assertThat(result.get()).isEmpty();
        }

        @Test
        void sequence_iterable_firstErrReturnsErrImmediately() {
            AtomicInteger seen = new AtomicInteger(0);
            List<Result<Integer, String>> base = List.of(
                Result.ok(1),
                Result.err("boom"),
                Result.ok(3)
            );
            Iterable<Result<Integer, String>> input = () -> base.stream()
                .peek(r -> seen.incrementAndGet())
                .iterator();

            Result<List<Integer>, String> result = Result.sequence(input);

            assertThat(result.isError()).isTrue();
            assertThat(result.getError()).isEqualTo("boom");
            assertThat(seen.get()).isLessThan(3);
        }

        @Test
        void sequence_iterable_nullElement_throwsNPE() {
            List<Result<Integer, String>> input = new java.util.ArrayList<>();
            input.add(Result.ok(1));
            input.add(null);
            assertThatThrownBy(() -> Result.sequence(input))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void sequence_iterable_nullInput_throwsNPE() {
            assertThatThrownBy(() -> Result.sequence((Iterable<Result<Integer, String>>) null))
                .isInstanceOf(NullPointerException.class);
        }

        // ----- sequence(Stream) -----

        @Test
        void sequence_stream_allOk_returnsOkList() {
            Result<List<Integer>, String> result = Result.sequence(
                Stream.of(Result.ok(10), Result.ok(20))
            );
            assertThat(result.isOk()).isTrue();
            assertThat(result.get()).containsExactly(10, 20);
            assertThatThrownBy(() -> result.get().add(99))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void sequence_stream_containsErr_returnsErr() {
            Result<List<Integer>, String> result = Result.sequence(
                Stream.of(Result.ok(1), Result.<Integer, String>err("fail"), Result.ok(3))
            );
            assertThat(result.isError()).isTrue();
            assertThat(result.getError()).isEqualTo("fail");
        }

        @Test
        void sequence_stream_empty_returnsOkEmptyList() {
            Result<List<Integer>, String> result = Result.sequence(Stream.of());
            assertThat(result.isOk()).isTrue();
            assertThat(result.get()).isEmpty();
        }

        // ----- traverse(Iterable) -----

        @Test
        void traverse_iterable_allOk_returnsOkList() {
            List<String> input = List.of("1", "2", "3");
            Result<List<Integer>, String> result = Result.traverse(input, s -> {
                try {
                    return Result.ok(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                    return Result.err("not a number: " + s);
                }
            });
            assertThat(result.isOk()).isTrue();
            assertThat(result.get()).containsExactly(1, 2, 3);
            assertThatThrownBy(() -> result.get().add(99))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void traverse_iterable_mapperReturnsErr_stopsEarly() {
            AtomicInteger calls = new AtomicInteger(0);
            List<String> input = List.of("1", "x", "3");

            Result<List<Integer>, String> result = Result.traverse(input, s -> {
                calls.incrementAndGet();
                try {
                    return Result.ok(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                    return Result.err("bad: " + s);
                }
            });

            assertThat(result.isError()).isTrue();
            assertThat(result.getError()).isEqualTo("bad: x");
            assertThat(calls.get()).isEqualTo(2); // stops after "x"
        }

        @Test
        void traverse_iterable_empty_returnsOkEmptyList() {
            Result<List<Integer>, String> result = Result.traverse(
                List.<String>of(), s -> Result.ok(s.length())
            );
            assertThat(result.isOk()).isTrue();
            assertThat(result.get()).isEmpty();
        }

        @Test
        void traverse_iterable_mapperReturnsNull_throwsNPE() {
            assertThatThrownBy(() -> Result.traverse(List.of("a"), s -> null))
                .isInstanceOf(NullPointerException.class);
        }

        // ----- traverse(Stream) -----

        @Test
        void traverse_stream_allOk_returnsOkList() {
            Result<List<Integer>, String> result = Result.traverse(
                Stream.of("10", "20"),
                s -> Result.ok(Integer.parseInt(s))
            );
            assertThat(result.isOk()).isTrue();
            assertThat(result.get()).containsExactly(10, 20);
            assertThatThrownBy(() -> result.get().add(99))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void traverse_stream_mapperReturnsErr_returnsErr() {
            Result<List<Integer>, String> result = Result.traverse(
                Stream.of("1", "bad"),
                s -> {
                    try {
                        return Result.ok(Integer.parseInt(s));
                    } catch (NumberFormatException e) {
                        return Result.err("invalid: " + s);
                    }
                }
            );
            assertThat(result.isError()).isTrue();
            assertThat(result.getError()).isEqualTo("invalid: bad");
        }

        @Test
        void sequence_stream_shortCircuitsAfterFirstErr() {
            AtomicInteger seen = new AtomicInteger(0);
            Stream<Result<Integer, String>> base = Stream.of(
                Result.ok(1), Result.<Integer, String>err("stop"), Result.ok(3)
            );
            Result<List<Integer>, String> result = Result.sequence(base.peek(r -> seen.incrementAndGet()));
            assertThat(result.isError()).isTrue();
            assertThat(result.getError()).isEqualTo("stop");
            assertThat(seen.get()).isLessThan(3);
        }

        @Test
        void traverse_stream_shortCircuitsAfterFirstErr() {
            AtomicInteger calls = new AtomicInteger(0);
            Result<List<Integer>, String> result = Result.traverse(
                Stream.of("1", "bad", "3"),
                s -> {
                    calls.incrementAndGet();
                    try {
                        return Result.ok(Integer.parseInt(s));
                    } catch (NumberFormatException e) {
                        return Result.err("invalid: " + s);
                    }
                }
            );
            assertThat(result.isError()).isTrue();
            assertThat(result.getError()).isEqualTo("invalid: bad");
            assertThat(calls.get()).isEqualTo(2);
        }
    }

    // =========================================================================
    // Try
    // =========================================================================

    @Nested
    class TrySequenceTraverse {

        // ----- sequence(Iterable) -----

        @Test
        void sequence_iterable_allSuccess_returnsSuccessList() {
            List<Try<Integer>> input = List.of(
                Try.success(1), Try.success(2), Try.success(3)
            );
            Try<List<Integer>> result = Try.sequence(input);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).containsExactly(1, 2, 3);
            assertThatThrownBy(() -> result.get().add(99))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void sequence_iterable_empty_returnsSuccessEmptyList() {
            Try<List<Integer>> result = Try.sequence(List.of());
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isEmpty();
        }

        @Test
        void sequence_iterable_firstFailureReturnsFailureImmediately() {
            RuntimeException boom = new RuntimeException("boom");
            AtomicInteger seen = new AtomicInteger(0);
            List<Try<Integer>> base = List.of(
                Try.success(1),
                Try.failure(boom),
                Try.success(3)
            );
            Iterable<Try<Integer>> input = () -> base.stream()
                .peek(t -> seen.incrementAndGet())
                .iterator();

            Try<List<Integer>> result = Try.sequence(input);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isSameAs(boom);
            assertThat(seen.get()).isLessThan(3);
        }

        @Test
        void sequence_iterable_nullElement_throwsNPE() {
            List<Try<Integer>> input = new java.util.ArrayList<>();
            input.add(Try.success(1));
            input.add(null);
            assertThatThrownBy(() -> Try.sequence(input))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void sequence_iterable_nullInput_throwsNPE() {
            assertThatThrownBy(() -> Try.sequence((Iterable<Try<Integer>>) null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void sequence_iterable_successWithNullValue_shouldNotThrow() {
            // Try.run() produces Success(null); sequence must not fail on null values
            // because List.copyOf() would throw NPE — Collections.unmodifiableList() does not
            Try<List<Void>> result = Try.sequence(List.of(Try.run(() -> {})));
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).hasSize(1);
            assertThat(result.get().get(0)).isNull();
        }

        // ----- sequence(Stream) -----

        @Test
        void sequence_stream_allSuccess_returnsSuccessList() {
            Try<List<Integer>> result = Try.sequence(
                Stream.of(Try.success(10), Try.success(20))
            );
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).containsExactly(10, 20);
            assertThatThrownBy(() -> result.get().add(99))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void sequence_stream_containsFailure_returnsFailure() {
            RuntimeException ex = new IllegalStateException("oops");
            Try<List<Integer>> result = Try.sequence(
                Stream.of(Try.success(1), Try.<Integer>failure(ex), Try.success(3))
            );
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isSameAs(ex);
        }

        @Test
        void sequence_stream_empty_returnsSuccessEmptyList() {
            Try<List<Integer>> result = Try.sequence(Stream.<Try<Integer>>of());
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isEmpty();
        }

        // ----- traverse(Iterable) -----

        @Test
        void traverse_iterable_allSuccess_returnsSuccessList() {
            List<String> input = List.of("1", "2", "3");
            Try<List<Integer>> result = Try.traverse(input,
                s -> Try.of(() -> Integer.parseInt(s))
            );
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).containsExactly(1, 2, 3);
            assertThatThrownBy(() -> result.get().add(99))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void traverse_iterable_mapperReturnsFailure_stopsEarly() {
            AtomicInteger calls = new AtomicInteger(0);
            List<String> input = List.of("1", "bad", "3");

            Try<List<Integer>> result = Try.traverse(input, s -> {
                calls.incrementAndGet();
                return Try.of(() -> Integer.parseInt(s));
            });

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isInstanceOf(NumberFormatException.class);
            assertThat(calls.get()).isEqualTo(2);
        }

        @Test
        void traverse_iterable_empty_returnsSuccessEmptyList() {
            Try<List<Integer>> result = Try.traverse(
                List.<String>of(), s -> Try.of(() -> Integer.parseInt(s))
            );
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isEmpty();
        }

        @Test
        void traverse_iterable_mapperReturnsNull_throwsNPE() {
            assertThatThrownBy(() -> Try.traverse(List.of("a"), s -> null))
                .isInstanceOf(NullPointerException.class);
        }

        // ----- traverse(Stream) -----

        @Test
        void traverse_stream_allSuccess_returnsSuccessList() {
            Try<List<Integer>> result = Try.traverse(
                Stream.of("10", "20"),
                s -> Try.of(() -> Integer.parseInt(s))
            );
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).containsExactly(10, 20);
            assertThatThrownBy(() -> result.get().add(99))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void traverse_stream_mapperReturnsFailure_returnsFailure() {
            Try<List<Integer>> result = Try.traverse(
                Stream.of("1", "oops"),
                s -> Try.of(() -> Integer.parseInt(s))
            );
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isInstanceOf(NumberFormatException.class);
        }

        @Test
        void sequence_stream_shortCircuitsAfterFirstFailure() {
            RuntimeException boom = new RuntimeException("boom");
            AtomicInteger seen = new AtomicInteger(0);
            Try<List<Integer>> result = Try.sequence(
                Stream.of(Try.success(1), Try.<Integer>failure(boom), Try.success(3))
                      .peek(t -> seen.incrementAndGet())
            );
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isSameAs(boom);
            assertThat(seen.get()).isLessThan(3);
        }

        @Test
        void traverse_stream_shortCircuitsAfterFirstFailure() {
            AtomicInteger calls = new AtomicInteger(0);
            Try<List<Integer>> result = Try.traverse(
                Stream.of("1", "bad", "3"),
                s -> {
                    calls.incrementAndGet();
                    return Try.of(() -> Integer.parseInt(s));
                }
            );
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isInstanceOf(NumberFormatException.class);
            assertThat(calls.get()).isEqualTo(2);
        }
    }
}
