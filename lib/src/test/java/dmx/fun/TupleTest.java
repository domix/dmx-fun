package dmx.fun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TupleTest {

    // ── Tuple2 ────────────────────────────────────────────────────────────────

    @Test
    void tuple2_of_exposesFields() {
        var t = Tuple2.of("a", 2);
        assertEquals("a", t._1());
        assertEquals(2, t._2());
    }

    @Test
    void tuple2_mapFirst_appliesFunction() {
        var t = Tuple2.of(1, "b").mapFirst(n -> n * 10);
        assertEquals(10, t._1());
        assertEquals("b", t._2());
    }

    @Test
    void tuple2_mapSecond_appliesFunction() {
        var t = Tuple2.of(1, "b").mapSecond(String::toUpperCase);
        assertEquals(1, t._1());
        assertEquals("B", t._2());
    }

    @Test
    void tuple2_map_collapsesToValue() {
        var result = Tuple2.of(3, 4).map(Integer::sum);
        assertEquals(7, result);
    }

    @Test
    void tuple2_equality() {
        assertEquals(Tuple2.of("x", 1), Tuple2.of("x", 1));
    }

    // ── Tuple3 ────────────────────────────────────────────────────────────────

    @Test
    void tuple3_of_exposesFields() {
        var t = Tuple3.of("a", 2, 3.0);
        assertEquals("a", t._1());
        assertEquals(2, t._2());
        assertEquals(3.0, t._3());
    }

    @Test
    void tuple3_null_first_throws() {
        assertThrows(NullPointerException.class, () -> Tuple3.of(null, 2, 3));
    }

    @Test
    void tuple3_null_second_throws() {
        assertThrows(NullPointerException.class, () -> Tuple3.of(1, null, 3));
    }

    @Test
    void tuple3_null_third_throws() {
        assertThrows(NullPointerException.class, () -> Tuple3.of(1, 2, null));
    }

    @Test
    void tuple3_mapFirst_appliesFunction() {
        var t = Tuple3.of(1, "b", 3.0).mapFirst(n -> n * 10);
        assertEquals(10, t._1());
        assertEquals("b", t._2());
        assertEquals(3.0, t._3());
    }

    @Test
    void tuple3_mapSecond_appliesFunction() {
        var t = Tuple3.of(1, "b", 3.0).mapSecond(String::toUpperCase);
        assertEquals(1, t._1());
        assertEquals("B", t._2());
        assertEquals(3.0, t._3());
    }

    @Test
    void tuple3_mapThird_appliesFunction() {
        var t = Tuple3.of(1, "b", 3.0).mapThird(d -> d * 2);
        assertEquals(1, t._1());
        assertEquals("b", t._2());
        assertEquals(6.0, t._3());
    }

    @Test
    void tuple3_map_collapsesToValue() {
        var result = Tuple3.of(1, 2, 3).map((a, b, c) -> a + b + c);
        assertEquals(6, result);
    }

    @Test
    void tuple3_equality() {
        assertEquals(Tuple3.of("x", 1, true), Tuple3.of("x", 1, true));
    }

    // ── Tuple4 ────────────────────────────────────────────────────────────────

    @Test
    void tuple4_of_exposesFields() {
        var t = Tuple4.of("a", 2, 3.0, true);
        assertEquals("a", t._1());
        assertEquals(2, t._2());
        assertEquals(3.0, t._3());
        assertEquals(true, t._4());
    }

    @Test
    void tuple4_null_first_throws() {
        assertThrows(NullPointerException.class, () -> Tuple4.of(null, 2, 3, 4));
    }

    @Test
    void tuple4_null_second_throws() {
        assertThrows(NullPointerException.class, () -> Tuple4.of(1, null, 3, 4));
    }

    @Test
    void tuple4_null_third_throws() {
        assertThrows(NullPointerException.class, () -> Tuple4.of(1, 2, null, 4));
    }

    @Test
    void tuple4_null_fourth_throws() {
        assertThrows(NullPointerException.class, () -> Tuple4.of(1, 2, 3, null));
    }

    @Test
    void tuple4_mapFirst_appliesFunction() {
        var t = Tuple4.of(1, "b", 3.0, true).mapFirst(n -> n * 10);
        assertEquals(10, t._1());
        assertEquals("b", t._2());
        assertEquals(3.0, t._3());
        assertEquals(true, t._4());
    }

    @Test
    void tuple4_mapSecond_appliesFunction() {
        var t = Tuple4.of(1, "b", 3.0, true).mapSecond(String::toUpperCase);
        assertEquals(1, t._1());
        assertEquals("B", t._2());
        assertEquals(3.0, t._3());
        assertEquals(true, t._4());
    }

    @Test
    void tuple4_mapThird_appliesFunction() {
        var t = Tuple4.of(1, "b", 3.0, true).mapThird(d -> d * 2);
        assertEquals(1, t._1());
        assertEquals("b", t._2());
        assertEquals(6.0, t._3());
        assertEquals(true, t._4());
    }

    @Test
    void tuple4_mapFourth_appliesFunction() {
        var t = Tuple4.of(1, "b", 3.0, true).mapFourth(b -> !b);
        assertEquals(1, t._1());
        assertEquals("b", t._2());
        assertEquals(3.0, t._3());
        assertEquals(false, t._4());
    }

    @Test
    void tuple4_map_collapsesToValue() {
        var result = Tuple4.of(1, 2, 3, 4).map((a, b, c, d) -> a + b + c + d);
        assertEquals(10, result);
    }

    @Test
    void tuple4_equality() {
        assertEquals(Tuple4.of("x", 1, true, 4.0), Tuple4.of("x", 1, true, 4.0));
    }
}
