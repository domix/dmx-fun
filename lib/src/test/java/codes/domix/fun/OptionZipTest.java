package codes.domix.fun;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptionZipTest {

    @Test
    void zip_shouldCombineTwoSomes() {
        Option.Tuple2<Integer, String> expected = new Option.Tuple2<>(1, "a");
        Option<Option.Tuple2<Integer, String>> r = Option.zip(Option.some(1), Option.some("a"));

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
        Option<Option.Tuple2<Integer, String>> r = Option.some(1).zip(Option.some("a"));
        assertEquals(Option.some(new Option.Tuple2<>(1, "a")), r);
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
}

