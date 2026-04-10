package dmx.fun;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NonEmptyListInteropTest {

    // =========================================================================
    // toStream()
    // =========================================================================

    @Nested
    class ToStream {

        @Test
        void toStream_shouldContainAllElements() {
            NonEmptyList<Integer> nel = NonEmptyList.of(1, List.of(2, 3));
            assertThat(nel.toStream()).containsExactly(1, 2, 3);
        }

        @Test
        void toStream_shouldSupportStreamOperations() {
            NonEmptyList<Integer> nel = NonEmptyList.of(1, List.of(2, 3, 4));
            int sum = nel.toStream().mapToInt(Integer::intValue).sum();
            assertThat(sum).isEqualTo(10);
        }

        @Test
        void toStream_onSingleton_shouldContainOneElement() {
            assertThat(NonEmptyList.singleton("x").toStream()).containsExactly("x");
        }
    }

    // =========================================================================
    // collector()
    // =========================================================================

    @Nested
    class CollectorTests {

        @Test
        void collector_shouldReturnSome_forNonEmptyStream() {
            Option<NonEmptyList<Integer>> result =
                Stream.of(1, 2, 3).collect(NonEmptyList.collector());

            assertThat(result.isDefined()).isTrue();
            assertThat(result.get().toList()).containsExactly(1, 2, 3);
        }

        @Test
        void collector_shouldReturnNone_forEmptyStream() {
            Option<NonEmptyList<String>> result =
                Stream.<String>empty().collect(NonEmptyList.collector());

            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        void collector_shouldPreserveOrder() {
            Option<NonEmptyList<String>> result =
                Stream.of("c", "a", "b").collect(NonEmptyList.collector());

            assertThat(result.get().toList()).containsExactly("c", "a", "b");
        }
    }

    // =========================================================================
    // sequence(NonEmptyList<Option<T>>)
    // =========================================================================

    @Nested
    class SequenceOption {

        @Test
        void sequence_shouldReturnSome_whenAllOptionsAreSome() {
            NonEmptyList<Option<Integer>> nel = NonEmptyList.of(
                Option.some(1), List.of(Option.some(2), Option.some(3)));

            Option<NonEmptyList<Integer>> result = NonEmptyList.sequence(nel);

            assertThat(result.isDefined()).isTrue();
            assertThat(result.get().toList()).containsExactly(1, 2, 3);
        }

        @Test
        void sequence_shouldReturnNone_whenAnyOptionIsNone() {
            NonEmptyList<Option<Integer>> nel = NonEmptyList.of(
                Option.some(1), List.of(Option.none(), Option.some(3)));

            Option<NonEmptyList<Integer>> result = NonEmptyList.sequence(nel);

            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        void sequence_shouldReturnNone_whenFirstIsNone() {
            NonEmptyList<Option<String>> nel = NonEmptyList.singleton(Option.none());

            assertThat(NonEmptyList.sequence(nel).isEmpty()).isTrue();
        }

        @Test
        void sequence_singleton_Some_shouldReturnSome() {
            NonEmptyList<Option<String>> nel = NonEmptyList.singleton(Option.some("ok"));

            Option<NonEmptyList<String>> result = NonEmptyList.sequence(nel);

            assertThat(result.isDefined()).isTrue();
            assertThat(result.get().size()).isEqualTo(1);
            assertThat(result.get().head()).isEqualTo("ok");
        }

        @Test
        void sequence_shouldThrowNPE_whenNelIsNull() {
            assertThatThrownBy(() -> NonEmptyList.sequence(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("nel");
        }
    }

    // =========================================================================
    // Validated.invalidNel()
    // =========================================================================

    @Nested
    class InvalidNel {

        @Test
        void invalidNel_shouldProduceInvalidWithSingletonNel() {
            Validated<NonEmptyList<String>, Integer> v = Validated.invalidNel("email is required");

            assertThat(v.isInvalid()).isTrue();
            assertThat(v.getError().toList()).containsExactly("email is required");
        }

        @Test
        void invalidNel_shouldThrowNPE_whenErrorIsNull() {
            assertThatThrownBy(() -> Validated.<String, Integer>invalidNel(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("error");
        }

        @Test
        void invalidNel_shouldAccumulateErrors_viaCombine() {
            Validated<NonEmptyList<String>, String> email =
                Validated.invalidNel("email is required");
            Validated<NonEmptyList<String>, String> password =
                Validated.invalidNel("password too short");
            Validated<NonEmptyList<String>, String> name =
                Validated.invalidNel("name is required");

            Validated<NonEmptyList<String>, String> combined = email
                .combine(password, NonEmptyList::concat, (e, p) -> e)
                .combine(name,     NonEmptyList::concat, (ep, n) -> ep);

            assertThat(combined.isInvalid()).isTrue();
            assertThat(combined.getError().toList()).containsExactlyInAnyOrder(
                "email is required",
                "password too short",
                "name is required"
            );
        }

        @Test
        void invalidNel_mixedWithValid_shouldCollectOnlyErrors() {
            Validated<NonEmptyList<String>, String> email = Validated.valid("alice@example.com");
            Validated<NonEmptyList<String>, String> password =
                Validated.invalidNel("password too short");

            Validated<NonEmptyList<String>, String> combined =
                email.combine(password, NonEmptyList::concat, (e, p) -> e);

            assertThat(combined.isInvalid()).isTrue();
            assertThat(combined.getError().toList()).containsExactly("password too short");
        }
    }

    // =========================================================================
    // Validated.sequenceNel()
    // =========================================================================

    @Nested
    class SequenceNel {

        @Test
        void sequenceNel_shouldReturnValid_whenAllAreValid() {
            List<Validated<NonEmptyList<String>, Integer>> items = List.of(
                Validated.valid(1),
                Validated.valid(2),
                Validated.valid(3)
            );
            Validated<NonEmptyList<String>, List<Integer>> result = Validated.sequenceNel(items);

            assertThat(result.isValid()).isTrue();
            assertThat(result.get()).containsExactly(1, 2, 3);
        }

        @Test
        void sequenceNel_shouldAccumulateAllErrors() {
            List<Validated<NonEmptyList<String>, Integer>> items = List.of(
                Validated.invalidNel("err1"),
                Validated.valid(2),
                Validated.invalidNel("err2")
            );
            Validated<NonEmptyList<String>, List<Integer>> result = Validated.sequenceNel(items);

            assertThat(result.isInvalid()).isTrue();
            assertThat(result.getError().toList()).containsExactly("err1", "err2");
        }

        @Test
        void sequenceNel_empty_shouldReturnValidEmptyList() {
            Validated<NonEmptyList<String>, List<Integer>> result =
                Validated.sequenceNel(List.of());

            assertThat(result.isValid()).isTrue();
            assertThat(result.get()).isEmpty();
        }
    }

    // =========================================================================
    // Validated.traverseNel()
    // =========================================================================

    @Nested
    class TraverseNel {

        @Test
        void traverseNel_shouldReturnValid_whenAllMappingsSucceed() {
            List<String> inputs = List.of("1", "2", "3");

            Validated<NonEmptyList<String>, List<Integer>> result =
                Validated.traverseNel(inputs, s -> {
                    try {
                        return Validated.valid(Integer.parseInt(s));
                    } catch (NumberFormatException e) {
                        return Validated.invalidNel("not a number: " + s);
                    }
                });

            assertThat(result.isValid()).isTrue();
            assertThat(result.get()).containsExactly(1, 2, 3);
        }

        @Test
        void traverseNel_shouldAccumulateAllErrors() {
            List<String> inputs = List.of("1", "abc", "3", "xyz");

            Validated<NonEmptyList<String>, List<Integer>> result =
                Validated.traverseNel(inputs, s -> {
                    try {
                        return Validated.valid(Integer.parseInt(s));
                    } catch (NumberFormatException e) {
                        return Validated.invalidNel("not a number: " + s);
                    }
                });

            assertThat(result.isInvalid()).isTrue();
            assertThat(result.getError().toList()).containsExactly(
                "not a number: abc",
                "not a number: xyz"
            );
        }

        @Test
        void traverseNel_empty_shouldReturnValidEmptyList() {
            Validated<NonEmptyList<String>, List<Integer>> result =
                Validated.traverseNel(List.of(), (String s) -> Validated.valid(s.length()));

            assertThat(result.isValid()).isTrue();
            assertThat(result.get()).isEmpty();
        }
    }
}
