package dmx.fun.samples;

import dmx.fun.Accumulator;
import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Try;
import dmx.fun.Tuple2;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

/**
 * Demonstrates Accumulator<E, A>: a computed value paired with a side-channel accumulation
 * (log entries, metrics, audit trail). The functional alternative to mutable shared state
 * for cross-cutting concerns.
 */
public class AccumulatorSample {

    record Order(String id, double basePrice) {}
    record PricedOrder(String id, double price, String currency) {}

    static void main() {

        // ---- Basic usage ----

        System.out.println("=== Basic usage ===");

        Accumulator<List<String>, Integer> acc = Accumulator.of(42, List.of("computed answer"));
        System.out.println("value:       " + acc.value());       // 42
        System.out.println("accumulated: " + acc.accumulated()); // [computed answer]

        // ---- pure — value with empty accumulation ----

        System.out.println("\n=== pure (empty accumulation) ===");

        Accumulator<List<String>, String> pure = Accumulator.pure("hello", List.of());
        System.out.println("value:       " + pure.value());       // hello
        System.out.println("accumulated: " + pure.accumulated()); // []

        // ---- tell — record without producing a value ----

        System.out.println("\n=== tell ===");

        BinaryOperator<List<String>> concat = (a, b) -> {
            var merged = new ArrayList<>(a);
            merged.addAll(b);
            return merged;
        };

        Accumulator<List<String>, Integer> fromTell =
            Accumulator.tell(List.of("pre-check passed"))
                .flatMap(__ -> Accumulator.of(42, List.of("value computed")), concat);

        System.out.println("value:       " + fromTell.value());       // 42
        System.out.println("accumulated: " + fromTell.accumulated()); // [pre-check passed, value computed]

        // ---- map — transform value, accumulation unchanged ----

        System.out.println("\n=== map ===");

        Accumulator<List<String>, String> mapped =
            Accumulator.of(42, List.of("original")).map(Object::toString);
        System.out.println("value:       " + mapped.value());       // 42
        System.out.println("accumulated: " + mapped.accumulated()); // [original]

        // ---- flatMap — chain computations, merge accumulations ----

        System.out.println("\n=== flatMap (chain) ===");

        Accumulator<List<String>, Integer> chained =
            Accumulator.of(10, List.of("step 1"))
                .flatMap(v -> Accumulator.of(v * 2, List.of("step 2")), concat)
                .flatMap(v -> Accumulator.of(v + 5, List.of("step 3")), concat);

        System.out.println("value:       " + chained.value());        // 25
        System.out.println("accumulated: " + chained.accumulated());  // [step 1, step 2, step 3]

        // ---- flatMap — with NonEmptyList ----

        System.out.println("\n=== flatMap (NonEmptyList accumulation) ===");

        Accumulator<NonEmptyList<String>, Integer> nelChain =
            Accumulator.of(1, NonEmptyList.of("a", List.of()))
                .flatMap(v -> Accumulator.of(v + 1, NonEmptyList.of("b", List.of())), NonEmptyList::concat)
                .flatMap(v -> Accumulator.of(v + 1, NonEmptyList.of("c", List.of())), NonEmptyList::concat);

        System.out.println("value:       " + nelChain.value());                   // 3
        System.out.println("accumulated: " + nelChain.accumulated().toList());    // [a, b, c]

        // ---- mapAccumulated — transform the accumulation ----

        System.out.println("\n=== mapAccumulated ===");

        Accumulator<Integer, Integer> countOnly =
            Accumulator.of(42, List.of("a", "b", "c")).mapAccumulated(List::size);
        System.out.println("value:       " + countOnly.value());       // 42
        System.out.println("accumulated: " + countOnly.accumulated()); // 3

        // ---- toTuple2 — interop ----

        System.out.println("\n=== toTuple2 ===");

        Tuple2<List<String>, Integer> pair = chained.toTuple2();
        System.out.println("_1 (accumulated): " + pair._1()); // [step 1, step 2, step 3]
        System.out.println("_2 (value):       " + pair._2()); // 25

        // ---- hasValue ----

        System.out.println("\n=== hasValue ===");

        System.out.println("of.hasValue():   " + Accumulator.of(1, List.of()).hasValue());    // true
        System.out.println("tell.hasValue(): " + Accumulator.tell(List.of("x")).hasValue());  // false

        // ---- Real-world: order pricing with audit trail ----

        System.out.println("\n=== Real-world: order pricing audit ===");

        Accumulator<List<String>, PricedOrder> priced =
            Accumulator.of(new Order("ord-7", 100.0), List.of("order loaded"))
                .flatMap(o -> {
                    double discounted = o.basePrice() * 0.85;
                    return Accumulator.of(
                        new Order(o.id(), discounted),
                        List.of("15% discount applied → " + discounted)
                    );
                }, concat)
                .flatMap(o -> {
                    double taxed = o.basePrice() * 1.21;
                    return Accumulator.of(
                        new PricedOrder(o.id(), Math.round(taxed * 100.0) / 100.0, "USD"),
                        List.of("21% VAT applied → " + Math.round(taxed * 100.0) / 100.0)
                    );
                }, concat);

        System.out.println("Result:  " + priced.value());
        System.out.println("Audit trail:");
        priced.accumulated().forEach(entry -> System.out.println("  - " + entry));
        // Result:  PricedOrder[id=ord-7, price=102.85, currency=USD]
        // Audit trail:
        //   - order loaded
        //   - 15% discount applied → 85.0
        //   - 21% VAT applied → 102.85

        // ---- Real-world: counter accumulation ----

        // ---- combine — independent accumulators ----

        System.out.println("\n=== combine ===");

        Accumulator<List<String>, String>  userAcc  = Accumulator.of("Alice", List.of("user loaded"));
        Accumulator<List<String>, Integer> scoreAcc = Accumulator.of(98,      List.of("score loaded"));

        Accumulator<List<String>, String> dashboard = userAcc.combine(scoreAcc, concat,
            (name, score) -> name + " — score: " + score);

        System.out.println("value:       " + dashboard.value());       // Alice — score: 98
        System.out.println("accumulated: " + dashboard.accumulated()); // [user loaded, score loaded]

        // ---- sequence — fold a list of accumulators ----

        System.out.println("\n=== sequence ===");

        var steps = List.of(
            Accumulator.of(10, List.of("step A")),
            Accumulator.of(20, List.of("step B")),
            Accumulator.of(30, List.of("step C"))
        );

        Accumulator<List<String>, List<Integer>> seqResult =
            Accumulator.sequence(steps, concat, List.of());

        System.out.println("value:       " + seqResult.value());       // [10, 20, 30]
        System.out.println("accumulated: " + seqResult.accumulated()); // [step A, step B, step C]

        // ---- toOption ----

        System.out.println("\n=== toOption ===");

        System.out.println("of.toOption():   " + Accumulator.of(42, List.of()).toOption());    // Some(42)
        System.out.println("tell.toOption(): " + Accumulator.tell(List.of("x")).toOption());   // None

        // ---- liftOption — log presence/absence ----

        System.out.println("\n=== liftOption ===");

        Accumulator<List<String>, Option<String>> optFound = Accumulator.liftOption(
            Option.some("config.yaml"),
            path -> List.of("config found: " + path),
            List.of("config not found, using defaults")
        );
        System.out.println("value:       " + optFound.value());       // Some(config.yaml)
        System.out.println("accumulated: " + optFound.accumulated()); // [config found: config.yaml]

        Accumulator<List<String>, Option<String>> optMissing = Accumulator.liftOption(
            Option.none(),
            path -> List.of("config found: " + path),
            List.of("config not found, using defaults")
        );
        System.out.println("value:       " + optMissing.value());       // None
        System.out.println("accumulated: " + optMissing.accumulated()); // [config not found, using defaults]

        // ---- liftTry — log success/failure ----

        System.out.println("\n=== liftTry ===");

        Accumulator<List<String>, Try<Integer>> tryOk = Accumulator.liftTry(
            Try.of(() -> Integer.parseInt("42")),
            v  -> List.of("parsed: " + v),
            ex -> List.of("parse failed: " + ex.getMessage())
        );
        System.out.println("value:       " + tryOk.value());       // Success(42)
        System.out.println("accumulated: " + tryOk.accumulated()); // [parsed: 42]

        Accumulator<List<String>, Try<Integer>> tryFail = Accumulator.liftTry(
            Try.of(() -> Integer.parseInt("not-a-number")),
            v  -> List.of("parsed: " + v),
            ex -> List.of("parse failed: " + ex.getMessage())
        );
        System.out.println("value.isFailure: " + tryFail.value().isFailure());  // true
        System.out.println("accumulated:     " + tryFail.accumulated());        // [parse failed: ...]

        // ---- Real-world: step counter ----

        System.out.println("\n=== Real-world: step counter ===");

        BinaryOperator<Integer> add = Integer::sum;

        Accumulator<Integer, String> processed =
            Accumulator.pure("raw input", 0)
                .flatMap(s -> Accumulator.of(s.trim(), 1), add)
                .flatMap(s -> Accumulator.of(s.toUpperCase(), 1), add)
                .flatMap(s -> Accumulator.of("[" + s + "]", 1), add);

        System.out.println("value:  " + processed.value());       // [RAW INPUT]
        System.out.println("steps:  " + processed.accumulated()); // 3
    }
}
