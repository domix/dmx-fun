package codes.domix.fun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TryZipTest {

    // ── zip (two-way) ─────────────────────────────────────────────────────────

    @Test
    void zip_bothSuccess_returnsTuple2() {
        var result = Try.zip(Try.success(1), Try.success("a"));
        assertEquals(Try.success(new Tuple2<>(1, "a")), result);
    }

    @Test
    void zip_firstFailure_returnsFirstCause() {
        var cause = new RuntimeException("e1");
        var result = Try.zip(Try.failure(cause), Try.success("a"));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zip_secondFailure_returnsSecondCause() {
        var cause = new RuntimeException("e2");
        var result = Try.zip(Try.success(1), Try.failure(cause));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zip_bothFailure_returnsFirstCause() {
        var cause1 = new RuntimeException("e1");
        var cause2 = new RuntimeException("e2");
        var result = Try.zip(Try.failure(cause1), Try.failure(cause2));
        assertTrue(result.isFailure());
        assertEquals(cause1, result.getCause());
    }

    @Test
    void zip_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Try.zip(null, Try.success(1)));
        assertThrows(NullPointerException.class, () -> Try.zip(Try.success(1), null));
    }

    // ── zip3 ─────────────────────────────────────────────────────────────────

    @Test
    void zip3_allSuccess_returnsTuple3() {
        var result = Try.zip3(Try.success(1), Try.success("a"), Try.success(true));
        assertEquals(Try.success(new Tuple3<>(1, "a", true)), result);
    }

    @Test
    void zip3_firstFailure_returnsFirstCause() {
        var cause = new RuntimeException("e1");
        var result = Try.zip3(Try.failure(cause), Try.success("a"), Try.success(true));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zip3_secondFailure_returnsSecondCause() {
        var cause = new RuntimeException("e2");
        var result = Try.zip3(Try.success(1), Try.failure(cause), Try.success(true));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zip3_thirdFailure_returnsThirdCause() {
        var cause = new RuntimeException("e3");
        var result = Try.zip3(Try.success(1), Try.success("a"), Try.failure(cause));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zip3_failFast_firstFailureWins() {
        var cause1 = new RuntimeException("e1");
        var cause2 = new RuntimeException("e2");
        var cause3 = new RuntimeException("e3");
        var result = Try.zip3(Try.failure(cause1), Try.failure(cause2), Try.failure(cause3));
        assertTrue(result.isFailure());
        assertEquals(cause1, result.getCause());
    }

    @Test
    void zip3_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Try.zip3(null, Try.success(1), Try.success(2)));
        assertThrows(NullPointerException.class, () -> Try.zip3(Try.success(1), null, Try.success(2)));
        assertThrows(NullPointerException.class, () -> Try.zip3(Try.success(1), Try.success(2), null));
    }

    // ── zipWith3 ──────────────────────────────────────────────────────────────

    @Test
    void zipWith3_allSuccess_appliesCombiner() {
        var result = Try.<Integer, Integer, Integer, String>zipWith3(
            Try.success(1), Try.success(2), Try.success(3),
            (a, b, c) -> "sum=" + (a + b + c));
        assertEquals(Try.success("sum=6"), result);
    }

    @Test
    void zipWith3_firstFailure_returnsThatCause() {
        var cause = new RuntimeException("e1");
        var result = Try.<Integer, Integer, Integer, String>zipWith3(
            Try.failure(cause), Try.success(2), Try.success(3),
            (a, b, c) -> "sum=" + (a + b + c));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zipWith3_failFast_firstFailureWins() {
        var cause1 = new RuntimeException("e1");
        var cause2 = new RuntimeException("e2");
        var cause3 = new RuntimeException("e3");
        var result = Try.<Integer, Integer, Integer, String>zipWith3(
            Try.failure(cause1), Try.failure(cause2), Try.failure(cause3),
            (a, b, c) -> "sum=" + (a + b + c));
        assertTrue(result.isFailure());
        assertEquals(cause1, result.getCause());
    }

    @Test
    void zipWith3_nullCombiner_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Try.zipWith3(Try.success(1), Try.success(2), Try.success(3), null));
    }

    @Test
    void zipWith3_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Try.zipWith3(null, Try.success(1), Try.success(2), (a, b, c) -> a));
    }
}
