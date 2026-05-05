package dmx.fun.samples;

import dmx.fun.Option;
import dmx.fun.Tuple2;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Demonstrates Option<T>: a value that may or may not be present.
 * Use Option instead of null or @Nullable return types.
 */
public class OptionSample {

    // A repository method that may find no record
    static Option<String> findEmail(String username) {
        if ("alice".equals(username)) {
            return Option.some("alice@example.com");
        }
        return Option.none();
    }

    static Option<Integer> findPort(String host) {
        return "api.example.com".equals(host) ? Option.some(8080) : Option.none();
    }

    public static void main(String[] args) {
        // Transform the value if present — map returns None when the source is None
        Option<Integer> length = findEmail("alice").map(String::length);
        System.out.println("Email length: " + length.getOrElse(0)); // 17

        // Chain a lookup that may also produce None
        Option<String> domain = findEmail("alice")
            .flatMap(email -> {
                int at = email.indexOf('@');
                return at >= 0 ? Option.some(email.substring(at + 1)) : Option.none();
            });
        System.out.println("Domain: " + domain.getOrElse("unknown")); // example.com

        // Missing user — all combinators propagate None gracefully
        Option<String> missing = findEmail("bob").map(String::toUpperCase);
        System.out.println("Missing: " + missing.getOrElse("not found")); // not found

        // Pattern match with sealed interface
        String result = switch (findEmail("alice")) {
            case Option.Some<String> some -> "Found: " + some.value();
            case Option.None<String> none -> "Not found";
        };
        System.out.println(result); // Found: alice@example.com

        // ---- zipWith(Function) / flatZip ----

        System.out.println("\n=== zipWith(Function) ===");

        // Pair the value with a derived Option — both present
        Option<Tuple2<String, Integer>> emailWithLength =
            findEmail("alice").zipWith(e -> Option.some(e.length()));
        emailWithLength.peek(t -> System.out.println("email=" + t._1() + " len=" + t._2()));
        // email=alice@example.com len=17

        // Derived lookup returns None — whole result is None
        Option<Tuple2<String, Integer>> noPort =
            findEmail("alice").zipWith(e -> findPort(e)); // no port for email domain
        System.out.println("No port: " + noPort.isEmpty()); // true

        // flatZip — alias, same semantics
        Option<Tuple2<String, Integer>> hostWithPort =
            Option.some("api.example.com").flatZip(h -> findPort(h));
        hostWithPort.peek(t -> System.out.println("host=" + t._1() + " port=" + t._2()));
        // host=api.example.com port=8080

        // Source is None — mapper is not called
        Option<Tuple2<String, Integer>> noneSource =
            Option.<String>none().zipWith(e -> findPort(e));
        System.out.println("None source: " + noneSource.isEmpty()); // true

        // ---- sequenceCollector ----

        System.out.println("\n=== sequenceCollector ===");

        // All Some — present list
        Optional<List<String>> allFound = Stream.of("alice", "alice")
            .map(u -> findEmail(u))
            .collect(Option.sequenceCollector());
        System.out.println("All found: " + allFound.isPresent()); // true
        System.out.println("Emails: " + allFound.get()); // [alice@example.com, alice@example.com]

        // Any None — empty Optional
        Optional<List<String>> oneMissing = Stream.of("alice", "bob")
            .map(u -> findEmail(u))
            .collect(Option.sequenceCollector());
        System.out.println("One missing: " + oneMissing.isEmpty()); // true
    }
}
