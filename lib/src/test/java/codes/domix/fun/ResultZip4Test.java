package codes.domix.fun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultZip4Test {

    // ── zip4 ──────────────────────────────────────────────────────────────────

    @Test
    void zip4_allOk_returnsTuple4() {
        var result = Result.zip4(Result.ok(1), Result.ok("a"), Result.ok(true), Result.ok(3.14));
        assertEquals(Result.ok(new Tuple4<>(1, "a", true, 3.14)), result);
    }

    @Test
    void zip4_firstErr_returnsFirstError() {
        var result = Result.<Integer, String, Boolean, Double, String>zip4(
            Result.err("e1"), Result.ok("a"), Result.ok(true), Result.ok(3.14));
        assertTrue(result.isError());
        assertEquals("e1", result.getError());
    }

    @Test
    void zip4_secondErr_returnsSecondError() {
        var result = Result.<Integer, String, Boolean, Double, String>zip4(
            Result.ok(1), Result.err("e2"), Result.ok(true), Result.ok(3.14));
        assertTrue(result.isError());
        assertEquals("e2", result.getError());
    }

    @Test
    void zip4_thirdErr_returnsThirdError() {
        var result = Result.<Integer, String, Boolean, Double, String>zip4(
            Result.ok(1), Result.ok("a"), Result.err("e3"), Result.ok(3.14));
        assertTrue(result.isError());
        assertEquals("e3", result.getError());
    }

    @Test
    void zip4_fourthErr_returnsFourthError() {
        var result = Result.<Integer, String, Boolean, Double, String>zip4(
            Result.ok(1), Result.ok("a"), Result.ok(true), Result.err("e4"));
        assertTrue(result.isError());
        assertEquals("e4", result.getError());
    }

    @Test
    void zip4_failFast_firstErrWins() {
        var result = Result.<Integer, String, Boolean, Double, String>zip4(
            Result.err("e1"), Result.err("e2"), Result.err("e3"), Result.err("e4"));
        assertEquals("e1", result.getError());
    }

    @Test
    void zip4_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Result.zip4(null, Result.ok(1), Result.ok(2), Result.ok(3)));
        assertThrows(NullPointerException.class,
            () -> Result.zip4(Result.ok(0), null, Result.ok(2), Result.ok(3)));
        assertThrows(NullPointerException.class,
            () -> Result.zip4(Result.ok(0), Result.ok(1), null, Result.ok(3)));
        assertThrows(NullPointerException.class,
            () -> Result.zip4(Result.ok(0), Result.ok(1), Result.ok(2), null));
    }

    // ── zipWith4 ──────────────────────────────────────────────────────────────

    @Test
    void zipWith4_allOk_appliesCombiner() {
        var result = Result.<Integer, Integer, Integer, Integer, String, String>zipWith4(
            Result.ok(1), Result.ok(2), Result.ok(3), Result.ok(4),
            (a, b, c, d) -> "sum=" + (a + b + c + d));
        assertEquals(Result.ok("sum=10"), result);
    }

    @Test
    void zipWith4_firstErr_returnsThatError() {
        var result = Result.<Integer, Integer, Integer, Integer, String, String>zipWith4(
            Result.err("e1"), Result.ok(2), Result.ok(3), Result.ok(4),
            (a, b, c, d) -> "sum=" + (a + b + c + d));
        assertTrue(result.isError());
        assertEquals("e1", result.getError());
    }

    @Test
    void zipWith4_failFast_firstErrWins() {
        var result = Result.<Integer, Integer, Integer, Integer, String, String>zipWith4(
            Result.err("e1"), Result.err("e2"), Result.err("e3"), Result.err("e4"),
            (a, b, c, d) -> "sum=" + (a + b + c + d));
        assertEquals("e1", result.getError());
    }

    @Test
    void zipWith4_nullCombiner_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Result.zipWith4(Result.ok(1), Result.ok(2), Result.ok(3), Result.ok(4), null));
    }

    @Test
    void zipWith4_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Result.zipWith4(null, Result.ok(1), Result.ok(2), Result.ok(3), (a, b, c, d) -> a));
        assertThrows(NullPointerException.class,
            () -> Result.zipWith4(Result.ok(0), null, Result.ok(2), Result.ok(3), (a, b, c, d) -> a));
        assertThrows(NullPointerException.class,
            () -> Result.zipWith4(Result.ok(0), Result.ok(1), null, Result.ok(3), (a, b, c, d) -> a));
        assertThrows(NullPointerException.class,
            () -> Result.zipWith4(Result.ok(0), Result.ok(1), Result.ok(2), null, (a, b, c, d) -> a));
    }

    @Test
    void zipWith4_combinerReturnsNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Result.zipWith4(Result.ok(1), Result.ok(2), Result.ok(3), Result.ok(4),
                (a, b, c, d) -> null));
    }
}
