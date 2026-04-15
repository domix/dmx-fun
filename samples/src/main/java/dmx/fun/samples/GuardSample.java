package dmx.fun.samples;

import dmx.fun.Guard;
import dmx.fun.NonEmptyList;
import dmx.fun.Validated;

/**
 * Demonstrates Guard<T>: composable, named predicates that produce Validated results.
 * Use Guard to define validation rules once and compose them declaratively into pipelines.
 */
public class GuardSample {

    record RegistrationForm(String username, String email, int age) {}

    static void main() {

        // ---- Basic usage — of with static message ----

        System.out.println("=== Basic usage ===");

        Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");

        System.out.println(notBlank.check("hello").isValid());  // true
        System.out.println(notBlank.check("   ").isValid());    // false
        System.out.println(notBlank.check("   ").getError());   // [must not be blank]

        // ---- Dynamic message — value included in error ----

        System.out.println("\n=== Dynamic error message ===");

        Guard<Integer> max100 = Guard.of(
            n -> n <= 100,
            n -> "must be ≤ 100, got " + n
        );

        System.out.println(max100.check(50).isValid());                      // true
        System.out.println(max100.check(150).getError().head());             // must be ≤ 100, got 150

        // ---- and — accumulates errors from all failing guards ----

        System.out.println("\n=== and (error accumulation) ===");

        Guard<String> minLength3   = Guard.of(s -> s.length() >= 3,            "min 3 chars");
        Guard<String> alphanumeric = Guard.of(s -> s.matches("[a-zA-Z0-9]+"),   "must be alphanumeric");

        Guard<String> username = notBlank.and(minLength3).and(alphanumeric);

        System.out.println(username.check("alice").isValid());               // true
        System.out.println(username.check("a!").getError().toList());        // [min 3 chars, must be alphanumeric]
        System.out.println(username.check("  ").getError().size());          // 3 — all three fail

        // ---- or — first passing guard short-circuits ----

        System.out.println("\n=== or (short-circuit) ===");

        Guard<String> email = Guard.of(s -> s.contains("@"),   "must contain @");
        Guard<String> phone = Guard.of(s -> s.matches("\\d+"), "must be all digits");

        Guard<String> contact = email.or(phone);

        System.out.println(contact.check("alice@example.com").isValid());    // true — email passes
        System.out.println(contact.check("12345").isValid());                // true — phone passes
        System.out.println(contact.check("hello").getError().toList());      // [must contain @, must be all digits]

        // ---- negate ----

        System.out.println("\n=== negate ===");

        Guard<String> noAdmin = Guard.<String>of(s -> s.equals("admin"), "is admin")
            .negate("username must not be 'admin'");

        System.out.println(noAdmin.check("alice").isValid());                // true
        System.out.println(noAdmin.check("admin").getError().head());        // username must not be 'admin'

        // ---- Integration with Validated.combine ----

        System.out.println("\n=== Validated.combine ===");

        Guard<Integer> positive  = Guard.of(n -> n > 0,   "age must be positive");
        Guard<Integer> maxAge    = Guard.of(n -> n <= 120, "age must be ≤ 120");
        Guard<Integer> ageGuard  = positive.and(maxAge);

        Validated<NonEmptyList<String>, String>  usernameV = username.check("al!");
        Validated<NonEmptyList<String>, String>  emailV    = email.check("not-an-email");
        Validated<NonEmptyList<String>, Integer> ageV      = ageGuard.check(-5);

        // Combine all three — errors accumulate across fields
        Validated<NonEmptyList<String>, RegistrationForm> form =
            usernameV.combine(emailV, NonEmptyList::concat, (u, e) -> u + "/" + e)
                     .combine(ageV,   NonEmptyList::concat, (ue, age) -> new RegistrationForm(ue, ue, age));

        System.out.println("Valid: " + form.isValid());                       // false
        form.peekError(errors -> errors.toList().forEach(
            e -> System.out.println("Error: " + e)));
        // Error: min 3 chars
        // Error: must be alphanumeric
        // Error: must contain @
        // Error: age must be positive
    }
}
