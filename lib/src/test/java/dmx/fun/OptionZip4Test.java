package dmx.fun;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptionZip4Test {

    // ── zip4 (static) ─────────────────────────────────────────────────────────

    @Test
    void zip4_static_allSome_returnsSomeTuple4() {
        var result = Option.zip4(Option.some(1), Option.some("a"), Option.some(true), Option.some(3.14));
        assertEquals(Option.some(new Tuple4<>(1, "a", true, 3.14)), result);
    }

    @Test
    void zip4_static_firstIsNone_returnsNone() {
        assertEquals(Option.none(),
            Option.zip4(Option.none(), Option.some("a"), Option.some(true), Option.some(1)));
    }

    @Test
    void zip4_static_secondIsNone_returnsNone() {
        assertEquals(Option.none(),
            Option.zip4(Option.some(1), Option.none(), Option.some(true), Option.some(1)));
    }

    @Test
    void zip4_static_thirdIsNone_returnsNone() {
        assertEquals(Option.none(),
            Option.zip4(Option.some(1), Option.some("a"), Option.none(), Option.some(1)));
    }

    @Test
    void zip4_static_fourthIsNone_returnsNone() {
        assertEquals(Option.none(),
            Option.zip4(Option.some(1), Option.some("a"), Option.some(true), Option.none()));
    }

    @Test
    void zip4_static_allNone_returnsNone() {
        assertEquals(Option.none(),
            Option.zip4(Option.none(), Option.none(), Option.none(), Option.none()));
    }

    @Test
    void zip4_static_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.zip4(null, Option.some(1), Option.some(2), Option.some(3)));
        assertThrows(NullPointerException.class,
            () -> Option.zip4(Option.some(1), null, Option.some(2), Option.some(3)));
        assertThrows(NullPointerException.class,
            () -> Option.zip4(Option.some(1), Option.some(2), null, Option.some(3)));
        assertThrows(NullPointerException.class,
            () -> Option.zip4(Option.some(1), Option.some(2), Option.some(3), null));
    }

    // ── zip4 (instance) ───────────────────────────────────────────────────────

    @Test
    void zip4_instanceMethod_allSome_returnsSomeTuple4() {
        var result = Option.some(1).zip4(Option.some("a"), Option.some(true), Option.some(3.14));
        assertEquals(Option.some(new Tuple4<>(1, "a", true, 3.14)), result);
    }

    @Test
    void zip4_instanceMethod_anyNone_returnsNone() {
        assertEquals(Option.none(),
            Option.some(1).zip4(Option.none(), Option.some(true), Option.some(2)));
    }

    @Test
    void zip4_instanceMethod_nullB_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zip4(null, Option.some(2), Option.some(3)));
    }

    @Test
    void zip4_instanceMethod_nullC_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zip4(Option.some(2), null, Option.some(3)));
    }

    @Test
    void zip4_instanceMethod_nullD_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zip4(Option.some(2), Option.some(3), null));
    }

    // ── map4 (static) ─────────────────────────────────────────────────────────

    @Test
    void map4_static_allSome_appliesCombiner() {
        var result = Option.map4(Option.some(1), Option.some(2), Option.some(3), Option.some(4),
            (a, b, c, d) -> a + b + c + d);
        assertEquals(Option.some(10), result);
    }

    @Test
    void map4_static_anyNone_returnsNone() {
        var result = Option.<Integer, Integer, Integer, Integer, Integer>map4(
            Option.some(1), Option.none(), Option.some(3), Option.some(4),
            (a, b, c, d) -> a + b + c + d);
        assertEquals(Option.none(), result);
    }

    @Test
    void map4_static_combinerNotCalledWhenAnyNone() {
        var called = new AtomicBoolean(false);
        Option.map4(Option.none(), Option.some(2), Option.some(3), Option.some(4),
            (a, b, c, d) -> { called.set(true); return 0; });
        assertFalse(called.get(), "combiner must not be called when any option is None");
    }

    @Test
    void map4_static_nullResultTreatedAsNone() {
        var result = Option.map4(Option.some(1), Option.some(2), Option.some(3), Option.some(4),
            (a, b, c, d) -> null);
        assertEquals(Option.none(), result);
    }

    @Test
    void map4_static_nullCombiner_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.map4(Option.some(1), Option.some(2), Option.some(3), Option.some(4), null));
    }

    @Test
    void map4_static_nullA_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.map4(null, Option.some(2), Option.some(3), Option.some(4), (a, b, c, d) -> a));
    }

    @Test
    void map4_static_nullB_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.map4(Option.some(1), null, Option.some(3), Option.some(4), (a, b, c, d) -> a));
    }

    @Test
    void map4_static_nullC_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.map4(Option.some(1), Option.some(2), null, Option.some(4), (a, b, c, d) -> a));
    }

    @Test
    void map4_static_nullD_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.map4(Option.some(1), Option.some(2), Option.some(3), null, (a, b, c, d) -> a));
    }

    // ── zipWith4 (instance) ───────────────────────────────────────────────────

    @Test
    void zipWith4_instanceMethod_allSome_appliesCombiner() {
        var result = Option.some(1).zipWith4(Option.some(2), Option.some(3), Option.some(4),
            (a, b, c, d) -> "sum=" + (a + b + c + d));
        assertEquals(Option.some("sum=10"), result);
    }

    @Test
    void zipWith4_instanceMethod_anyNone_returnsNone() {
        var result = Option.some(1).<Integer, Integer, Integer, Integer>zipWith4(
            Option.none(), Option.some(3), Option.some(4),
            (a, b, c, d) -> a + b + c + d);
        assertEquals(Option.none(), result);
    }

    @Test
    void zipWith4_instanceMethod_nullCombiner_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zipWith4(Option.some(2), Option.some(3), Option.some(4), null));
    }

    @Test
    void zipWith4_instanceMethod_nullB_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zipWith4(null, Option.some(3), Option.some(4), (a, b, c, d) -> a));
    }

    @Test
    void zipWith4_instanceMethod_nullC_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zipWith4(Option.some(2), null, Option.some(4), (a, b, c, d) -> a));
    }

    @Test
    void zipWith4_instanceMethod_nullD_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zipWith4(Option.some(2), Option.some(3), null, (a, b, c, d) -> a));
    }
}
