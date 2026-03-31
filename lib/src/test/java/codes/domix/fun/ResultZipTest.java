package codes.domix.fun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultZipTest {

    // ── zip (two-way) ─────────────────────────────────────────────────────────

    @Test
    void zip_bothOk_returnsTuple2() {
        var result = Result.zip(Result.ok(1), Result.ok("a"));
        assertEquals(Result.ok(new Tuple2<>(1, "a")), result);
    }

    @Test
    void zip_firstErr_returnsFirstError() {
        Result<Integer, String> r1 = Result.err("e1");
        Result<String, String>  r2 = Result.ok("a");
        var result = Result.<Integer, String, String>zip(r1, r2);
        assertTrue(result.isError());
        assertEquals("e1", result.getError());
    }

    @Test
    void zip_secondErr_returnsSecondError() {
        Result<Integer, String> r1 = Result.ok(1);
        Result<String, String>  r2 = Result.err("e2");
        var result = Result.<Integer, String, String>zip(r1, r2);
        assertTrue(result.isError());
        assertEquals("e2", result.getError());
    }

    @Test
    void zip_bothErr_returnsFirstError() {
        Result<Integer, String> r1 = Result.err("e1");
        Result<String, String>  r2 = Result.err("e2");
        var result = Result.<Integer, String, String>zip(r1, r2);
        assertEquals("e1", result.getError());
    }

    @Test
    void zip_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Result.zip(null, Result.ok(1)));
        assertThrows(NullPointerException.class, () -> Result.zip(Result.ok(1), null));
    }

    // ── zip3 ─────────────────────────────────────────────────────────────────

    @Test
    void zip3_allOk_returnsTuple3() {
        var result = Result.zip3(Result.ok(1), Result.ok("a"), Result.ok(true));
        assertEquals(Result.ok(new Tuple3<>(1, "a", true)), result);
    }

    @Test
    void zip3_firstErr_returnsFirstError() {
        var result = Result.zip3(
            Result.<Integer, String>err("e1"),
            Result.<String, String>ok("a"),
            Result.<Boolean, String>ok(true));
        assertTrue(result.isError());
        assertEquals("e1", result.getError());
    }

    @Test
    void zip3_secondErr_returnsSecondError() {
        var result = Result.zip3(
            Result.<Integer, String>ok(1),
            Result.<String, String>err("e2"),
            Result.<Boolean, String>ok(true));
        assertTrue(result.isError());
        assertEquals("e2", result.getError());
    }

    @Test
    void zip3_thirdErr_returnsThirdError() {
        var result = Result.zip3(
            Result.<Integer, String>ok(1),
            Result.<String, String>ok("a"),
            Result.<Boolean, String>err("e3"));
        assertTrue(result.isError());
        assertEquals("e3", result.getError());
    }

    @Test
    void zip3_failFast_firstErrWins() {
        var result = Result.zip3(
            Result.<Integer, String>err("e1"),
            Result.<String, String>err("e2"),
            Result.<Boolean, String>err("e3"));
        assertEquals("e1", result.getError());
    }

    @Test
    void zip3_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Result.zip3(null, Result.ok(1), Result.ok(2)));
        assertThrows(NullPointerException.class, () -> Result.zip3(Result.ok(1), null, Result.ok(2)));
        assertThrows(NullPointerException.class, () -> Result.zip3(Result.ok(1), Result.ok(2), null));
    }

    // ── zipWith3 ──────────────────────────────────────────────────────────────

    @Test
    void zipWith3_allOk_appliesCombiner() {
        var result = Result.<Integer, Integer, Integer, String, String>zipWith3(
            Result.ok(1), Result.ok(2), Result.ok(3),
            (a, b, c) -> "sum=" + (a + b + c));
        assertEquals(Result.ok("sum=6"), result);
    }

    @Test
    void zipWith3_firstErr_returnsThatError() {
        var result = Result.<Integer, Integer, Integer, String, String>zipWith3(
            Result.err("e1"), Result.ok(2), Result.ok(3),
            (a, b, c) -> "sum=" + (a + b + c));
        assertTrue(result.isError());
        assertEquals("e1", result.getError());
    }

    @Test
    void zipWith3_failFast_firstErrWins() {
        var result = Result.<Integer, Integer, Integer, String, String>zipWith3(
            Result.err("e1"), Result.err("e2"), Result.err("e3"),
            (a, b, c) -> "sum=" + (a + b + c));
        assertEquals("e1", result.getError());
    }

    @Test
    void zipWith3_nullCombiner_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Result.zipWith3(Result.ok(1), Result.ok(2), Result.ok(3), null));
    }

    @Test
    void zipWith3_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Result.zipWith3(null, Result.ok(1), Result.ok(2), (a, b, c) -> a));
        assertThrows(NullPointerException.class,
            () -> Result.zipWith3(Result.ok(0), null, Result.ok(2), (a, b, c) -> a));
        assertThrows(NullPointerException.class,
            () -> Result.zipWith3(Result.ok(0), Result.ok(1), null, (a, b, c) -> a));
    }
}
