package dmx.fun.samples;

import dmx.fun.Option;
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

    static void main() {
        // Transform the value if present — map returns None when the source is None
        var length = findEmail("alice")
            .map(String::length);
        IO.println("Email length: " + length.getOrElse(0)); // 17

        // Chain a lookup that may also produce None
        var domain = findEmail("alice")
            .flatMap(email -> {
                int at = email.indexOf('@');
                return at >= 0 ? Option.some(email.substring(at + 1)) : Option.none();
            });
        IO.println("Domain: " + domain.getOrElse("unknown")); // example.com

        // Missing user — all combinators propagate None gracefully
        var missing = findEmail("bob")
            .map(String::toUpperCase);
        IO.println("Missing: " + missing.getOrElse("not found")); // not found

        // Pattern match with sealed interface
        var result = switch (findEmail("alice")) {
            case Option.Some<String> some -> "Found: " + some.value();
            case Option.None<String> _ -> "Not found";
        };
        IO.println(result); // Found: alice@example.com

        // ---- zipWith(Function) / flatZip ----

        IO.println("\n=== zipWith(Function) ===");

        // Pair the value with a derived Option — both present
        var emailWithLength =
            findEmail("alice")
                .zipWith(e -> Option.some(e.length()));
        emailWithLength
            .peek(t -> IO.println("email=%s len=%d".formatted(t._1(), t._2())));
        // email=alice@example.com len=17

        // Derived lookup returns None — whole result is None
        var noPort =
            findEmail("alice").zipWith(OptionSample::findPort); // no port for email domain
        IO.println("No port: " + noPort.isEmpty()); // true

        // flatZip — alias, same semantics
        var hostWithPort =
            Option.some("api.example.com")
                .flatZip(OptionSample::findPort);
        hostWithPort
            .peek(t -> IO.println("host=%s port=%d".formatted(t._1(), t._2())));
        // host=api.example.com port=8080

        // Source is None — mapper is not called
        var noneSource =
            Option.<String>none().zipWith(OptionSample::findPort);
        IO.println("None source: %s".formatted(noneSource.isEmpty())); // true

        // ---- sequenceCollector ----

        IO.println("\n=== sequenceCollector ===");

        // All Some — Some(list)
        var allFound = Stream.of("alice", "alice")
            .map(OptionSample::findEmail)
            .collect(Option.sequenceCollector());
        IO.println("All found: " + allFound.isDefined()); // true
        IO.println("Emails: " + allFound.get()); // [alice@example.com, alice@example.com]

        // Any None — None
        var oneMissing = Stream.of("alice", "bob")
            .map(OptionSample::findEmail)
            .collect(Option.sequenceCollector());
        IO.println("One missing: " + oneMissing.isEmpty()); // true
    }
}
