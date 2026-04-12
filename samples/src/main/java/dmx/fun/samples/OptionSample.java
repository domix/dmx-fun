package dmx.fun.samples;

import dmx.fun.Option;

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

    static void main(String[] args) {
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
    }
}
