package dmx.fun;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccumulatorTest {

    // Merge helper used throughout
    static final BinaryOperator<List<String>> CONCAT = (a, b) -> {
        var merged = new ArrayList<>(a);
        merged.addAll(b);
        return merged;
    };

    // -------------------------------------------------------------------------
    // of — factory
    // -------------------------------------------------------------------------

    @Test
    void of_storesValueAndAccumulated() {
        var acc = Accumulator.of(42, List.of("step 1"));
        assertThat(acc.value()).isEqualTo(42);
        assertThat(acc.accumulated()).containsExactly("step 1");
    }

    @Test
    void of_shouldThrowNPE_whenValueIsNull() {
        assertThatThrownBy(() -> Accumulator.of(null, List.of()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("value");
    }

    @Test
    void of_shouldThrowNPE_whenAccumulatedIsNull() {
        assertThatThrownBy(() -> Accumulator.of(42, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("accumulated");
    }

    // -------------------------------------------------------------------------
    // pure — factory
    // -------------------------------------------------------------------------

    @Test
    void pure_storesValueWithEmptyAccumulation() {
        var acc = Accumulator.pure("hello", List.of());
        assertThat(acc.value()).isEqualTo("hello");
        assertThat(acc.accumulated()).isEmpty();
    }

    @Test
    void pure_shouldThrowNPE_whenValueIsNull() {
        assertThatThrownBy(() -> Accumulator.pure(null, List.of()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("value");
    }

    // -------------------------------------------------------------------------
    // tell — factory
    // -------------------------------------------------------------------------

    @Test
    void tell_accumulationIsSet_valueIsNull() {
        var acc = Accumulator.tell(List.of("audit entry"));
        assertThat(acc.value()).isNull();
        assertThat(acc.accumulated()).containsExactly("audit entry");
    }

    @Test
    void tell_hasValue_returnsFalse() {
        var acc = Accumulator.tell(List.of("entry"));
        assertThat(acc.hasValue()).isFalse();
    }

    @Test
    void tell_shouldThrowNPE_whenAccumulatedIsNull() {
        assertThatThrownBy(() -> Accumulator.tell(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("accumulated");
    }

    // -------------------------------------------------------------------------
    // hasValue
    // -------------------------------------------------------------------------

    @Test
    void hasValue_returnsTrueForOf() {
        assertThat(Accumulator.of(1, List.of()).hasValue()).isTrue();
    }

    @Test
    void hasValue_returnsTrueForPure() {
        assertThat(Accumulator.pure("x", List.of()).hasValue()).isTrue();
    }

    @Test
    void hasValue_returnsFalseForTell() {
        assertThat(Accumulator.tell(List.of("e")).hasValue()).isFalse();
    }

    // -------------------------------------------------------------------------
    // map
    // -------------------------------------------------------------------------

    @Test
    void map_transformsValue_accumulationUnchanged() {
        var result = Accumulator.of(42, List.of("step 1")).map(Object::toString);
        assertThat(result.value()).isEqualTo("42");
        assertThat(result.accumulated()).containsExactly("step 1");
    }

    @Test
    void map_shouldThrowNPE_whenFunctionIsNull() {
        assertThatThrownBy(() -> Accumulator.of(1, List.of()).map(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void map_onTell_shouldThrowNPE() {
        var tell = Accumulator.tell(List.of("e"));
        assertThatThrownBy(() -> tell.map(v -> v + "x"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void map_chain_accumulationCarriedThrough() {
        var result = Accumulator.of(5, List.of("a"))
            .map(v -> v * 2)
            .map(v -> v + 1);
        assertThat(result.value()).isEqualTo(11);
        assertThat(result.accumulated()).containsExactly("a");
    }

    // -------------------------------------------------------------------------
    // flatMap
    // -------------------------------------------------------------------------

    @Test
    void flatMap_chainsValueAndMergesAccumulation() {
        var result = Accumulator.of(10, List.of("step 1"))
            .flatMap(v -> Accumulator.of(v * 2, List.of("step 2")), CONCAT);
        assertThat(result.value()).isEqualTo(20);
        assertThat(result.accumulated()).containsExactly("step 1", "step 2");
    }

    @Test
    void flatMap_multipleSteps_accumulatesAll() {
        var result = Accumulator.of(10, List.of("step 1"))
            .flatMap(v -> Accumulator.of(v * 2, List.of("step 2")), CONCAT)
            .flatMap(v -> Accumulator.of(v + 5, List.of("step 3")), CONCAT);
        assertThat(result.value()).isEqualTo(25);
        assertThat(result.accumulated()).containsExactly("step 1", "step 2", "step 3");
    }

    @Test
    void flatMap_withNonEmptyList_mergesCorrectly() {
        var result = Accumulator.of(1, NonEmptyList.of("a", List.of()))
            .flatMap(v -> Accumulator.of(v + 1, NonEmptyList.of("b", List.of())), NonEmptyList::concat);
        assertThat(result.value()).isEqualTo(2);
        assertThat(result.accumulated().toList()).containsExactly("a", "b");
    }

    @Test
    void flatMap_afterTell_producesValueAndMergedLog() {
        var result = Accumulator.tell(List.of("pre-check"))
            .flatMap(__ -> Accumulator.of(42, List.of("computed")), CONCAT);
        assertThat(result.value()).isEqualTo(42);
        assertThat(result.accumulated()).containsExactly("pre-check", "computed");
    }

    @Test
    void flatMap_shouldThrowNPE_whenFunctionIsNull() {
        var acc = Accumulator.<List<String>, Integer>of(1, List.of());
        assertThatThrownBy(() -> acc.flatMap(null, CONCAT))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void flatMap_shouldThrowNPE_whenMergeIsNull() {
        var acc = Accumulator.<List<String>, Integer>of(1, List.of());
        assertThatThrownBy(() -> acc.flatMap(v -> Accumulator.of(v, List.of()), null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void flatMap_shouldThrowNPE_whenFunctionReturnsNull() {
        var acc = Accumulator.<List<String>, Integer>of(1, List.of());
        assertThatThrownBy(() -> acc.flatMap(v -> null, CONCAT))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // mapAccumulated
    // -------------------------------------------------------------------------

    @Test
    void mapAccumulated_transformsAccumulation_valueUnchanged() {
        var result = Accumulator.of(42, List.of("a", "b", "c")).mapAccumulated(List::size);
        assertThat(result.value()).isEqualTo(42);
        assertThat(result.accumulated()).isEqualTo(3);
    }

    @Test
    void mapAccumulated_shouldThrowNPE_whenFunctionIsNull() {
        assertThatThrownBy(() -> Accumulator.of(1, List.of()).mapAccumulated(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void mapAccumulated_shouldThrowNPE_whenFunctionReturnsNull() {
        assertThatThrownBy(() -> Accumulator.of(1, List.of("a")).mapAccumulated(l -> null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // toTuple2
    // -------------------------------------------------------------------------

    @Test
    void toTuple2_firstIsAccumulated_secondIsValue() {
        var acc  = Accumulator.of(42, List.of("step 1"));
        var pair = acc.toTuple2();
        assertThat(pair._1()).containsExactly("step 1");
        assertThat(pair._2()).isEqualTo(42);
    }

    @Test
    void toTuple2_onTell_valueIsNull() {
        var acc  = Accumulator.tell(List.of("entry"));
        var pair = acc.toTuple2();
        assertThat(pair._1()).containsExactly("entry");
        assertThat(pair._2()).isNull();
    }

    // -------------------------------------------------------------------------
    // record: equals, hashCode, toString
    // -------------------------------------------------------------------------

    @Test
    void equals_sameValueAndAccumulation() {
        var a = Accumulator.of(42, List.of("x"));
        var b = Accumulator.of(42, List.of("x"));
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_differentValue_notEqual() {
        var a = Accumulator.of(42, List.of("x"));
        var b = Accumulator.of(99, List.of("x"));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_sameForEqualAccumulators() {
        var a = Accumulator.of("hello", List.of("log"));
        var b = Accumulator.of("hello", List.of("log"));
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toString_containsValueAndAccumulated() {
        var acc = Accumulator.of(42, List.of("step 1")).toString();
        assertThat(acc).contains("42").contains("step 1");
    }

    // -------------------------------------------------------------------------
    // Integration — real-world audit chain
    // -------------------------------------------------------------------------

    @Test
    void integration_orderProcessingChain_accumulatesAuditLog() {
        record Order(String id, double total) {}

        var result = Accumulator.of(new Order("ord-1", 95.00), List.of("order loaded"))
            .flatMap(o -> {
                double discounted = o.total() * 0.9;
                return Accumulator.of(new Order(o.id(), discounted), List.of("10% discount applied"));
            }, CONCAT)
            .flatMap(o -> {
                double taxed = o.total() * 1.07;
                return Accumulator.of(new Order(o.id(), Math.round(taxed * 100.0) / 100.0), List.of("tax applied"));
            }, CONCAT)
            .map(o -> "Order " + o.id() + " total: " + o.total());

        assertThat(result.value()).isEqualTo("Order ord-1 total: 91.49");
        assertThat(result.accumulated())
            .containsExactly("order loaded", "10% discount applied", "tax applied");
    }

    @Test
    void integration_counterAccumulation() {
        BinaryOperator<Integer> add = Integer::sum;

        var result = Accumulator.of("alice", 0)
            .flatMap(name -> Accumulator.of(name.toUpperCase(), 1), add)
            .flatMap(name -> Accumulator.of(name + "!", 1), add);

        assertThat(result.value()).isEqualTo("ALICE!");
        assertThat(result.accumulated()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // combine
    // -------------------------------------------------------------------------

    @Test
    void combine_mergesValuesAndAccumulations() {
        var a = Accumulator.of("Alice", List.of("user loaded"));
        var b = Accumulator.of(42,     List.of("score loaded"));

        var result = a.combine(b, CONCAT, (name, score) -> name + ":" + score);

        assertThat(result.value()).isEqualTo("Alice:42");
        assertThat(result.accumulated()).containsExactly("user loaded", "score loaded");
    }

    @Test
    void combine_shouldThrowNPE_whenOtherIsNull() {
        var acc = Accumulator.<List<String>, Integer>of(1, List.of());
        assertThatThrownBy(() -> acc.combine(null, CONCAT, Integer::sum))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void combine_shouldThrowNPE_whenMergeIsNull() {
        var a = Accumulator.<List<String>, Integer>of(1, List.of());
        var b = Accumulator.<List<String>, Integer>of(2, List.of());
        assertThatThrownBy(() -> a.combine(b, null, Integer::sum))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void combine_shouldThrowNPE_whenFunctionIsNull() {
        var a = Accumulator.<List<String>, Integer>of(1, List.of());
        var b = Accumulator.<List<String>, Integer>of(2, List.of());
        assertThatThrownBy(() -> a.<Integer, Integer>combine(b, CONCAT, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void combine_shouldThrowNPE_whenThisIsTell() {
        var tell = Accumulator.tell(List.of("entry"));
        var other = Accumulator.<List<String>, Integer>of(42, List.of());
        assertThatThrownBy(() -> tell.combine(other, CONCAT, (a, b) -> b))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void combine_shouldThrowNPE_whenOtherIsTell() {
        var acc = Accumulator.<List<String>, Integer>of(42, List.of());
        var tell = Accumulator.tell(List.of("entry"));
        assertThatThrownBy(() -> acc.combine(tell, CONCAT, (a, b) -> a))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // sequence
    // -------------------------------------------------------------------------

    @Test
    void sequence_foldsListIntoSingleAccumulator() {
        var steps = List.of(
            Accumulator.of(1, List.of("step A")),
            Accumulator.of(2, List.of("step B")),
            Accumulator.of(3, List.of("step C"))
        );

        var result = Accumulator.sequence(steps, CONCAT, List.of());

        assertThat(result.value()).containsExactly(1, 2, 3);
        assertThat(result.accumulated()).containsExactly("step A", "step B", "step C");
    }

    @Test
    void sequence_emptyList_returnsEmptyValueAndEmptyAccumulation() {
        var result = Accumulator.<List<String>, Integer>sequence(List.of(), CONCAT, List.of());
        assertThat(result.value()).isEmpty();
        assertThat(result.accumulated()).isEmpty();
    }

    @Test
    void sequence_shouldThrowNPE_whenListIsNull() {
        assertThatThrownBy(() -> Accumulator.<List<String>, Integer>sequence(null, CONCAT, List.of()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void sequence_shouldThrowNPE_whenElementIsTell() {
        // Use the canonical record constructor directly to put a null-value element in the list
        // (mirrors the tell() case without the Void type mismatch)
        var noValue = new Accumulator<List<String>, Integer>(null, List.of("tell — no value"));
        var steps = List.of(
            Accumulator.of(1, List.of("ok")),
            noValue
        );
        assertThatThrownBy(() -> Accumulator.sequence(steps, CONCAT, List.of()))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // toOption
    // -------------------------------------------------------------------------

    @Test
    void toOption_returnsSome_forOfAccumulator() {
        var acc = Accumulator.of(42, List.of("log"));
        assertThat(acc.toOption().isDefined()).isTrue();
        assertThat(acc.toOption().get()).isEqualTo(42);
    }

    @Test
    void toOption_returnsNone_forTellAccumulator() {
        var acc = Accumulator.tell(List.of("log"));
        assertThat(acc.toOption().isEmpty()).isTrue();
    }

    @Test
    void toOption_discardsAccumulation() {
        var acc = Accumulator.of("value", List.of("important log"));
        var opt = acc.toOption();
        assertThat(opt.get()).isEqualTo("value");
        // accumulated is not accessible through Option — that's by design
    }

    // -------------------------------------------------------------------------
    // toResult
    // -------------------------------------------------------------------------

    @Test
    void toResult_ofAccumulator_returnsOk() {
        var acc = Accumulator.of(42, List.of("step 1"));
        var result = acc.toResult();
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo(42);
    }

    @Test
    void toResult_tellAccumulator_returnsErr() {
        var acc = Accumulator.tell(List.of("entry"));
        var result = acc.toResult();
        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).containsExactly("entry");
    }

    // -------------------------------------------------------------------------
    // liftOption
    // -------------------------------------------------------------------------

    @Test
    void liftOption_some_appliesSomeLog() {
        var result = Accumulator.liftOption(
            Option.some("alice"),
            name -> List.of("found: " + name),
            List.of("not found")
        );
        assertThat(result.value()).isEqualTo(Option.some("alice"));
        assertThat(result.accumulated()).containsExactly("found: alice");
    }

    @Test
    void liftOption_none_usesNoneLog() {
        var result = Accumulator.liftOption(
            Option.<String>none(),
            name -> List.of("found: " + name),
            List.of("not found")
        );
        assertThat(result.value().isEmpty()).isTrue();
        assertThat(result.accumulated()).containsExactly("not found");
    }

    @Test
    void liftOption_shouldThrowNPE_whenOptionIsNull() {
        assertThatThrownBy(() -> Accumulator.liftOption(null, v -> List.of(), List.of()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void liftOption_shouldThrowNPE_whenSomeLogReturnsNull() {
        assertThatThrownBy(() -> Accumulator.liftOption(Option.some("x"), v -> null, List.of()))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // liftTry
    // -------------------------------------------------------------------------

    @Test
    void liftTry_success_appliesSuccessLog() {
        var result = Accumulator.liftTry(
            Try.success(42),
            v  -> List.of("loaded: " + v),
            ex -> List.of("failed: " + ex.getMessage())
        );
        assertThat(result.value().isSuccess()).isTrue();
        assertThat(result.value().get()).isEqualTo(42);
        assertThat(result.accumulated()).containsExactly("loaded: 42");
    }

    @Test
    void liftTry_failure_appliesFailureLog() {
        var ex = new RuntimeException("boom");
        var result = Accumulator.liftTry(
            Try.failure(ex),
            v  -> List.of("loaded: " + v),
            e  -> List.of("failed: " + e.getMessage())
        );
        assertThat(result.value().isFailure()).isTrue();
        assertThat(result.accumulated()).containsExactly("failed: boom");
    }

    @Test
    void liftTry_shouldThrowNPE_whenTryIsNull() {
        assertThatThrownBy(() -> Accumulator.liftTry(null, v -> List.of(), ex -> List.of()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void liftTry_shouldThrowNPE_whenSuccessLogReturnsNull() {
        assertThatThrownBy(() -> Accumulator.liftTry(Try.success(1), v -> null, ex -> List.of()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void liftTry_shouldThrowNPE_whenFailureLogReturnsNull() {
        assertThatThrownBy(() ->
            Accumulator.liftTry(Try.failure(new RuntimeException()), v -> List.of(), ex -> null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // Integration — combine + sequence
    // -------------------------------------------------------------------------

    @Test
    void integration_combineThreeAccumulators_viaPairwise() {
        var a = Accumulator.of("Alice", List.of("user"));
        var b = Accumulator.of(42,     List.of("score"));
        var c = Accumulator.of(true,   List.of("active"));

        var result = a
            .combine(b, CONCAT, (name, score) -> name + "/" + score)
            .combine(c, CONCAT, (nameScore, active) -> nameScore + "/" + active);

        assertThat(result.value()).isEqualTo("Alice/42/true");
        assertThat(result.accumulated()).containsExactly("user", "score", "active");
    }

    @Test
    void integration_sequenceThenMap() {
        var steps = List.of(
            Accumulator.of(10, List.of("a")),
            Accumulator.of(20, List.of("b")),
            Accumulator.of(30, List.of("c"))
        );

        var result = Accumulator.sequence(steps, CONCAT, List.of())
            .map(values -> values.stream().mapToInt(Integer::intValue).sum());

        assertThat(result.value()).isEqualTo(60);
        assertThat(result.accumulated()).containsExactly("a", "b", "c");
    }

    @Test
    void integration_liftOptionInChain() {
        var concat = CONCAT;

        var result = Accumulator.liftOption(
                Option.some("config.yaml"),
                path -> List.of("config path resolved: " + path),
                List.of("config not found, using defaults")
            )
            .flatMap(opt -> opt.isDefined()
                ? Accumulator.of(opt.get().toUpperCase(), List.of("path normalized"))
                : Accumulator.of("DEFAULT", List.of("using default")),
                concat);

        assertThat(result.value()).isEqualTo("CONFIG.YAML");
        assertThat(result.accumulated())
            .containsExactly("config path resolved: config.yaml", "path normalized");
    }
}
