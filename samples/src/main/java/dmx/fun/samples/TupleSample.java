package dmx.fun.samples;

import dmx.fun.Result;
import dmx.fun.Tuple2;
import dmx.fun.Tuple3;
import dmx.fun.Tuple4;

/**
 * Demonstrates Tuple2/3/4: typed heterogeneous tuples without a dedicated class.
 * Fields are accessed via record accessors _1(), _2(), _3(), _4().
 */
public class TupleSample {

    static Tuple2<String, Integer> parseNameAge(String csv) {
        String[] parts = csv.split(",");
        return new Tuple2<>(parts[0].trim(), Integer.parseInt(parts[1].trim()));
    }

    static Result<Tuple3<String, String, Integer>, String> lookupUser(String id) {
        return switch (id) {
            case "u1" -> Result.ok(Tuple3.of("Alice", "alice@example.com", 30));
            default   -> Result.err("User not found: " + id);
        };
    }

    static void main(String[] args) {
        // Tuple2 — parse a CSV row into typed fields
        Tuple2<String, Integer> nameAge = parseNameAge("Alice, 30");
        System.out.println("Name: " + nameAge._1()); // Alice
        System.out.println("Age:  " + nameAge._2()); // 30

        // Tuple3 — return three values from a lookup
        lookupUser("u1").peek(t ->
            System.out.printf("User: %s <%s> age %d%n", t._1(), t._2(), t._3()));

        // Tuple4 — combine four pieces of data
        Tuple4<String, Integer, Boolean, Double> product =
            Tuple4.of("Widget", 42, true, 9.99);
        System.out.println("Product: " + product._1()
            + " qty="       + product._2()
            + " available=" + product._3()
            + " price="     + product._4());

        // Map over Tuple3 slots
        Tuple3<String, String, Integer> updated = Tuple3.of("Alice", "alice@example.com", 30)
            .mapThird(age -> age + 10);
        System.out.println("In 10 years: " + updated._3()); // 40

        // Collapse a Tuple3 to a single value
        String label = Tuple3.of("Alice", "alice@example.com", 30)
            .map((name, email, age) -> name + " <" + email + "> (" + age + ")");
        System.out.println("Label: " + label);
    }
}
