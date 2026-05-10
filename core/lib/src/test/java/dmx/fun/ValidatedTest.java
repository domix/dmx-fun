package dmx.fun;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatedTest {

    // ---------- Factories ----------

    @Test
    void valid_shouldWrapValue() {
        var v = Validated.valid(42);

        assertThat(v.isValid()).isTrue();
        assertThat(v.isInvalid()).isFalse();
        assertThat(v.get()).isEqualTo(42);
    }

    @Test
    void invalid_shouldWrapError() {
        var v = Validated.invalid("bad");

        assertThat(v.isValid()).isFalse();
        assertThat(v.isInvalid()).isTrue();
        assertThat(v.getError()).isEqualTo("bad");
    }

    @Test
    void valid_shouldThrowNPE_ifValueIsNull() {
        assertThatThrownBy(() -> Validated.valid(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void invalid_shouldThrowNPE_ifErrorIsNull() {
        assertThatThrownBy(() -> Validated.invalid(null))
            .isInstanceOf(NullPointerException.class);
    }

    // ---------- Accessors ----------

    @Test
    void get_onInvalid_shouldThrowNoSuchElementException() {
        assertThatThrownBy(() -> Validated.invalid("err").get())
            .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void getError_onValid_shouldThrowNoSuchElementException() {
        assertThatThrownBy(() -> Validated.valid(1).getError())
            .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void getOrElse_onValid_shouldReturnValue() {
        var value = Validated.<String, Integer>valid(42)
            .getOrElse(0);

        assertThat(value)
            .isEqualTo(42);
    }

    @Test
    void getOrElse_onInvalid_shouldReturnFallback() {
        var value = Validated.<String, Integer>invalid("err")
            .getOrElse(0);

        assertThat(value)
            .isEqualTo(0);
    }

    @Test
    void getOrElseGet_onValid_shouldReturnValue() {
        var value = Validated.<String, Integer>valid(42)
            .getOrElseGet(() -> 0);

        assertThat(value)
            .isEqualTo(42);
    }

    @Test
    void getOrElseGet_onInvalid_shouldReturnSuppliedValue() {
        var value = Validated.<String, Integer>invalid("err")
            .getOrElseGet(() -> 0);

        assertThat(value)
            .isEqualTo(0);
    }

    @Test
    void getOrNull_onValid_shouldReturnValue() {
        var value = Validated.<String, Integer>valid(42)
            .getOrNull();

        assertThat(value)
            .isEqualTo(42);
    }

    @Test
    void getOrNull_onInvalid_shouldReturnNull() {
        var value = Validated.<String, Integer>invalid("err")
            .getOrNull();

        assertThat(value)
            .isNull();
    }

    @Test
    void getOrThrow_onValid_shouldReturnValue() {
        var value = Validated.<String, Integer>valid(42)
            .getOrThrow(RuntimeException::new);

        assertThat(value)
            .isEqualTo(42);
    }

    @Test
    void getOrThrow_onInvalid_shouldThrowMappedException() {
        assertThatThrownBy(
            () -> Validated.<String, Integer>invalid("oops")
                .getOrThrow(RuntimeException::new)
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessage("oops");
    }

    // ---------- Transformations ----------

    @Test
    void map_onValid_shouldApplyFunction() {
        var result = Validated.<String, Integer>valid(5)
            .map(n -> n * 2);

        assertThat(result.get())
            .isEqualTo(10);
    }

    @Test
    void map_onInvalid_shouldPreserveError() {
        var result = Validated.<String, Integer>invalid("err")
            .map(n -> n * 2);

        assertThat(result.isInvalid())
            .isTrue();
        assertThat(result.getError())
            .isEqualTo("err");
    }

    @Test
    void mapError_onInvalid_shouldApplyFunction() {
        var result = Validated.<String, String>invalid("err")
            .mapError(String::length);

        assertThat(result.getError())
            .isEqualTo(3);
    }

    @Test
    void mapError_onValid_shouldPreserveValue() {
        var result = Validated.<String, String>valid("ok")
            .mapError(String::length);

        assertThat(result.isValid())
            .isTrue();
        assertThat(result.get())
            .isEqualTo("ok");
    }

    @Test
    void flatMap_onValid_shouldChain() {
        var result = Validated.valid(5)
            .flatMap(n -> Validated.valid(n + 1));

        assertThat(result.get())
            .isEqualTo(6);
    }

    @Test
    void flatMap_onValid_canProduceInvalid() {
        var result = Validated.valid(5)
            .flatMap(_ -> Validated.invalid("nope"));

        assertThat(result.isInvalid())
            .isTrue();
        assertThat(result.getError())
            .isEqualTo("nope");
    }

    @Test
    void flatMap_onInvalid_shouldShortCircuit() {
        var called = new AtomicBoolean(false);
        var result = Validated.invalid("err")
            .flatMap(n -> { called.set(true); return Validated.valid(n); });

        assertThat(called.get())
            .isFalse();
        assertThat(result.isInvalid())
            .isTrue();
    }

    // ---------- Side effects ----------

    @Test
    void peek_onValid_shouldExecuteAction() {
        var captured = new AtomicReference<Integer>();
        var v = Validated.<String, Integer>valid(42)
            .peek(captured::set);

        assertThat(captured.get())
            .isEqualTo(42);
        assertThat(v.isValid())
            .isTrue();
    }

    @Test
    void peek_onInvalid_shouldNotExecuteAction() {
        var called = new AtomicBoolean(false);
        Validated.invalid("err")
            .peek(_ -> called.set(true));

        assertThat(called.get())
            .isFalse();
    }

    @Test
    void peekError_onInvalid_shouldExecuteAction() {
        var captured = new AtomicReference<String>();
        Validated.invalid("err")
            .peekError(captured::set);

        assertThat(captured.get())
            .isEqualTo("err");
    }

    @Test
    void peekError_onValid_shouldNotExecuteAction() {
        var called = new AtomicBoolean(false);
        Validated.valid(1)
            .peekError(_ -> called.set(true));

        assertThat(called.get())
            .isFalse();
    }

    // ---------- Null-check guards ----------

    @Test
    void map_shouldThrowNPE_whenMapperIsNull() {
        assertThatThrownBy(() -> Validated.valid(1).map(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    @Test
    void map_onInvalid_shouldThrowNPE_whenMapperIsNull() {
        assertThatThrownBy(() -> Validated.<String, Integer>invalid("err").map(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    @Test
    void mapError_shouldThrowNPE_whenMapperIsNull() {
        assertThatThrownBy(() -> Validated.<String, Integer>invalid("err").mapError(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    @Test
    void mapError_onValid_shouldThrowNPE_whenMapperIsNull() {
        assertThatThrownBy(() -> Validated.<String, Integer>valid(1).mapError(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    @Test
    void flatMap_shouldThrowNPE_whenMapperIsNull() {
        assertThatThrownBy(() -> Validated.valid(1).flatMap(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    @Test
    void flatMap_onInvalid_shouldThrowNPE_whenMapperIsNull() {
        assertThatThrownBy(() -> Validated.<String, Integer>invalid("err").flatMap(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    @Test
    void flatMap_shouldThrowNPE_whenMapperReturnsNull() {
        assertThatThrownBy(() -> Validated.valid(1).flatMap(_ -> null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void peek_shouldThrowNPE_whenActionIsNull() {
        assertThatThrownBy(() -> Validated.valid(1).peek(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    @Test
    void peekError_shouldThrowNPE_whenActionIsNull() {
        assertThatThrownBy(() -> Validated.<String, Integer>invalid("err").peekError(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    @Test
    void match_onValid_shouldCallOnValidConsumer() {
        var val = new AtomicReference<Integer>();
        var errCalled = new AtomicBoolean(false);
        Validated.valid(7)
            .match(val::set, _ -> errCalled.set(true));

        assertThat(val.get())
            .isEqualTo(7);
        assertThat(errCalled.get())
            .isFalse();
    }

    @Test
    void match_onInvalid_shouldCallOnInvalidConsumer() {
        var err = new AtomicReference<String>();
        var valCalled = new AtomicBoolean(false);
        Validated.invalid("boom")
            .match(_ -> valCalled.set(true), err::set);

        assertThat(err.get())
            .isEqualTo("boom");
        assertThat(valCalled.get())
            .isFalse();
    }

    // ---------- fold / stream ----------

    @Test
    void fold_onValid_shouldApplyOnValidFunction() {
        var result = Validated.valid(5)
            .fold(
                v -> "value:" + v,
                e -> "error:" + e
            );

        assertThat(result)
            .isEqualTo("value:5");
    }

    @Test
    void fold_onInvalid_shouldApplyOnInvalidFunction() {
        var result = Validated.invalid("bad")
            .fold(
                v -> "value:" + v,
                e -> "error:" + e
            );

        assertThat(result)
            .isEqualTo("error:bad");
    }

    @Test
    void stream_onValid_shouldReturnSingleElement() {
        var list = Validated.valid(42)
            .stream()
            .toList();

        assertThat(list)
            .isEqualTo(List.of(42));
    }

    @Test
    void stream_onInvalid_shouldReturnEmptyStream() {
        var list = Validated.invalid("err")
            .stream()
            .toList();

        assertThat(list)
            .isEmpty();
    }

    // ---------- sequence / traverse ----------

    @Test
    void sequence_allValid_shouldReturnValidList() {
        var items = List.<Validated<String, Integer>>of(
            Validated.valid(1),
            Validated.valid(2),
            Validated.valid(3)
        );
        var result = Validated.sequence(
            items,
            (a, b) -> a + "; " + b
        );

        assertThat(result.isValid())
            .isTrue();
        assertThat(result.get())
            .isEqualTo(List.of(1, 2, 3));
    }

    @Test
    void sequence_withOneInvalid_shouldReturnInvalid() {
        var items = List.<Validated<String, Integer>>of(
            Validated.valid(1),
            Validated.invalid("err1"),
            Validated.valid(3)
        );
        var result = Validated.sequence(
            items,
            (a, b) -> a + "; " + b
        );

        assertThat(result.isInvalid())
            .isTrue();
        assertThat(result.getError())
            .isEqualTo("err1");
    }

    @Test
    void sequence_withMultipleInvalid_shouldAccumulateErrors() {
        var items = List.<Validated<String, Integer>>of(
            Validated.invalid("e1"),
            Validated.valid(2),
            Validated.invalid("e2")
        );
        var result = Validated.sequence(
            items,
            (a, b) -> a + "; " + b
        );

        assertThat(result.isInvalid())
            .isTrue();
        assertThat(result.getError())
            .isEqualTo("e1; e2");
    }

    @Test
    void sequence_stream_allValid_shouldReturnValidList() {
        var stream = Stream.<Validated<String, Integer>>of(
            Validated.valid(1),
            Validated.valid(2)
        );
        var result = Validated.sequence(
            stream,
            (a, b) -> a + "; " + b
        );

        assertThat(result.isValid())
            .isTrue();
        assertThat(result.get())
            .isEqualTo(List.of(1, 2));
    }

    @Test
    void sequence_stream_shouldAccumulateErrors() {
        var stream = Stream.<Validated<String, Integer>>of(
            Validated.invalid("e1"),
            Validated.invalid("e2")
        );
        var result = Validated.sequence(
            stream,
            (a, b) -> a + "; " + b
        );

        assertThat(result.isInvalid())
            .isTrue();
        assertThat(result.getError())
            .isEqualTo("e1; e2");
    }

    @Test
    void traverse_allValid_shouldReturnValidList() {
        var inputs = List.of(1, 2, 3);
        Validated<String, List<String>> result = Validated.traverse(
            inputs,
            n -> Validated.valid("v" + n),
            (a, b) -> a + "; " + b
        );

        assertThat(result.isValid())
            .isTrue();
        assertThat(result.get())
            .isEqualTo(List.of("v1", "v2", "v3"));
    }

    @Test
    void traverse_shouldAccumulateErrors() {
        var inputs = List.of(1, 2, 3);
        var result = Validated.traverse(
            inputs,
            n -> n % 2 == 0 ? Validated.invalid("bad:" + n) : Validated.valid("ok:" + n),
            (a, b) -> a + "; " + b
        );

        assertThat(result.isInvalid())
            .isTrue();
        assertThat(result.getError())
            .isEqualTo("bad:2");
    }

    // ---------- collector / traverseCollector ----------

    @Test
    void collector_allValid_shouldReturnValidList() {
        var result = Stream
            .<Validated<String, Integer>>of(
                Validated.valid(1),
                Validated.valid(2),
                Validated.valid(3)
            )
            .collect(
                Validated.collector(
                    (a, b) -> a + "; " + b
                )
            );

        assertThat(result.isValid())
            .isTrue();
        assertThat(result.get())
            .isEqualTo(List.of(1, 2, 3));
    }

    @Test
    void collector_mixedValidAndInvalid_shouldReturnAccumulatedError() {
        var result = Stream
            .<Validated<String, Integer>>of(
                Validated.valid(1),
                Validated.invalid("e1"),
                Validated.valid(3),
                Validated.invalid("e2")
            )
            .collect(
                Validated.collector(
                    (a, b) -> a + "; " + b
                )
            );

        assertThat(result.isInvalid())
            .isTrue();
        assertThat(result.getError())
            .isEqualTo("e1; e2");
    }

    @Test
    void collector_allInvalid_shouldAccumulateAllErrors() {
        var result = Stream
            .<Validated<String, Integer>>of(
                Validated.invalid("e1"),
                Validated.invalid("e2"),
                Validated.invalid("e3")
            )
            .collect(
                Validated.collector(
                    (a, b) -> a + "; " + b
                )
            );

        assertThat(result.isInvalid())
            .isTrue();
        assertThat(result.getError())
            .isEqualTo("e1; e2; e3");
    }

    @Test
    void collector_emptyStream_shouldReturnValidEmptyList() {
        var result = Stream
            .<Validated<String, Integer>>of()
            .collect(
                Validated.collector(
                    (a, b) -> a + "; " + b
                )
            );

        assertThat(result.isValid())
            .isTrue();
        assertThat(result.get())
            .isEmpty();
    }

    @Test
    void collector_parallelStream_shouldReturnValidList() {
        var result = Stream
            .<Validated<String, Integer>>of(
                Validated.valid(1),
                Validated.valid(2),
                Validated.valid(3),
                Validated.valid(4),
                Validated.valid(5)
            )
            .parallel()
            .collect(
                Validated.collector(
                    (a, b) -> a + "; " + b
                )
            );

        assertThat(result.isValid())
            .isTrue();
        assertThat(result.get())
            .containsExactlyInAnyOrder(1, 2, 3, 4, 5);
    }

    @Test
    void collector_shouldThrowNPE_ifErrMergeIsNull() {
        assertThatThrownBy(() -> Validated.collector(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void traverseCollector_allValid_shouldReturnValidList() {
        var result = Stream
            .of(1, 2, 3)
            .collect(
                Validated.traverseCollector(
                    n -> Validated.valid("v" + n),
                    (a, b) -> a + "; " + b
                )
            );

        assertThat(result.isValid())
            .isTrue();
        assertThat(result.get())
            .isEqualTo(List.of("v1", "v2", "v3"));
    }

    @Test
    void traverseCollector_mixedValidAndInvalid_shouldAccumulateErrors() {
        var result = Stream
            .of(1, 2, 3)
            .collect(
                Validated.traverseCollector(
                    n -> n % 2 == 0 ? Validated.invalid("bad:" + n) : Validated.valid("ok:" + n),
                    (a, b) -> a + "; " + b
                )
            );

        assertThat(result.isInvalid())
            .isTrue();
        assertThat(result.getError())
            .isEqualTo("bad:2");
    }

    @Test
    void traverseCollector_allInvalid_shouldAccumulateAllErrors() {
        var result = Stream
            .of(1, 2, 3)
            .collect(
                Validated.traverseCollector(
                    n -> Validated.invalid("e" + n),
                    (a, b) -> a + "; " + b
                )
            );

        assertThat(result.isInvalid())
            .isTrue();
        assertThat(result.getError())
            .isEqualTo("e1; e2; e3");
    }

    @Test
    void traverseCollector_emptyStream_shouldReturnValidEmptyList() {
        var result = Stream
            .<Integer>of()
            .collect(
                Validated.traverseCollector(
                    n -> Validated.valid("v" + n),
                    (a, b) -> a + "; " + b
                )
            );

        assertThat(result.isValid())
            .isTrue();
        assertThat(result.get())
            .isEmpty();
    }

    @Test
    void traverseCollector_parallelStream_shouldAccumulateErrors() {
        var result = Stream
            .of(1, 2, 3, 4, 5)
            .parallel()
            .collect(
                Validated.traverseCollector(
                    n -> n % 2 == 0 ? Validated.invalid("bad:" + n) : Validated.valid("ok:" + n),
                    (a, b) -> a + "; " + b
                )
            );

        assertThat(result.isInvalid())
            .isTrue();
        assertThat(result.getError())
            .contains("bad:2");
    }

    @Test
    void traverseCollector_shouldThrowNPE_ifMapperIsNull() {
        assertThatThrownBy(
            () -> Validated.traverseCollector(null, (a, b) -> a + "; " + b)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void traverseCollector_shouldThrowNPE_ifErrMergeIsNull() {
        assertThatThrownBy(
            () -> Validated.traverseCollector(n -> Validated.valid(n), null)
        ).isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // combine3
    // -------------------------------------------------------------------------

    @Test
    void combine3_allValid_shouldApplyValueMerge() {
        Validated<String, String> result = Validated.combine3(
            Validated.valid("a"),
            Validated.valid("b"),
            Validated.valid("c"),
            (e1, e2) -> e1 + "," + e2,
            (a, b, c) -> a + b + c
        );

        assertThat(result.isValid())
            .isTrue();
        assertThat(result.get())
            .isEqualTo("abc");
    }

    @Test
    void combine3_oneInvalid_shouldReturnInvalid() {
        Validated<String, String> result = Validated.combine3(
            Validated.valid("a"),
            Validated.invalid("err-b"),
            Validated.valid("c"),
            (e1, e2) -> e1 + "," + e2,
            (a, b, c) -> a + b + c
        );

        assertThat(result.isValid())
            .isFalse();
        assertThat(result.getError())
            .isEqualTo("err-b");
    }

    @Test
    void combine3_allInvalid_shouldAccumulateAllErrors() {
        var result = Validated.combine3(
            Validated.<String, String>invalidNel("err-a"),
            Validated.<String, String>invalidNel("err-b"),
            Validated.<String, String>invalidNel("err-c"),
            NonEmptyList::concat,
            (a, b, c) -> a + b + c
        );

        assertThat(result.isValid())
            .isFalse();
        assertThat(result.getError().toList())
            .containsExactly("err-a", "err-b", "err-c");
    }

    @Test
    void combine3_shouldThrowNPE_whenAnyArgIsNull() {
        assertThatThrownBy(
            () -> Validated.combine3(
                null,
                Validated.valid("b"),
                Validated.valid("c"),
                (e1, e2) -> e1, (a, _, _) -> a)
        ).isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // combine4
    // -------------------------------------------------------------------------

    @Test
    void combine4_allValid_shouldApplyValueMerge() {
        Validated<String, String> result = Validated.combine4(
            Validated.valid("a"),
            Validated.valid("b"),
            Validated.valid("c"),
            Validated.valid("d"),
            (e1, e2) -> e1 + "," + e2,
            (a, b, c, d) -> a + b + c + d
        );

        assertThat(result.isValid())
            .isTrue();
        assertThat(result.get())
            .isEqualTo("abcd");
    }

    @Test
    void combine4_oneInvalid_shouldReturnInvalid() {
        Validated<String, String> result = Validated.combine4(
            Validated.valid("a"),
            Validated.valid("b"),
            Validated.invalid("err-c"),
            Validated.valid("d"),
            (e1, e2) -> e1 + "," + e2,
            (a, b, c, d) -> a + b + c + d
        );

        assertThat(result.isValid())
            .isFalse();
        assertThat(result.getError())
            .isEqualTo("err-c");
    }

    @Test
    void combine4_allInvalid_shouldAccumulateAllErrors() {
        var result = Validated.combine4(
            Validated.<String, String>invalidNel("err-a"),
            Validated.<String, String>invalidNel("err-b"),
            Validated.<String, String>invalidNel("err-c"),
            Validated.<String, String>invalidNel("err-d"),
            NonEmptyList::concat,
            (a, b, c, d) -> a + b + c + d
        );

        assertThat(result.isValid())
            .isFalse();
        assertThat(result.getError().toList())
            .containsExactly("err-a", "err-b", "err-c", "err-d");
    }

    @Test
    void combine4_shouldThrowNPE_whenAnyArgIsNull() {
        assertThatThrownBy(
            () -> Validated.combine4(
                null,
                Validated.valid("b"),
                Validated.valid("c"),
                Validated.valid("d"),
                (e1, e2) -> e1, (a, _, _, _) -> a)
        ).isInstanceOf(NullPointerException.class);
    }

    // ---------- toEither ----------

    @Test
    void toEither_shouldReturnRight_whenValid() {
        var e = Validated
            .<String, Integer>valid(42)
            .toEither();

        assertThat(e.isRight())
            .isTrue();
        assertThat(e.getRight())
            .isEqualTo(42);
    }

    @Test
    void toEither_shouldReturnLeft_whenInvalid() {
        var e = Validated
            .<String, Integer>invalid("bad")
            .toEither();

        assertThat(e.isLeft())
            .isTrue();
        assertThat(e.getLeft())
            .isEqualTo("bad");
    }

    // ---------- toOptional ----------

    @Test
    void toOptional_shouldReturnPresent_whenValid() {
        var opt = Validated
            .valid(42)
            .toOptional();

        assertThat(opt)
            .isPresent()
            .contains(42);
    }

    @Test
    void toOptional_shouldReturnEmpty_whenInvalid() {
        var opt = Validated
            .invalid("err")
            .toOptional();

        assertThat(opt)
            .isEmpty();
    }

    // ---------- fromEither ----------

    @Test
    void fromEither_shouldReturnValid_whenRight() {
        var v = Validated
            .fromEither(Either.right(42));

        assertThat(v.isValid())
            .isTrue();
        assertThat(v.get())
            .isEqualTo(42);
    }

    @Test
    void fromEither_shouldReturnInvalid_whenLeft() {
        var v = Validated
            .fromEither(
                Either.<String, Integer>left("bad")
            );

        assertThat(v.isInvalid())
            .isTrue();
        assertThat(v.getError())
            .isEqualTo("bad");
    }

    @Test
    void fromEither_shouldThrowNPE_whenEitherIsNull() {
        assertThatThrownBy(
            () -> Validated.fromEither(null)
        ).isInstanceOf(NullPointerException.class);
    }

    // ---------- fromOptional ----------

    @Test
    void fromOptional_shouldReturnValid_whenPresent() {
        var v = Validated
            .fromOptional(
                Optional.of(42),
                "missing"
            );

        assertThat(v.isValid())
            .isTrue();
        assertThat(v.get())
            .isEqualTo(42);
    }

    @Test
    void fromOptional_shouldReturnInvalid_whenEmpty() {
        var v = Validated.fromOptional(
            Optional.<Integer>empty(),
            "missing"
        );

        assertThat(v.isInvalid())
            .isTrue();
        assertThat(v.getError())
            .isEqualTo("missing");
    }

    @Test
    void fromOptional_shouldThrowNPE_whenOptionalIsNull() {
        assertThatThrownBy(
            () -> Validated.fromOptional(
                null,
                "err"
            )
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void fromOptional_shouldThrowNPE_whenErrorIfEmptyIsNull() {
        assertThatThrownBy(
            () -> Validated.fromOptional(
                Optional.empty(),
                null
            )
        ).isInstanceOf(NullPointerException.class);
    }
}
