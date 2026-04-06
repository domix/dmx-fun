package codes.domix.fun;

import java.util.List;
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
        Validated<String, Integer> v = Validated.valid(42);
        assertThat(v.isValid()).isTrue();
        assertThat(v.isInvalid()).isFalse();
        assertThat(v.get()).isEqualTo(42);
    }

    @Test
    void invalid_shouldWrapError() {
        Validated<String, Integer> v = Validated.invalid("bad");
        assertThat(v.isValid()).isFalse();
        assertThat(v.isInvalid()).isTrue();
        assertThat(v.getError()).isEqualTo("bad");
    }

    @Test
    void valid_shouldThrowNPE_ifValueIsNull() {
        assertThatThrownBy(() -> Validated.valid(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void invalid_shouldThrowNPE_ifErrorIsNull() {
        assertThatThrownBy(() -> Validated.invalid(null)).isInstanceOf(NullPointerException.class);
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
        assertThat(Validated.<String, Integer>valid(42).getOrElse(0)).isEqualTo(42);
    }

    @Test
    void getOrElse_onInvalid_shouldReturnFallback() {
        assertThat(Validated.<String, Integer>invalid("err").getOrElse(0)).isEqualTo(0);
    }

    @Test
    void getOrElseGet_onValid_shouldReturnValue() {
        assertThat(Validated.<String, Integer>valid(42).getOrElseGet(() -> 0)).isEqualTo(42);
    }

    @Test
    void getOrElseGet_onInvalid_shouldReturnSuppliedValue() {
        assertThat(Validated.<String, Integer>invalid("err").getOrElseGet(() -> 0)).isEqualTo(0);
    }

    @Test
    void getOrNull_onValid_shouldReturnValue() {
        assertThat(Validated.<String, Integer>valid(42).getOrNull()).isEqualTo(42);
    }

    @Test
    void getOrNull_onInvalid_shouldReturnNull() {
        assertThat(Validated.<String, Integer>invalid("err").getOrNull()).isNull();
    }

    @Test
    void getOrThrow_onValid_shouldReturnValue() {
        assertThat(Validated.<String, Integer>valid(42)
            .getOrThrow(e -> new RuntimeException(e))).isEqualTo(42);
    }

    @Test
    void getOrThrow_onInvalid_shouldThrowMappedException() {
        assertThatThrownBy(() ->
            Validated.<String, Integer>invalid("oops")
                .getOrThrow(e -> new RuntimeException(e)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("oops");
    }

    // ---------- Transformations ----------

    @Test
    void map_onValid_shouldApplyFunction() {
        Validated<String, Integer> result = Validated.<String, Integer>valid(5).map(n -> n * 2);
        assertThat(result.get()).isEqualTo(10);
    }

    @Test
    void map_onInvalid_shouldPreserveError() {
        Validated<String, Integer> result = Validated.<String, Integer>invalid("err").map(n -> n * 2);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getError()).isEqualTo("err");
    }

    @Test
    void mapError_onInvalid_shouldApplyFunction() {
        Validated<Integer, String> result = Validated.<String, String>invalid("err").mapError(String::length);
        assertThat(result.getError()).isEqualTo(3);
    }

    @Test
    void mapError_onValid_shouldPreserveValue() {
        Validated<Integer, String> result = Validated.<String, String>valid("ok").mapError(String::length);
        assertThat(result.isValid()).isTrue();
        assertThat(result.get()).isEqualTo("ok");
    }

    @Test
    void flatMap_onValid_shouldChain() {
        Validated<String, Integer> result = Validated.<String, Integer>valid(5)
            .flatMap(n -> Validated.valid(n + 1));
        assertThat(result.get()).isEqualTo(6);
    }

    @Test
    void flatMap_onValid_canProduceInvalid() {
        Validated<String, Integer> result = Validated.<String, Integer>valid(5)
            .flatMap(n -> Validated.invalid("nope"));
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getError()).isEqualTo("nope");
    }

    @Test
    void flatMap_onInvalid_shouldShortCircuit() {
        AtomicBoolean called = new AtomicBoolean(false);
        Validated<String, Integer> result = Validated.<String, Integer>invalid("err")
            .flatMap(n -> { called.set(true); return Validated.valid(n); });
        assertThat(called.get()).isFalse();
        assertThat(result.isInvalid()).isTrue();
    }

    // ---------- Side effects ----------

    @Test
    void peek_onValid_shouldExecuteAction() {
        AtomicReference<Integer> captured = new AtomicReference<>();
        Validated<String, Integer> v = Validated.<String, Integer>valid(42).peek(captured::set);
        assertThat(captured.get()).isEqualTo(42);
        assertThat(v.isValid()).isTrue();
    }

    @Test
    void peek_onInvalid_shouldNotExecuteAction() {
        AtomicBoolean called = new AtomicBoolean(false);
        Validated.<String, Integer>invalid("err").peek(n -> called.set(true));
        assertThat(called.get()).isFalse();
    }

    @Test
    void peekError_onInvalid_shouldExecuteAction() {
        AtomicReference<String> captured = new AtomicReference<>();
        Validated.<String, Integer>invalid("err").peekError(captured::set);
        assertThat(captured.get()).isEqualTo("err");
    }

    @Test
    void peekError_onValid_shouldNotExecuteAction() {
        AtomicBoolean called = new AtomicBoolean(false);
        Validated.<String, Integer>valid(1).peekError(e -> called.set(true));
        assertThat(called.get()).isFalse();
    }

    @Test
    void match_onValid_shouldCallOnValidConsumer() {
        AtomicReference<Integer> val = new AtomicReference<>();
        AtomicBoolean errCalled = new AtomicBoolean(false);
        Validated.<String, Integer>valid(7).match(val::set, e -> errCalled.set(true));
        assertThat(val.get()).isEqualTo(7);
        assertThat(errCalled.get()).isFalse();
    }

    @Test
    void match_onInvalid_shouldCallOnInvalidConsumer() {
        AtomicReference<String> err = new AtomicReference<>();
        AtomicBoolean valCalled = new AtomicBoolean(false);
        Validated.<String, Integer>invalid("boom").match(v -> valCalled.set(true), err::set);
        assertThat(err.get()).isEqualTo("boom");
        assertThat(valCalled.get()).isFalse();
    }

    // ---------- fold / stream ----------

    @Test
    void fold_onValid_shouldApplyOnValidFunction() {
        String result = Validated.<String, Integer>valid(5).fold(
            v -> "value:" + v,
            e -> "error:" + e
        );
        assertThat(result).isEqualTo("value:5");
    }

    @Test
    void fold_onInvalid_shouldApplyOnInvalidFunction() {
        String result = Validated.<String, Integer>invalid("bad").fold(
            v -> "value:" + v,
            e -> "error:" + e
        );
        assertThat(result).isEqualTo("error:bad");
    }

    @Test
    void stream_onValid_shouldReturnSingleElement() {
        List<Integer> list = Validated.<String, Integer>valid(42).stream().toList();
        assertThat(list).isEqualTo(List.of(42));
    }

    @Test
    void stream_onInvalid_shouldReturnEmptyStream() {
        List<Integer> list = Validated.<String, Integer>invalid("err").stream().toList();
        assertThat(list).isEmpty();
    }

    // ---------- sequence / traverse ----------

    @Test
    void sequence_allValid_shouldReturnValidList() {
        List<Validated<String, Integer>> items = List.of(
            Validated.valid(1), Validated.valid(2), Validated.valid(3));
        Validated<String, List<Integer>> result = Validated.sequence(items, (a, b) -> a + "; " + b);
        assertThat(result.isValid()).isTrue();
        assertThat(result.get()).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    void sequence_withOneInvalid_shouldReturnInvalid() {
        List<Validated<String, Integer>> items = List.of(
            Validated.valid(1), Validated.invalid("err1"), Validated.valid(3));
        Validated<String, List<Integer>> result = Validated.sequence(items, (a, b) -> a + "; " + b);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getError()).isEqualTo("err1");
    }

    @Test
    void sequence_withMultipleInvalid_shouldAccumulateErrors() {
        List<Validated<String, Integer>> items = List.of(
            Validated.invalid("e1"), Validated.valid(2), Validated.invalid("e2"));
        Validated<String, List<Integer>> result = Validated.sequence(items, (a, b) -> a + "; " + b);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getError()).isEqualTo("e1; e2");
    }

    @Test
    void sequence_stream_allValid_shouldReturnValidList() {
        Stream<Validated<String, Integer>> stream = Stream.of(
            Validated.valid(1), Validated.valid(2));
        Validated<String, List<Integer>> result = Validated.sequence(stream, (a, b) -> a + "; " + b);
        assertThat(result.isValid()).isTrue();
        assertThat(result.get()).isEqualTo(List.of(1, 2));
    }

    @Test
    void sequence_stream_shouldAccumulateErrors() {
        Stream<Validated<String, Integer>> stream = Stream.of(
            Validated.invalid("e1"), Validated.invalid("e2"));
        Validated<String, List<Integer>> result = Validated.sequence(stream, (a, b) -> a + "; " + b);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getError()).isEqualTo("e1; e2");
    }

    @Test
    void traverse_allValid_shouldReturnValidList() {
        List<Integer> inputs = List.of(1, 2, 3);
        Validated<String, List<String>> result = Validated.traverse(
            inputs,
            n -> Validated.valid("v" + n),
            (a, b) -> a + "; " + b
        );
        assertThat(result.isValid()).isTrue();
        assertThat(result.get()).isEqualTo(List.of("v1", "v2", "v3"));
    }

    @Test
    void traverse_shouldAccumulateErrors() {
        List<Integer> inputs = List.of(1, 2, 3);
        Validated<String, List<String>> result = Validated.traverse(
            inputs,
            n -> n % 2 == 0 ? Validated.invalid("bad:" + n) : Validated.valid("ok:" + n),
            (a, b) -> a + "; " + b
        );
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getError()).isEqualTo("bad:2");
    }
}
