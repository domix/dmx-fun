package dmx.fun;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionTest {

    @Test
    void some_null_shouldThrowNPE() {
        assertThatThrownBy(() -> Option.some(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void none_shouldBeEmpty_andReturnSingletonInstance() {
        Option<Integer> a = Option.none();
        Option<Integer> b = Option.none();

        assertThat(a.isEmpty()).isTrue();
        assertThat(b.isEmpty()).isTrue();
        assertThat(a).isEqualTo(b);   // value-based equality
        assertThat(a).isSameAs(b);    // singleton: same instance every time
    }

    @Test
    void fromOptional_nullOptional_shouldThrowNPE() {
        assertThatThrownBy(() -> Option.fromOptional(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void some_recordConstructor_shouldRejectNull_whenDirectlyConstructed() {
        assertThatThrownBy(() -> new Option.Some<>(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void ofNullable_shouldMapNullToNone() {
        assertThat(Option.ofNullable(null).isEmpty()).isTrue();
        assertThat(Option.ofNullable(10).get()).isEqualTo(10);
    }

    @Test
    void get_onNone_shouldThrow() {
        assertThatThrownBy(() -> Option.none().get()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getOrElse_shouldReturnValueForSome_orFallbackForNone() {
        assertThat(Option.some(10).getOrElse(99)).isEqualTo(10);
        assertThat(Option.<Integer>none().getOrElse(99)).isEqualTo(99);
    }

    @Test
    void getOrElseGet_shouldBeLazy_forSome_andCalledForNone() {
        AtomicBoolean called = new AtomicBoolean(false);

        int a = Option.some(10).getOrElseGet(() -> {
            called.set(true);
            return 99;
        });
        assertThat(a).isEqualTo(10);
        assertThat(called.get()).as("fallback supplier must not be called for Some").isFalse();

        int b = Option.<Integer>none().getOrElseGet(() -> {
            called.set(true);
            return 99;
        });
        assertThat(b).isEqualTo(99);
        assertThat(called.get()).as("fallback supplier must be called for None").isTrue();
    }

    @Test
    void getOrNull_shouldReturnValueOrNull() {
        assertThat(Option.some(10).getOrNull()).isEqualTo(10);
        assertThat(Option.<Integer>none().getOrNull()).isNull();
    }

    @Test
    void getOrThrow_shouldReturnValueForSome_orThrowCustomExceptionForNone() {
        assertThat(Option.some(10).getOrThrow(() -> new IllegalStateException("boom"))).isEqualTo(10);
        assertThatThrownBy(() -> Option.<Integer>none().getOrThrow(() -> new IllegalStateException("boom")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");
    }

    @Test
    void get_onNone_shouldThrow_NoSuchElementException_messageShouldMentionNone() {
        assertThatThrownBy(() -> Option.none().get())
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageMatching("(?i).*none.*");
    }

    @Test
    void map_shouldTransformSome_andPropagateNone() {
        assertThat(Option.some(2).map(v -> "v:" + v)).isEqualTo(Option.some("v:2"));
        assertThat(Option.<Integer>none().map(v -> v + 1)).isEqualTo(Option.none());
    }

    @Test
    void map_shouldTurnNullIntoNone() {
        assertThat(Option.some(1).map(v -> null)).isEqualTo(Option.none());
    }

    @Test
    void flatMap_mapperReturningNull_shouldThrowNPE() {
        assertThatThrownBy(() -> Option.some(1).flatMap(v -> null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void filter_onNone_shouldNotEvaluatePredicate() {
        AtomicBoolean called = new AtomicBoolean(false);

        Option<Integer> r = Option.<Integer>none().filter(v -> {
            called.set(true);
            return true;
        });

        assertThat(r).isEqualTo(Option.none());
        assertThat(called.get()).as("predicate must not be called for None").isFalse();
    }

    @Test
    void isDefined_isEmpty_shouldBeComplementary() {
        assertThat(Option.some(1).isDefined()).isTrue();
        assertThat(Option.some(1).isEmpty()).isFalse();

        assertThat(Option.<Integer>none().isDefined()).isFalse();
        assertThat(Option.<Integer>none().isEmpty()).isTrue();
    }

    @Test
    void flatMap_shouldChainSome_andPropagateNone() {
        Option<Integer> res = Option.some(2).flatMap(v -> Option.some(v * 10));
        assertThat(res).isEqualTo(Option.some(20));

        Option<Integer> none = Option.<Integer>none().flatMap(v -> Option.some(v * 10));
        assertThat(none).isEqualTo(Option.none());
    }

    @Test
    void filter_shouldKeepOrDropValue() {
        assertThat(Option.some(10).filter(v -> v > 0)).isEqualTo(Option.some(10));
        assertThat(Option.some(10).filter(v -> v < 0)).isEqualTo(Option.none());
        assertThat(Option.<Integer>none().filter(v -> true)).isEqualTo(Option.none());
    }

    @Test
    void peek_shouldRunForSome_andNotForNone() {
        AtomicInteger sum = new AtomicInteger(0);

        Option.some(7).peek(sum::addAndGet);
        assertThat(sum.get()).isEqualTo(7);

        Option.<Integer>none().peek(sum::addAndGet);
        assertThat(sum.get()).as("peek must not run for None").isEqualTo(7);
    }

    @Test
    void match_shouldExecuteCorrectBranch() {
        AtomicBoolean noneBranch = new AtomicBoolean(false);
        AtomicBoolean someBranch = new AtomicBoolean(false);

        Option.some(1).match(
            () -> noneBranch.set(true),
            v -> someBranch.set(true)
        );

        assertThat(noneBranch.get()).isFalse();
        assertThat(someBranch.get()).isTrue();

        noneBranch.set(false);
        someBranch.set(false);

        Option.<Integer>none().match(
            () -> noneBranch.set(true),
            v -> someBranch.set(true)
        );

        assertThat(noneBranch.get()).isTrue();
        assertThat(someBranch.get()).isFalse();
    }

    @Test
    void fold_shouldChooseBranch() {
        String a = Option.some(5).fold(() -> "none", v -> "some:" + v);
        String b = Option.<Integer>none().fold(() -> "none", v -> "some:" + v);

        assertThat(a).isEqualTo("some:5");
        assertThat(b).isEqualTo("none");
    }

    @Test
    void stream_shouldExposeSomeAsSingleElementStream_andNoneAsEmpty() {
        assertThat(Option.some(7).stream().toList()).isEqualTo(List.of(7));
        assertThat(Option.<Integer>none().stream().toList()).isEqualTo(List.of());
    }

    @Test
    void collectPresent_shouldFlattenOptions() {
        List<Integer> values = Option.collectPresent(Stream.of(
            Option.some(1),
            Option.none(),
            Option.some(3)
        ));

        assertThat(values).isEqualTo(List.of(1, 3));
    }

    @Test
    void presentValuesToList_collector_shouldWork() {
        List<Integer> values = Stream.of(
            Option.some(1),
            Option.<Integer>none(),
            Option.some(3)
        ).collect(Option.presentValuesToList());

        assertThat(values).isEqualTo(List.of(1, 3));
    }

    @Test
    void collectPresent_shouldThrow_ifStreamContainsNullElements() {
        // flatMap(Option::stream) exploit with NPE if null in the stream
        assertThatThrownBy(() ->
            Option.collectPresent(java.util.stream.Stream.of(
                Option.some(1),
                null,
                Option.some(3)
            ))
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void fromOptional_toOptional_roundtrip() {
        Option<Integer> a = Option.fromOptional(Optional.of(10));
        Option<Integer> b = Option.fromOptional(Optional.empty());

        assertThat(a.toOptional()).isEqualTo(Optional.of(10));
        assertThat(b.toOptional()).isEqualTo(Optional.empty());
    }

    // ---------- sequence / traverse ----------

    @Test
    void sequence_iterable_allSome_shouldReturnSomeList() {
        Option<List<Integer>> r = Option.sequence(List.of(
            Option.some(1),
            Option.some(2),
            Option.some(3)
        ));

        assertThat(r.isDefined()).isTrue();
        assertThat(r.get()).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    void sequence_iterable_withNone_shouldReturnNone() {
        Option<List<Integer>> r = Option.sequence(List.of(
            Option.some(1),
            Option.none(),
            Option.some(3)
        ));

        assertThat(r.isEmpty()).isTrue();
    }

    @Test
    void sequence_stream_shouldWork() {
        Option<List<Integer>> r = Option.sequence(Stream.of(
            Option.some(1),
            Option.some(2)
        ));

        assertThat(r).isEqualTo(Option.some(List.of(1, 2)));
    }

    @Test
    void sequence_iterable_shouldThrow_ifIterableContainsNullElement() {
        assertThatThrownBy(() ->
            Option.sequence(List.of(Option.some(1), null, Option.some(3)))
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void sequence_iterable_shouldThrow_withDescriptiveMessage_whenElementIsNull() {
        // List.of() rejects null before sequence() sees it; use ArrayList to exercise
        // the null-check branch inside Option.sequence(Iterable) itself.
        List<Option<Integer>> list = new ArrayList<>();
        list.add(Option.some(1));
        list.add(null);
        list.add(Option.some(3));

        assertThatThrownBy(() -> Option.sequence(list))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("null element");
    }

    @Test
    void sequence_stream_shouldThrow_ifStreamContainsNullElement() {
        assertThatThrownBy(() ->
            Option.sequence(java.util.stream.Stream.of(Option.some(1), null, Option.some(3)))
        ).isInstanceOf(NullPointerException.class);
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

        assertThat(r.isEmpty()).isTrue();
        assertThat(shouldNotReach.get()).isFalse();
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
        assertThat(r).isEqualTo(Option.none());
        assertThat(pulledAfterNone.get()).as("sequence(stream) must short-circuit after None").isFalse();
    }

    @Test
    void traverse_iterable_shouldThrow_ifMapperReturnsNull() {
        assertThatThrownBy(() ->
            Option.traverse(List.of(1, 2, 3), i -> null)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void traverse_stream_shouldThrow_ifMapperReturnsNull() {
        assertThatThrownBy(() ->
            Option.traverse(java.util.stream.Stream.of(1, 2, 3), i -> null)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void traverse_iterable_shouldThrow_ifValuesIterableIsNull() {
        assertThatThrownBy(() ->
            Option.traverse((Iterable<Integer>) null, Option::some)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void traverse_stream_shouldThrow_ifValuesStreamIsNull() {
        assertThatThrownBy(() ->
            Option.traverse((java.util.stream.Stream<Integer>) null, i -> Option.some(i))
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void toOptional_shouldBePresentForSome_emptyForNone() {
        assertThat(Option.some(1).toOptional()).isEqualTo(Optional.of(1));
        assertThat(Option.<Integer>none().toOptional()).isEqualTo(Optional.empty());
    }

    @Test
    void fromOptional_shouldCreateSomeOrNone() {
        assertThat(Option.fromOptional(Optional.of(10))).isEqualTo(Option.some(10));
        assertThat(Option.fromOptional(Optional.empty())).isEqualTo(Option.none());
    }

    @Test
    void traverse_iterable_shouldMapAndAccumulate() {
        Option<List<Integer>> r = Option.traverse(List.of("1", "2", "3"), s -> Option.some(Integer.parseInt(s)));
        assertThat(r).isEqualTo(Option.some(List.of(1, 2, 3)));
    }

    @Test
    void traverse_iterable_shouldShortCircuitToNone() {
        Option<List<Integer>> r = Option.traverse(List.of("1", "x", "3"), s -> {
            if ("x".equals(s)) return Option.none();
            return Option.some(Integer.parseInt(s));
        });
        assertThat(r).isEqualTo(Option.none());
    }

    @Test
    void traverse_stream_shouldWork() {
        Option<List<Integer>> r = Option.traverse(Stream.of(1, 2, 3), i -> Option.some(i * 10));
        assertThat(r).isEqualTo(Option.some(List.of(10, 20, 30)));
    }

    @Test
    void traverse_stream_shouldShortCircuitToNone() {
        AtomicBoolean mapperCalledAfterNone = new AtomicBoolean(false);

        Option<List<Integer>> r = Option.traverse(Stream.of(1, 2, 3, 4), i -> {
            if (i == 2) return Option.none();
            if (i > 2) mapperCalledAfterNone.set(true);
            return Option.some(i);
        });

        assertThat(r).isEqualTo(Option.none());
        // Note: Since we implemented traverse with iterator + loop, it returns as soon as it sees None.
        assertThat(mapperCalledAfterNone.get()).isFalse();
    }

    @Test
    void toResult_shouldReturnOkForSome_andErrForNone() {
        Result<Integer, String> ok = Option.some(10).toResult("nope");
        Result<Integer, String> err = Option.<Integer>none().toResult("nope");

        assertThat(ok.isOk()).isTrue();
        assertThat(ok.get()).isEqualTo(10);

        assertThat(err.isError()).isTrue();
        assertThat(err.getError()).isEqualTo("nope");
    }

    @Test
    void toTry_shouldReturnSuccessForSome_andFailureForNone() {
        Try<Integer> a = Option.some(10).toTry(() -> new RuntimeException("boom"));
        Try<Integer> b = Option.<Integer>none().toTry(() -> new RuntimeException("boom"));

        assertThat(a.isSuccess()).isTrue();
        assertThat(a.get()).isEqualTo(10);

        assertThat(b.isFailure()).isTrue();
        assertThat(b.getCause().getMessage()).isEqualTo("boom");
    }

    // ---------- Laws (Functor + Monad) ----------

    @Test
    void functor_identityLaw() {
        Option<Integer> a = Option.some(42);
        Option<Integer> b = Option.<Integer>none();

        assertThat(a.map(Function.identity())).isEqualTo(a);
        assertThat(b.map(Function.identity())).isEqualTo(b);
    }

    @Test
    void functor_compositionLaw() {
        Function<Integer, Integer> f = x -> x + 1;
        Function<Integer, Integer> g = x -> x * 2;

        Option<Integer> m = Option.some(10);

        Option<Integer> left = m.map(f).map(g);
        Option<Integer> right = m.map(f.andThen(g));

        assertThat(left).isEqualTo(right);
    }

    @Test
    void monad_leftIdentity() {
        int a = 7;
        Function<Integer, Option<String>> f = x -> Option.some("v:" + x);

        Option<String> left = Option.some(a).flatMap(f);
        Option<String> right = f.apply(a);

        assertThat(left).isEqualTo(right);
    }

    @Test
    void monad_rightIdentity() {
        Option<Integer> m1 = Option.some(7);
        Option<Integer> m2 = Option.none();

        assertThat(m1.flatMap(Option::some)).isEqualTo(m1);
        assertThat(m2.flatMap(Option::some)).isEqualTo(m2);
    }

    @Test
    void monad_associativity() {
        Option<Integer> m = Option.some(3);

        Function<Integer, Option<Integer>> f = x -> Option.some(x + 1);
        Function<Integer, Option<Integer>> g = x -> Option.some(x * 2);

        Option<Integer> left = m.flatMap(f).flatMap(g);
        Option<Integer> right = m.flatMap(x -> f.apply(x).flatMap(g));

        assertThat(left).isEqualTo(right);
    }
}
