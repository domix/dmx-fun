package dmx.fun.samples;

import dmx.fun.Accumulator;
import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Try;
import dmx.fun.Tuple2;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;

/**
 * Demonstrates Accumulator<E, A>: a computed value paired with a side-channel accumulation
 * (log entries, metrics, audit trail). The functional alternative to mutable shared state
 * for cross-cutting concerns.
 */
public class AccumulatorSample {

    record Order(String id, BigDecimal basePrice) {
    }

    record PricedOrder(String id, BigDecimal price, String currency) {
    }

    static void main() {

        // ---- Basic usage ----

        IO.println("=== Basic usage ===");

        var acc = Accumulator.of(42, List.of("computed answer"));
        IO.println("value       : %d".formatted(acc.value()));       // 42
        IO.println("accumulated : %s".formatted(acc.accumulated())); // [computed answer]

        // ---- pure — value with empty accumulation ----

        IO.println("\n=== pure (empty accumulation) ===");

        var pure = Accumulator.pure("hello", List.of());
        IO.println("value:       %s".formatted(pure.value()));       // hello
        IO.println("accumulated: %s".formatted(pure.accumulated())); // []

        // ---- tell — record without producing a value ----

        IO.println("\n=== tell ===");

        BinaryOperator<List<String>> concat = (a, b) -> {
            var merged = new ArrayList<>(a);
            merged.addAll(b);
            return merged;
        };

        var fromTell =
            Accumulator.tell(List.of("pre-check passed"))
                .flatMap(_ -> Accumulator.of(42, List.of("value computed")), concat);

        IO.println("value:       %d".formatted(fromTell.value()));       // 42
        IO.println("accumulated: %s".formatted(fromTell.accumulated())); // [pre-check passed, value computed]

        // ---- map — transform value, accumulation unchanged ----

        IO.println("\n=== map ===");

        var mapped = Accumulator.of(42, List.of("original"))
            .map(Object::toString);
        IO.println("value:       %s".formatted(mapped.value()));       // 42
        IO.println("Value type:  %s".formatted(Objects.requireNonNull(mapped.value()).getClass().getSimpleName()));
        IO.println("accumulated: %s".formatted(mapped.accumulated())); // [original]

        // ---- flatMap — chain computations, merge accumulations ----

        IO.println("\n=== flatMap (chain) ===");

        var chained =
            Accumulator.of(10, List.of("step 1"))
                .flatMap(v -> Accumulator.of(v * 2, List.of("step 2")), concat)
                .flatMap(v -> Accumulator.of(v + 5, List.of("step 3")), concat);

        IO.println("value:       %s".formatted(chained.value()));        // 25
        IO.println("accumulated: %s".formatted(chained.accumulated()));  // [step 1, step 2, step 3]

        // ---- flatMap — with NonEmptyList ----

        IO.println("\n=== flatMap (NonEmptyList accumulation) ===");

        var nelChain = Accumulator.of(1, NonEmptyList.singleton("a"))
            .flatMap(
                value -> Accumulator.of(value + 1, NonEmptyList.singleton("b")),
                NonEmptyList::concat
            )
            .flatMap(
                value -> Accumulator.of(value + 1, NonEmptyList.singleton("c")),
                NonEmptyList::concat
            );

        IO.println("value:       %s".formatted(nelChain.value()));                   // 3
        IO.println("accumulated: %s".formatted(nelChain.accumulated().toList()));    // [a, b, c]

        // ---- mapAccumulated — transform the accumulation ----

        IO.println("\n=== mapAccumulated ===");

        var countOnly = Accumulator.of(42, List.of("a", "b", "c"))
            .mapAccumulated(List::size);
        IO.println("value:       %s".formatted(countOnly.value()));       // 42
        IO.println("accumulated: %s".formatted(countOnly.accumulated())); // 3

        // ---- toTuple2 — interop ----

        IO.println("\n=== toTuple2 ===");

        Tuple2<List<String>, Integer> pair = chained.toTuple2();
        IO.println("_1 (accumulated): %s".formatted(pair._1())); // [step 1, step 2, step 3]
        IO.println("_2 (value):       %s".formatted(pair._2())); // 25

        // ---- hasValue ----

        IO.println("\n=== hasValue ===");

        IO.println("of.hasValue():   %s".formatted(Accumulator.of(1, List.of()).hasValue()));    // true
        IO.println("tell.hasValue(): %s".formatted(Accumulator.tell(List.of("x")).hasValue()));  // false

        // ---- Real-world: order pricing with audit trail ----

        IO.println("\n=== Real-world: order pricing audit ===");

        var baseOrder = new Order("ord-7", BigDecimal.valueOf(100.0));
        var priced = Accumulator.of(baseOrder, List.of("order loaded"))
            .flatMap(order -> {
                var discounted = order.basePrice()
                    .multiply(BigDecimal.valueOf(0.85));
                return Accumulator.of(
                    new Order(order.id(), discounted),
                    List.of("15%% discount applied → %s".formatted(discounted))
                );
            }, concat)
            .flatMap(order -> {
                var taxed = order.basePrice()
                    .multiply(BigDecimal.valueOf(1.21))
                    .setScale(2, RoundingMode.HALF_UP);
                return Accumulator.of(
                    new PricedOrder(order.id(), taxed, "USD"),
                    List.of("21%% VAT applied → %s".formatted(taxed))
                );
            }, concat);

        IO.println("Result:  %s".formatted(priced.value()));
        IO.println("Audit trail:");
        priced.accumulated().forEach(entry -> IO.println("  - %s".formatted(entry)));
        // Result:  PricedOrder[id=ord-7, price=102.85, currency=USD]
        // Audit trail:
        //   - order loaded
        //   - 15% discount applied → 85.00
        //   - 21% VAT applied → 102.85

        // ---- Real-world: counter accumulation ----

        // ---- combine — independent accumulators ----

        IO.println("\n=== combine ===");

        var userAcc = Accumulator.of("Alice", List.of("user loaded"));
        var scoreAcc = Accumulator.of(98, List.of("score loaded"));

        var dashboard = userAcc.combine(
            scoreAcc,
            concat,
            "%s — score: %s"::formatted
        );

        IO.println("value:       %s".formatted(dashboard.value()));       // Alice — score: 98
        IO.println("accumulated: %s".formatted(dashboard.accumulated())); // [user loaded, score loaded]

        // ---- sequence — fold a list of accumulators ----

        IO.println("\n=== sequence ===");

        var steps = List.of(
            Accumulator.of(10, List.of("step A")),
            Accumulator.of(20, List.of("step B")),
            Accumulator.of(30, List.of("step C"))
        );

        var seqResult = Accumulator.sequence(steps, concat, List.of());

        IO.println("value:       %s".formatted(seqResult.value()));       // [10, 20, 30]
        IO.println("accumulated: %s".formatted(seqResult.accumulated())); // [step A, step B, step C]

        // ---- toOption ----

        IO.println("\n=== toOption ===");

        IO.println("of.toOption():   %s".formatted(Accumulator.of(42, List.of()).toOption()));    // Some(42)
        IO.println("tell.toOption(): %s".formatted(Accumulator.tell(List.of("x")).toOption()));   // None

        // ---- toResult — convert to Result ----

        IO.println("\n=== toResult ===");

        var okResult = Accumulator.of(42, List.of("step"))
            .toResult();
        IO.println("of.toResult():   %s".formatted(okResult));  // Ok(42)

        var errResult = Accumulator.tell(List.of("only log"))
            .toResult();
        IO.println("tell.toResult(): %s".formatted(errResult)); // Err([only log])

        // ---- liftOption — log presence/absence ----

        IO.println("\n=== liftOption ===");

        var optFound = Accumulator.liftOption(
            Option.some("config.yaml"),
            path -> List.of("config found: %s".formatted(path)),
            List.of("config not found, using defaults")
        );
        IO.println("value:       %s".formatted(optFound.value()));       // Some(config.yaml)
        IO.println("accumulated: %s".formatted(optFound.accumulated())); // [config found: config.yaml]

        var optMissing = Accumulator.liftOption(
            Option.none(),
            path -> List.of("config found: %s".formatted(path)),
            List.of("config not found, using defaults")
        );
        IO.println("value:       %s".formatted(optMissing.value()));       // None
        IO.println("accumulated: %s".formatted(optMissing.accumulated())); // [config not found, using defaults]

        // ---- liftTry — log success/failure ----

        IO.println("\n=== liftTry ===");

        var tryOk = Accumulator.liftTry(
            Try.of(() -> Integer.parseInt("42")),
            v -> List.of("parsed: %s".formatted(v)),
            ex -> List.of("parse failed: %s".formatted(ex.getMessage()))
        );
        IO.println("value:       %s".formatted(tryOk.value()));       // Success(42)
        IO.println("accumulated: %s".formatted(tryOk.accumulated())); // [parsed: 42]

        var tryFail = Accumulator.liftTry(
            Try.of(() -> Integer.parseInt("not-a-number")),
            v -> List.of("parsed: %s".formatted(v)),
            ex -> List.of("parse failed: %s".formatted(ex.getMessage()))
        );
        IO.println("value.isFailure: %s".formatted(tryFail.value().isFailure()));  // true
        IO.println("accumulated:     %s".formatted(tryFail.accumulated()));        // [parse failed: ...]

        // ---- Real-world: step counter ----

        IO.println("\n=== Real-world: step counter ===");

        BinaryOperator<Integer> add = Integer::sum;

        var processed =
            Accumulator.pure("raw input", 0)
                .flatMap(s -> Accumulator.of(s.trim(), 1), add)
                .flatMap(s -> Accumulator.of(s.toUpperCase(), 1), add)
                .flatMap(s -> Accumulator.of("[%s]".formatted(s), 1), add);

        IO.println("value:  %s".formatted(processed.value()));       // [RAW INPUT]
        IO.println("steps:  %s".formatted(processed.accumulated())); // 3
    }
}
