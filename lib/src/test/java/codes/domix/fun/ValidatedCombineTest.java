package codes.domix.fun;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Validated#product} and {@link Validated#combine} (error-accumulating combination).
 */
class ValidatedCombineTest {

    private static final java.util.function.BinaryOperator<String> JOIN = (a, b) -> a + "; " + b;

    // ---------- product ----------

    @Test
    void product_both_valid_returns_valid_tuple() {
        Validated<String, Integer> a = Validated.valid(1);
        Validated<String, String> b = Validated.valid("hello");

        Validated<String, Tuple2<Integer, String>> result = a.product(b, JOIN);

        assertTrue(result.isValid());
        assertEquals(new Tuple2<>(1, "hello"), result.get());
    }

    @Test
    void product_this_invalid_other_valid_returns_this_error() {
        Validated<String, Integer> a = Validated.invalid("err-a");
        Validated<String, String> b = Validated.valid("hello");

        Validated<String, Tuple2<Integer, String>> result = a.product(b, JOIN);

        assertTrue(result.isInvalid());
        assertEquals("err-a", result.getError());
    }

    @Test
    void product_this_valid_other_invalid_returns_other_error() {
        Validated<String, Integer> a = Validated.valid(1);
        Validated<String, String> b = Validated.invalid("err-b");

        Validated<String, Tuple2<Integer, String>> result = a.product(b, JOIN);

        assertTrue(result.isInvalid());
        assertEquals("err-b", result.getError());
    }

    @Test
    void product_both_invalid_accumulates_errors() {
        Validated<String, Integer> a = Validated.invalid("err-a");
        Validated<String, String> b = Validated.invalid("err-b");

        Validated<String, Tuple2<Integer, String>> result = a.product(b, JOIN);

        assertTrue(result.isInvalid());
        assertEquals("err-a; err-b", result.getError());
    }

    // ---------- combine ----------

    @Test
    void combine_both_valid_merges_values() {
        Validated<String, Integer> a = Validated.valid(3);
        Validated<String, Integer> b = Validated.valid(4);

        Validated<String, Integer> result = a.combine(b, JOIN, Integer::sum);

        assertTrue(result.isValid());
        assertEquals(7, result.get());
    }

    @Test
    void combine_this_invalid_other_valid_returns_this_error() {
        Validated<String, Integer> a = Validated.invalid("missing-name");
        Validated<String, Integer> b = Validated.valid(25);

        Validated<String, Integer> result = a.combine(b, JOIN, Integer::sum);

        assertTrue(result.isInvalid());
        assertEquals("missing-name", result.getError());
    }

    @Test
    void combine_this_valid_other_invalid_returns_other_error() {
        Validated<String, Integer> a = Validated.valid(3);
        Validated<String, Integer> b = Validated.invalid("missing-age");

        Validated<String, Integer> result = a.combine(b, JOIN, Integer::sum);

        assertTrue(result.isInvalid());
        assertEquals("missing-age", result.getError());
    }

    @Test
    void combine_both_invalid_accumulates_errors() {
        Validated<String, Integer> a = Validated.invalid("missing-name");
        Validated<String, Integer> b = Validated.invalid("missing-age");

        Validated<String, Integer> result = a.combine(b, JOIN, Integer::sum);

        assertTrue(result.isInvalid());
        assertEquals("missing-name; missing-age", result.getError());
    }

    @Test
    void combine_with_list_error_accumulation() {
        Validated<List<String>, String> name = Validated.invalid(List.of("Name is required"));
        Validated<List<String>, Integer> age = Validated.invalid(List.of("Age must be positive"));

        Validated<List<String>, String> result = name.combine(
            age,
            (e1, e2) -> {
                java.util.ArrayList<String> merged = new java.util.ArrayList<>(e1);
                merged.addAll(e2);
                return List.copyOf(merged);
            },
            (n, a2) -> n + " age=" + a2
        );

        assertTrue(result.isInvalid());
        assertEquals(List.of("Name is required", "Age must be positive"), result.getError());
    }

    @Test
    void combine_chained_three_fields() {
        Validated<String, String> firstName = Validated.valid("Alice");
        Validated<String, String> lastName = Validated.invalid("lastName required");
        Validated<String, Integer> age = Validated.invalid("age required");

        // Chain: (firstName combine lastName) combine age
        Validated<String, String> step1 = firstName.combine(
            lastName, JOIN, (f, l) -> f + " " + l);
        Validated<String, String> finalResult = step1.combine(
            age, JOIN, (name, a) -> name + " age=" + a);

        assertTrue(finalResult.isInvalid());
        assertEquals("lastName required; age required", finalResult.getError());
    }
}
