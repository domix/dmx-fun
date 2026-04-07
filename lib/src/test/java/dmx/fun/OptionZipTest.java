package dmx.fun;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptionZipTest {

    @Test
    void zip_shouldCombineTwoSomes() {
        Tuple2<Integer, String> expected = new Tuple2<>(1, "a");
        Option<Tuple2<Integer, String>> r = Option.zip(Option.some(1), Option.some("a"));

        assertEquals(Option.some(expected), r);
    }

    @Test
    void zip_shouldReturnNone_ifEitherIsNone() {
        assertEquals(Option.none(), Option.zip(Option.none(), Option.some("a")));
        assertEquals(Option.none(), Option.zip(Option.some(1), Option.none()));
        assertEquals(Option.none(), Option.zip(Option.none(), Option.none()));
    }

    @Test
    void zip_instanceMethod_shouldWork() {
        Option<Tuple2<Integer, String>> r = Option.some(1).zip(Option.some("a"));
        assertEquals(Option.some(new Tuple2<>(1, "a")), r);
    }

    @Test
    void map2_shouldCombineTwoSomes() {
        Option<String> r = Option.map2(Option.some(2), Option.some(3), (a, b) -> "v:" + (a + b));
        assertEquals(Option.some("v:5"), r);
    }

    @Test
    void map2_shouldReturnNone_ifEitherIsNone() {
        Option<String> r1 = Option.map2(Option.none(), Option.some(3), (a, b) -> "x");
        Option<String> r2 = Option.map2(Option.some(2), Option.none(), (a, b) -> "x");
        assertEquals(Option.none(), r1);
        assertEquals(Option.none(), r2);
    }

    @Test
    void map2_shouldBeLazy_whenAnySideIsNone() {
        AtomicBoolean called = new AtomicBoolean(false);

        Option<String> r = Option.map2(
            Option.none(),
            Option.some(1),
            (a, b) -> {
                called.set(true);
                return "boom";
            }
        );

        assertEquals(Option.none(), r);
        assertFalse(called.get(), "combiner must not be called when any side is None");
    }

    @Test
    void map2_shouldTurnNullResultIntoNone() {
        Option<String> r = Option.map2(Option.some(1), Option.some(2), (a, b) -> null);
        assertEquals(Option.none(), r);
    }

    @Test
    void zipWith_instanceMethod_shouldWork() {
        Option<String> r = Option.some(2).zipWith(Option.some(3), (a, b) -> "sum=" + (a + b));
        assertEquals(Option.some("sum=5"), r);
    }

    @Test
    void zipWith_shouldThrowNPE_ifCombinerIsNull() {
        assertThrows(NullPointerException.class, () -> Option.some(1).zipWith(Option.some(2), null));
    }

    @Test
    void zip_shouldThrowNPE_ifOtherIsNull() {
        assertThrows(NullPointerException.class, () -> Option.some(1).zip(null));
    }

    // ── zip3 / zipWith3 / map3 ────────────────────────────────────────────────

    @Test
    void zip3_static_allSome_returnsSomeTuple3() {
        var result = Option.zip3(Option.some(1), Option.some("a"), Option.some(true));
        assertEquals(Option.some(new Tuple3<>(1, "a", true)), result);
    }

    @Test
    void zip3_static_firstIsNone_returnsNone() {
        assertEquals(Option.none(), Option.zip3(Option.none(), Option.some("a"), Option.some(true)));
    }

    @Test
    void zip3_static_secondIsNone_returnsNone() {
        assertEquals(Option.none(), Option.zip3(Option.some(1), Option.none(), Option.some(true)));
    }

    @Test
    void zip3_static_thirdIsNone_returnsNone() {
        assertEquals(Option.none(), Option.zip3(Option.some(1), Option.some("a"), Option.none()));
    }

    @Test
    void zip3_static_allNone_returnsNone() {
        assertEquals(Option.none(), Option.zip3(Option.none(), Option.none(), Option.none()));
    }

    @Test
    void zip3_static_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Option.zip3(null, Option.some("a"), Option.some(1)));
        assertThrows(NullPointerException.class, () -> Option.zip3(Option.some(1), null, Option.some(1)));
        assertThrows(NullPointerException.class, () -> Option.zip3(Option.some(1), Option.some("a"), null));
    }

    @Test
    void zip3_instanceMethod_allSome_returnsSomeTuple3() {
        var result = Option.some(1).zip3(Option.some("a"), Option.some(true));
        assertEquals(Option.some(new Tuple3<>(1, "a", true)), result);
    }

    @Test
    void zip3_instanceMethod_anyNone_returnsNone() {
        assertEquals(Option.none(), Option.some(1).zip3(Option.none(), Option.some(true)));
    }

    @Test
    void map3_static_allSome_appliesCombiner() {
        var result = Option.map3(Option.some(1), Option.some(2), Option.some(3),
            (a, b, c) -> a + b + c);
        assertEquals(Option.some(6), result);
    }

    @Test
    void map3_static_anyNone_returnsNone() {
        var result = Option.<Integer, Integer, Integer, Integer>map3(
            Option.some(1), Option.none(), Option.some(3),
            (a, b, c) -> a + b + c);
        assertEquals(Option.none(), result);
    }

    @Test
    void map3_static_combinerNotCalledWhenAnyNone() {
        var called = new java.util.concurrent.atomic.AtomicBoolean(false);
        Option.map3(Option.none(), Option.some(2), Option.some(3),
            (a, b, c) -> { called.set(true); return 0; });
        assertFalse(called.get(), "combiner must not be called when any option is None");
    }

    @Test
    void map3_static_nullResultTreatedAsNone() {
        var result = Option.map3(Option.some(1), Option.some(2), Option.some(3),
            (a, b, c) -> null);
        assertEquals(Option.none(), result);
    }

    @Test
    void map3_static_nullCombiner_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.map3(Option.some(1), Option.some(2), Option.some(3), null));
    }

    @Test
    void map3_static_nullA_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.map3(null, Option.some(2), Option.some(3), (a, b, c) -> a));
    }

    @Test
    void map3_static_nullB_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.map3(Option.some(1), null, Option.some(3), (a, b, c) -> a));
    }

    @Test
    void map3_static_nullC_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.map3(Option.some(1), Option.some(2), null, (a, b, c) -> a));
    }

    @Test
    void zipWith3_instanceMethod_allSome_appliesCombiner() {
        var result = Option.some(1).zipWith3(Option.some(2), Option.some(3),
            (a, b, c) -> "sum=" + (a + b + c));
        assertEquals(Option.some("sum=6"), result);
    }

    @Test
    void zipWith3_instanceMethod_anyNone_returnsNone() {
        var result = Option.some(1).<Integer, Integer, Integer>zipWith3(Option.none(), Option.some(3),
            (a, b, c) -> a + b + c);
        assertEquals(Option.none(), result);
    }

    @Test
    void zipWith3_instanceMethod_nullCombiner_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zipWith3(Option.some(2), Option.some(3), null));
    }

    @Test
    void zipWith3_instanceMethod_nullB_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zipWith3(null, Option.some(3), (a, b, c) -> a));
    }

    @Test
    void zipWith3_instanceMethod_nullC_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zipWith3(Option.some(2), null, (a, b, c) -> a));
    }

    @Test
    void zip3_instanceMethod_nullB_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zip3(null, Option.some(true)));
    }

    @Test
    void zip3_instanceMethod_nullC_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> Option.some(1).zip3(Option.some("a"), null));
    }
}

