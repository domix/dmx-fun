package codes.domix.fun;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class OptionTest {

    @Test
    void some_shouldRejectNull() {
        assertThrows(NullPointerException.class, () -> Option.some(null));
    }

    @Test
    void ofNullable_shouldMapNullToNone() {
        assertTrue(Option.ofNullable(null).isEmpty());
        assertEquals(10, Option.ofNullable(10).get());
    }

    @Test
    void get_onNone_shouldThrow() {
        assertThrows(NoSuchElementException.class, () -> Option.none().get());
    }

    @Test
    void map_shouldTransformSome_andPropagateNone() {
        assertEquals(Option.some("v:2"), Option.some(2).map(v -> "v:" + v));
        assertEquals(Option.none(), Option.<Integer>none().map(v -> v + 1));
    }

    @Test
    void map_shouldTurnNullIntoNone() {
        assertEquals(Option.none(), Option.some(1).map(v -> null));
    }

    @Test
    void flatMap_shouldChainSome_andPropagateNone() {
        Option<Integer> res = Option.some(2).flatMap(v -> Option.some(v * 10));
        assertEquals(Option.some(20), res);

        Option<Integer> none = Option.<Integer>none().flatMap(v -> Option.some(v * 10));
        assertEquals(Option.none(), none);
    }

    @Test
    void filter_shouldKeepOrDropValue() {
        assertEquals(Option.some(10), Option.some(10).filter(v -> v > 0));
        assertEquals(Option.none(), Option.some(10).filter(v -> v < 0));
        assertEquals(Option.none(), Option.<Integer>none().filter(v -> true));
    }

    @Test
    void fold_shouldChooseBranch() {
        String a = Option.some(5).fold(() -> "none", v -> "some:" + v);
        String b = Option.<Integer>none().fold(() -> "none", v -> "some:" + v);

        assertEquals("some:5", a);
        assertEquals("none", b);
    }

    @Test
    void stream_shouldExposeSomeAsSingleElementStream_andNoneAsEmpty() {
        assertEquals(List.of(7), Option.some(7).stream().toList());
        assertEquals(List.of(), Option.<Integer>none().stream().toList());
    }

    @Test
    void collectPresent_shouldFlattenOptions() {
        List<Integer> values = Option.collectPresent(Stream.of(
            Option.some(1),
            Option.none(),
            Option.some(3)
        ));

        assertEquals(List.of(1, 3), values);
    }

    @Test
    void presentValuesToList_collector_shouldWork() {
        List<Integer> values = Stream.of(
            Option.some(1),
            Option.none(),
            Option.some(3)
        ).collect(Option.presentValuesToList());

        assertEquals(List.of(1, 3), values);
    }

    @Test
    void fromOptional_toOptional_roundtrip() {
        Option<Integer> a = Option.fromOptional(Optional.of(10));
        Option<Integer> b = Option.fromOptional(Optional.empty());

        assertEquals(Optional.of(10), a.toOptional());
        assertEquals(Optional.empty(), b.toOptional());
    }

    // ---------- sequence / traverse ----------

    @Test
    void sequence_iterable_allSome_shouldReturnSomeList() {
        Option<List<Integer>> r = Option.sequence(List.of(
            Option.some(1),
            Option.some(2),
            Option.some(3)
        ));

        assertTrue(r.isDefined());
        assertEquals(List.of(1, 2, 3), r.get());
    }

    @Test
    void sequence_iterable_withNone_shouldReturnNone() {
        Option<List<Integer>> r = Option.sequence(List.of(
            Option.some(1),
            Option.none(),
            Option.some(3)
        ));

        assertTrue(r.isEmpty());
    }

    @Test
    void sequence_stream_shouldWork() {
        Option<List<Integer>> r = Option.sequence(Stream.of(
            Option.some(1),
            Option.some(2)
        ));

        assertEquals(Option.some(List.of(1, 2)), r);
    }

    @Test
    void traverse_iterable_shouldMapAndAccumulate() {
        Option<List<Integer>> r = Option.traverse(List.of("1", "2", "3"), s -> Option.some(Integer.parseInt(s)));
        assertEquals(Option.some(List.of(1, 2, 3)), r);
    }

    @Test
    void traverse_iterable_shouldShortCircuitToNone() {
        Option<List<Integer>> r = Option.traverse(List.of("1", "x", "3"), s -> {
            if ("x".equals(s)) return Option.none();
            return Option.some(Integer.parseInt(s));
        });
        assertEquals(Option.none(), r);
    }

    @Test
    void traverse_stream_shouldWork() {
        Option<List<Integer>> r = Option.traverse(Stream.of(1, 2, 3), i -> Option.some(i * 10));
        assertEquals(Option.some(List.of(10, 20, 30)), r);
    }

    @Test
    void traverse_stream_shouldShortCircuitToNone() {
        AtomicBoolean mapperCalledAfterNone = new AtomicBoolean(false);

        Option<List<Integer>> r = Option.traverse(Stream.of(1, 2, 3, 4), i -> {
            if (i == 2) return Option.none();
            if (i > 2) mapperCalledAfterNone.set(true);
            return Option.some(i);
        });

        assertEquals(Option.none(), r);
        // Nota: como implementamos traverse con iterator + loop, en cuanto ve None regresa.
        assertFalse(mapperCalledAfterNone.get());
    }

    // ---------- Laws (Functor + Monad) ----------

    @Test
    void functor_identityLaw() {
        Option<Integer> a = Option.some(42);
        Option<Integer> b = Option.<Integer>none();

        assertEquals(a, a.map(Function.identity()));
        assertEquals(b, b.map(Function.identity()));
    }

    @Test
    void functor_compositionLaw() {
        Function<Integer, Integer> f = x -> x + 1;
        Function<Integer, Integer> g = x -> x * 2;

        Option<Integer> m = Option.some(10);

        Option<Integer> left = m.map(f).map(g);
        Option<Integer> right = m.map(f.andThen(g));

        assertEquals(right, left);
    }

    @Test
    void monad_leftIdentity() {
        int a = 7;
        Function<Integer, Option<String>> f = x -> Option.some("v:" + x);

        Option<String> left = Option.some(a).flatMap(f);
        Option<String> right = f.apply(a);

        assertEquals(right, left);
    }

    @Test
    void monad_rightIdentity() {
        Option<Integer> m1 = Option.some(7);
        Option<Integer> m2 = Option.none();

        assertEquals(m1, m1.flatMap(Option::some));
        assertEquals(m2, m2.flatMap(Option::some));
    }

    @Test
    void monad_associativity() {
        Option<Integer> m = Option.some(3);

        Function<Integer, Option<Integer>> f = x -> Option.some(x + 1);
        Function<Integer, Option<Integer>> g = x -> Option.some(x * 2);

        Option<Integer> left = m.flatMap(f).flatMap(g);
        Option<Integer> right = m.flatMap(x -> f.apply(x).flatMap(g));

        assertEquals(right, left);
    }
}
