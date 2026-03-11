package codes.domix.fun;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatedTest {

    // ---------- Factories ----------

    @Test
    void valid_wraps_value() {
        Validated<String, Integer> v = Validated.valid(42);
        assertTrue(v.isValid());
        assertFalse(v.isInvalid());
        assertEquals(42, v.get());
    }

    @Test
    void invalid_wraps_error() {
        Validated<String, Integer> v = Validated.invalid("bad");
        assertFalse(v.isValid());
        assertTrue(v.isInvalid());
        assertEquals("bad", v.getError());
    }

    @Test
    void valid_rejects_null() {
        assertThrows(NullPointerException.class, () -> Validated.valid(null));
    }

    @Test
    void invalid_rejects_null() {
        assertThrows(NullPointerException.class, () -> Validated.invalid(null));
    }

    // ---------- Accessors ----------

    @Test
    void get_on_invalid_throws() {
        assertThrows(NoSuchElementException.class, () -> Validated.invalid("err").get());
    }

    @Test
    void getError_on_valid_throws() {
        assertThrows(NoSuchElementException.class, () -> Validated.valid(1).getError());
    }

    @Test
    void getOrElse_valid_returns_value() {
        assertEquals(42, Validated.<String, Integer>valid(42).getOrElse(0));
    }

    @Test
    void getOrElse_invalid_returns_fallback() {
        assertEquals(0, Validated.<String, Integer>invalid("err").getOrElse(0));
    }

    @Test
    void getOrElseGet_valid_returns_value() {
        assertEquals(42, Validated.<String, Integer>valid(42).getOrElseGet(() -> 0));
    }

    @Test
    void getOrElseGet_invalid_returns_supplied() {
        assertEquals(0, Validated.<String, Integer>invalid("err").getOrElseGet(() -> 0));
    }

    @Test
    void getOrNull_valid_returns_value() {
        assertEquals(42, Validated.<String, Integer>valid(42).getOrNull());
    }

    @Test
    void getOrNull_invalid_returns_null() {
        assertNull(Validated.<String, Integer>invalid("err").getOrNull());
    }

    @Test
    void getOrThrow_valid_returns_value() {
        assertEquals(42, Validated.<String, Integer>valid(42)
            .getOrThrow(e -> new RuntimeException(e)));
    }

    @Test
    void getOrThrow_invalid_throws_mapped_exception() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            Validated.<String, Integer>invalid("oops")
                .getOrThrow(e -> new RuntimeException(e)));
        assertEquals("oops", ex.getMessage());
    }

    // ---------- Transformations ----------

    @Test
    void map_valid_applies_function() {
        Validated<String, Integer> result = Validated.<String, Integer>valid(5).map(n -> n * 2);
        assertEquals(10, result.get());
    }

    @Test
    void map_invalid_preserves_error() {
        Validated<String, Integer> result = Validated.<String, Integer>invalid("err").map(n -> n * 2);
        assertTrue(result.isInvalid());
        assertEquals("err", result.getError());
    }

    @Test
    void mapError_invalid_applies_function() {
        Validated<Integer, String> result = Validated.<String, String>invalid("err").mapError(String::length);
        assertEquals(3, result.getError());
    }

    @Test
    void mapError_valid_preserves_value() {
        Validated<Integer, String> result = Validated.<String, String>valid("ok").mapError(String::length);
        assertTrue(result.isValid());
        assertEquals("ok", result.get());
    }

    @Test
    void flatMap_valid_chains() {
        Validated<String, Integer> result = Validated.<String, Integer>valid(5)
            .flatMap(n -> Validated.valid(n + 1));
        assertEquals(6, result.get());
    }

    @Test
    void flatMap_valid_can_produce_invalid() {
        Validated<String, Integer> result = Validated.<String, Integer>valid(5)
            .flatMap(n -> Validated.invalid("nope"));
        assertTrue(result.isInvalid());
        assertEquals("nope", result.getError());
    }

    @Test
    void flatMap_invalid_short_circuits() {
        AtomicBoolean called = new AtomicBoolean(false);
        Validated<String, Integer> result = Validated.<String, Integer>invalid("err")
            .flatMap(n -> { called.set(true); return Validated.valid(n); });
        assertFalse(called.get());
        assertTrue(result.isInvalid());
    }

    // ---------- Side effects ----------

    @Test
    void peek_valid_executes_action() {
        AtomicReference<Integer> captured = new AtomicReference<>();
        Validated<String, Integer> v = Validated.<String, Integer>valid(42).peek(captured::set);
        assertEquals(42, captured.get());
        assertTrue(v.isValid());
    }

    @Test
    void peek_invalid_does_not_execute() {
        AtomicBoolean called = new AtomicBoolean(false);
        Validated.<String, Integer>invalid("err").peek(n -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    void peekError_invalid_executes_action() {
        AtomicReference<String> captured = new AtomicReference<>();
        Validated.<String, Integer>invalid("err").peekError(captured::set);
        assertEquals("err", captured.get());
    }

    @Test
    void peekError_valid_does_not_execute() {
        AtomicBoolean called = new AtomicBoolean(false);
        Validated.<String, Integer>valid(1).peekError(e -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    void match_valid_calls_onValid() {
        AtomicReference<Integer> val = new AtomicReference<>();
        AtomicBoolean errCalled = new AtomicBoolean(false);
        Validated.<String, Integer>valid(7).match(val::set, e -> errCalled.set(true));
        assertEquals(7, val.get());
        assertFalse(errCalled.get());
    }

    @Test
    void match_invalid_calls_onInvalid() {
        AtomicReference<String> err = new AtomicReference<>();
        AtomicBoolean valCalled = new AtomicBoolean(false);
        Validated.<String, Integer>invalid("boom").match(v -> valCalled.set(true), err::set);
        assertEquals("boom", err.get());
        assertFalse(valCalled.get());
    }

    // ---------- fold / stream ----------

    @Test
    void fold_valid_applies_onValid() {
        String result = Validated.<String, Integer>valid(5).fold(
            v -> "value:" + v,
            e -> "error:" + e
        );
        assertEquals("value:5", result);
    }

    @Test
    void fold_invalid_applies_onInvalid() {
        String result = Validated.<String, Integer>invalid("bad").fold(
            v -> "value:" + v,
            e -> "error:" + e
        );
        assertEquals("error:bad", result);
    }

    @Test
    void stream_valid_returns_single_element() {
        List<Integer> list = Validated.<String, Integer>valid(42).stream().toList();
        assertEquals(List.of(42), list);
    }

    @Test
    void stream_invalid_returns_empty() {
        List<Integer> list = Validated.<String, Integer>invalid("err").stream().toList();
        assertTrue(list.isEmpty());
    }

    // ---------- sequence / traverse ----------

    @Test
    void sequence_all_valid_returns_valid_list() {
        List<Validated<String, Integer>> items = List.of(
            Validated.valid(1), Validated.valid(2), Validated.valid(3));
        Validated<String, List<Integer>> result = Validated.sequence(items, (a, b) -> a + "; " + b);
        assertTrue(result.isValid());
        assertEquals(List.of(1, 2, 3), result.get());
    }

    @Test
    void sequence_with_one_invalid_returns_invalid() {
        List<Validated<String, Integer>> items = List.of(
            Validated.valid(1), Validated.invalid("err1"), Validated.valid(3));
        Validated<String, List<Integer>> result = Validated.sequence(items, (a, b) -> a + "; " + b);
        assertTrue(result.isInvalid());
        assertEquals("err1", result.getError());
    }

    @Test
    void sequence_with_multiple_invalid_accumulates_errors() {
        List<Validated<String, Integer>> items = List.of(
            Validated.invalid("e1"), Validated.valid(2), Validated.invalid("e2"));
        Validated<String, List<Integer>> result = Validated.sequence(items, (a, b) -> a + "; " + b);
        assertTrue(result.isInvalid());
        assertEquals("e1; e2", result.getError());
    }

    @Test
    void sequence_stream_all_valid() {
        Stream<Validated<String, Integer>> stream = Stream.of(
            Validated.valid(1), Validated.valid(2));
        Validated<String, List<Integer>> result = Validated.sequence(stream, (a, b) -> a + "; " + b);
        assertTrue(result.isValid());
        assertEquals(List.of(1, 2), result.get());
    }

    @Test
    void sequence_stream_accumulates_errors() {
        Stream<Validated<String, Integer>> stream = Stream.of(
            Validated.invalid("e1"), Validated.invalid("e2"));
        Validated<String, List<Integer>> result = Validated.sequence(stream, (a, b) -> a + "; " + b);
        assertTrue(result.isInvalid());
        assertEquals("e1; e2", result.getError());
    }

    @Test
    void traverse_all_valid() {
        List<Integer> inputs = List.of(1, 2, 3);
        Validated<String, List<String>> result = Validated.traverse(
            inputs,
            n -> Validated.valid("v" + n),
            (a, b) -> a + "; " + b
        );
        assertTrue(result.isValid());
        assertEquals(List.of("v1", "v2", "v3"), result.get());
    }

    @Test
    void traverse_accumulates_errors() {
        List<Integer> inputs = List.of(1, 2, 3);
        Validated<String, List<String>> result = Validated.traverse(
            inputs,
            n -> n % 2 == 0 ? Validated.invalid("bad:" + n) : Validated.valid("ok:" + n),
            (a, b) -> a + "; " + b
        );
        assertTrue(result.isInvalid());
        assertEquals("bad:2", result.getError());
    }
}
