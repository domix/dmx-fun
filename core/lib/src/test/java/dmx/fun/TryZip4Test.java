package dmx.fun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TryZip4Test {

    // ── zip4 ──────────────────────────────────────────────────────────────────

    @Test
    void zip4_allSuccess_returnsTuple4() {
        var result = Try.zip4(Try.success(1), Try.success("a"), Try.success(true), Try.success(3.14));
        assertEquals(Try.success(new Tuple4<>(1, "a", true, 3.14)), result);
    }

    @Test
    void zip4_firstFailure_returnsFirstCause() {
        var cause = new RuntimeException("e1");
        var result = Try.zip4(Try.failure(cause), Try.success("a"), Try.success(true), Try.success(1));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zip4_secondFailure_returnsSecondCause() {
        var cause = new RuntimeException("e2");
        var result = Try.zip4(Try.success(1), Try.failure(cause), Try.success(true), Try.success(1));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zip4_thirdFailure_returnsThirdCause() {
        var cause = new RuntimeException("e3");
        var result = Try.zip4(Try.success(1), Try.success("a"), Try.failure(cause), Try.success(1));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zip4_fourthFailure_returnsFourthCause() {
        var cause = new RuntimeException("e4");
        var result = Try.zip4(Try.success(1), Try.success("a"), Try.success(true), Try.failure(cause));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zip4_failFast_firstFailureWins() {
        var cause1 = new RuntimeException("e1");
        var cause2 = new RuntimeException("e2");
        var cause3 = new RuntimeException("e3");
        var cause4 = new RuntimeException("e4");
        var result = Try.zip4(Try.failure(cause1), Try.failure(cause2), Try.failure(cause3), Try.failure(cause4));
        assertTrue(result.isFailure());
        assertEquals(cause1, result.getCause());
    }

    @Test
    void zip4_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Try.zip4(null, Try.success(1), Try.success(2), Try.success(3)));
        assertThrows(NullPointerException.class,
            () -> Try.zip4(Try.success(0), null, Try.success(2), Try.success(3)));
        assertThrows(NullPointerException.class,
            () -> Try.zip4(Try.success(0), Try.success(1), null, Try.success(3)));
        assertThrows(NullPointerException.class,
            () -> Try.zip4(Try.success(0), Try.success(1), Try.success(2), null));
    }

    // ── zipWith4 ──────────────────────────────────────────────────────────────

    @Test
    void zipWith4_allSuccess_appliesCombiner() {
        var result = Try.<Integer, Integer, Integer, Integer, String>zipWith4(
            Try.success(1), Try.success(2), Try.success(3), Try.success(4),
            (a, b, c, d) -> "sum=" + (a + b + c + d));
        assertEquals(Try.success("sum=10"), result);
    }

    @Test
    void zipWith4_firstFailure_returnsThatCause() {
        var cause = new RuntimeException("e1");
        var result = Try.<Integer, Integer, Integer, Integer, String>zipWith4(
            Try.failure(cause), Try.success(2), Try.success(3), Try.success(4),
            (a, b, c, d) -> "sum=" + (a + b + c + d));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zipWith4_secondFailure_returnsThatCause() {
        var cause = new RuntimeException("e2");
        var result = Try.<Integer, Integer, Integer, Integer, String>zipWith4(
            Try.success(1), Try.failure(cause), Try.success(3), Try.success(4),
            (a, b, c, d) -> "sum=" + (a + b + c + d));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zipWith4_thirdFailure_returnsThatCause() {
        var cause = new RuntimeException("e3");
        var result = Try.<Integer, Integer, Integer, Integer, String>zipWith4(
            Try.success(1), Try.success(2), Try.failure(cause), Try.success(4),
            (a, b, c, d) -> "sum=" + (a + b + c + d));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zipWith4_fourthFailure_returnsThatCause() {
        var cause = new RuntimeException("e4");
        var result = Try.<Integer, Integer, Integer, Integer, String>zipWith4(
            Try.success(1), Try.success(2), Try.success(3), Try.failure(cause),
            (a, b, c, d) -> "sum=" + (a + b + c + d));
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zipWith4_failFast_firstFailureWins() {
        var cause1 = new RuntimeException("e1");
        var cause2 = new RuntimeException("e2");
        var cause3 = new RuntimeException("e3");
        var cause4 = new RuntimeException("e4");
        var result = Try.<Integer, Integer, Integer, Integer, String>zipWith4(
            Try.failure(cause1), Try.failure(cause2), Try.failure(cause3), Try.failure(cause4),
            (a, b, c, d) -> "sum=" + (a + b + c + d));
        assertTrue(result.isFailure());
        assertEquals(cause1, result.getCause());
    }

    @Test
    void zipWith4_combinerThrows_returnsFailureWithCause() {
        var cause = new RuntimeException("combiner");
        var result = Try.<Integer, Integer, Integer, Integer, String>zipWith4(
            Try.success(1), Try.success(2), Try.success(3), Try.success(4),
            (a, b, c, d) -> { throw cause; });
        assertTrue(result.isFailure());
        assertEquals(cause, result.getCause());
    }

    @Test
    void zipWith4_combinerReturnsNull_returnsSuccessWithNull() {
        var result = Try.<Integer, Integer, Integer, Integer, String>zipWith4(
            Try.success(1), Try.success(2), Try.success(3), Try.success(4),
            (a, b, c, d) -> null);
        assertTrue(result.isSuccess());
        assertEquals(Try.success(null), result);
    }

    @Test
    void zipWith4_nullCombiner_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Try.zipWith4(Try.success(1), Try.success(2), Try.success(3), Try.success(4), null));
    }

    @Test
    void zipWith4_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Try.zipWith4(null, Try.success(1), Try.success(2), Try.success(3), (a, b, c, d) -> a));
        assertThrows(NullPointerException.class,
            () -> Try.zipWith4(Try.success(0), null, Try.success(2), Try.success(3), (a, b, c, d) -> a));
        assertThrows(NullPointerException.class,
            () -> Try.zipWith4(Try.success(0), Try.success(1), null, Try.success(3), (a, b, c, d) -> a));
        assertThrows(NullPointerException.class,
            () -> Try.zipWith4(Try.success(0), Try.success(1), Try.success(2), null, (a, b, c, d) -> a));
    }
}
