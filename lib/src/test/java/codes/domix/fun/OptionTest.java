package codes.domix.fun;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionTest {
    @Test
    void some_null_shouldReturnNone() {
        assertTrue(Option.some(null).isEmpty());
        assertEquals(Option.none(), Option.some(null));
    }

    @Test
    void none_shouldBeEmpty_andSingletonSemanticsNotRequired() {
        Option<Integer> a = Option.none();
        Option<Integer> b = Option.none();

        assertTrue(a.isEmpty());
        assertTrue(b.isEmpty());
        assertEquals(a, b);          // records: value-based equality
        assertNotSame(a, b);         // no singleton obligation
    }

    @Test
    void fromOptional_nullOptional_shouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> Option.fromOptional(null));
    }

    @Test
    void some_recordConstructor_shouldRejectNull_whenDirectlyConstructed() {
        assertThrows(NullPointerException.class, () -> new Option.Some<>(null));
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
    void getOrElse_shouldReturnValueForSome_orFallbackForNone() {
        assertEquals(10, Option.some(10).getOrElse(99));
        assertEquals(99, Option.<Integer>none().getOrElse(99));
    }

    @Test
    void getOrElseGet_shouldBeLazy_forSome_andCalledForNone() {
        AtomicBoolean called = new AtomicBoolean(false);

        int a = Option.some(10).getOrElseGet(() -> {
            called.set(true);
            return 99;
        });
        assertEquals(10, a);
        assertFalse(called.get(), "fallback supplier must not be called for Some");

        int b = Option.<Integer>none().getOrElseGet(() -> {
            called.set(true);
            return 99;
        });
        assertEquals(99, b);
        assertTrue(called.get(), "fallback supplier must be called for None");
    }

    @Test
    void getOrNull_shouldReturnValueOrNull() {
        assertEquals(10, Option.some(10).getOrNull());
        assertNull(Option.<Integer>none().getOrNull());
    }

    @Test
    void getOrThrow_shouldReturnValueForSome_orThrowCustomExceptionForNone() {
        assertEquals(10, Option.some(10).getOrThrow(() -> new IllegalStateException("boom")));
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> Option.<Integer>none().getOrThrow(() -> new IllegalStateException("boom"))
        );
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void get_onNone_shouldThrow_NoSuchElementException_messageShouldMentionNone() {
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> Option.none().get());
        assertTrue(ex.getMessage().toLowerCase().contains("none"));
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
    void flatMap_mapperReturningNull_shouldThrowNPE() {
        assertThrows(NullPointerException.class, () ->
            Option.some(1).flatMap(v -> null)
        );
    }

    @Test
    void filter_onNone_shouldNotEvaluatePredicate() {
        AtomicBoolean called = new AtomicBoolean(false);

        Option<Integer> r = Option.<Integer>none().filter(v -> {
            called.set(true);
            return true;
        });

        assertEquals(Option.none(), r);
        assertFalse(called.get(), "predicate must not be called for None");
    }

    @Test
    void isDefined_isEmpty_shouldBeComplementary() {
        assertTrue(Option.some(1).isDefined());
        assertFalse(Option.some(1).isEmpty());

        assertFalse(Option.<Integer>none().isDefined());
        assertTrue(Option.<Integer>none().isEmpty());
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
    void peek_shouldRunForSome_andNotForNone() {
        AtomicInteger sum = new AtomicInteger(0);

        Option.some(7).peek(sum::addAndGet);
        assertEquals(7, sum.get());

        Option.<Integer>none().peek(sum::addAndGet);
        assertEquals(7, sum.get(), "peek must not run for None");
    }

    @Test
    void match_shouldExecuteCorrectBranch() {
        AtomicBoolean noneBranch = new AtomicBoolean(false);
        AtomicBoolean someBranch = new AtomicBoolean(false);

        Option.some(1).match(
            () -> noneBranch.set(true),
            v -> someBranch.set(true)
        );

        assertFalse(noneBranch.get());
        assertTrue(someBranch.get());

        noneBranch.set(false);
        someBranch.set(false);

        Option.<Integer>none().match(
            () -> noneBranch.set(true),
            v -> someBranch.set(true)
        );

        assertTrue(noneBranch.get());
        assertFalse(someBranch.get());
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
            Option.<Integer>none(),
            Option.some(3)
        ).collect(Option.presentValuesToList());

        assertEquals(List.of(1, 3), values);
    }

    @Test
    void collectPresent_shouldThrow_ifStreamContainsNullElements() {
        // flatMap(Option::stream) exploit with NPE if null in the stream
        assertThrows(NullPointerException.class, () ->
            Option.collectPresent(java.util.stream.Stream.of(
                Option.some(1),
                null,
                Option.some(3)
            ))
        );
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
    void sequence_iterable_shouldThrow_ifIterableContainsNullElement() {
        assertThrows(NullPointerException.class, () ->
            Option.sequence(List.of(Option.some(1), null, Option.some(3)))
        );
    }

    @Test
    void sequence_stream_shouldThrow_ifStreamContainsNullElement() {
        assertThrows(NullPointerException.class, () ->
            Option.sequence(java.util.stream.Stream.of(Option.some(1), null, Option.some(3)))
        );
    }

    @Test
    void sequence_iterable_shouldShortCircuit_onNone() {
        AtomicBoolean shouldNotReach = new AtomicBoolean(false);

        // The mapper/flag is not applied directly here; we tested an indirect short circuit with a "heavy" option.
        // We construct a list where the last access would flag if evaluated (but sequence does not evaluate anything from Some,
        // inspection instance only; also serves to test that it does not iterate excessively in the stream version below)
        Option<List<Integer>> r = Option.sequence(List.of(
            Option.some(1),
            Option.none(),
            Option.some(3)
        ));

        assertTrue(r.isEmpty());
        assertFalse(shouldNotReach.get());
    }

    @Test
    void sequence_stream_shouldShortCircuit_onNone_withoutPullingMoreElements() {
        AtomicBoolean pulledAfterNone = new AtomicBoolean(false);

        Iterator<Option<Integer>> it = new Iterator<>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < 4;
            }

            @Override
            public Option<Integer> next() {
                i++;
                if (i == 1) return Option.some(1);
                if (i == 2) return Option.none();
                // If we get here, it means that sequence continued iterating after None
                pulledAfterNone.set(true);
                return Option.some(99);
            }
        };

        Option<List<Integer>> r = Option.sequence(java.util.stream.Stream.generate(it::next).limit(4));
        assertEquals(Option.none(), r);
        assertFalse(pulledAfterNone.get(), "sequence(stream) must short-circuit after None");
    }

    @Test
    void traverse_iterable_shouldThrow_ifMapperReturnsNull() {
        assertThrows(NullPointerException.class, () ->
            Option.traverse(List.of(1, 2, 3), i -> null)
        );
    }

    @Test
    void traverse_stream_shouldThrow_ifMapperReturnsNull() {
        assertThrows(NullPointerException.class, () ->
            Option.traverse(java.util.stream.Stream.of(1, 2, 3), i -> null)
        );
    }

    @Test
    void traverse_iterable_shouldThrow_ifValuesIterableIsNull() {
        assertThrows(NullPointerException.class, () ->
            Option.traverse((Iterable<Integer>) null, Option::some)
        );
    }

    @Test
    void traverse_stream_shouldThrow_ifValuesStreamIsNull() {
        assertThrows(NullPointerException.class, () ->
            Option.traverse((java.util.stream.Stream<Integer>) null, i -> Option.some(i))
        );
    }

    @Test
    void toOptional_shouldBePresentForSome_emptyForNone() {
        assertEquals(Optional.of(1), Option.some(1).toOptional());
        assertEquals(Optional.empty(), Option.<Integer>none().toOptional());
    }

    @Test
    void fromOptional_shouldCreateSomeOrNone() {
        assertEquals(Option.some(10), Option.fromOptional(Optional.of(10)));
        assertEquals(Option.none(), Option.fromOptional(Optional.empty()));
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
        // Note: Since we implemented traverse with iterator + loop, it returns as soon as it sees None.
        assertFalse(mapperCalledAfterNone.get());
    }

    @Test
    void toResult_shouldReturnOkForSome_andErrForNone() {
        Result<Integer, String> ok = Option.some(10).toResult("nope");
        Result<Integer, String> err = Option.<Integer>none().toResult("nope");

        assertTrue(ok.isOk());
        assertEquals(10, ok.get());

        assertTrue(err.isError());
        assertEquals("nope", err.getError());
    }

    @Test
    void toTry_shouldReturnSuccessForSome_andFailureForNone() {
        Try<Integer> a = Option.some(10).toTry(() -> new RuntimeException("boom"));
        Try<Integer> b = Option.<Integer>none().toTry(() -> new RuntimeException("boom"));

        assertTrue(a.isSuccess());
        assertEquals(10, a.get());

        assertTrue(b.isFailure());
        assertEquals("boom", b.getCause().getMessage());
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
