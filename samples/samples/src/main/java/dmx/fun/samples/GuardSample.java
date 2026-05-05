package dmx.fun.samples;

import dmx.fun.Guard;
import dmx.fun.NonEmptyList;
import dmx.fun.Tuple2;
import java.util.stream.Stream;

/**
 * Demonstrates Guard<T>: composable, named predicates that produce Validated results.
 * Use Guard to define validation rules once and compose them declaratively into pipelines.
 */
public class GuardSample {

    record RegistrationForm(String username, String email, int age) {}
    record User(String name) {}

    static void main() {

        // ---- Basic usage — of with static message ----

        IO.println("=== Basic usage ===");

        var notBlank = Guard.<String>of(s -> !s.isBlank(), "must not be blank");

        IO.println(notBlank.check("hello").isValid());  // true
        IO.println(notBlank.check("   ").isValid());    // false
        IO.println(notBlank.check("   ").getError());   // [must not be blank]

        // ---- Dynamic message — value included in error ----

        IO.println("\n=== Dynamic error message ===");

        var max100 = Guard.<Integer>of(
            n -> n <= 100,
            n -> "must be ≤ 100, got " + n
        );

        IO.println(max100.check(50).isValid());                      // true
        IO.println(max100.check(150).getError().head());             // must be ≤ 100, got 150

        // ---- and — accumulates errors from all failing guards ----

        IO.println("\n=== and (error accumulation) ===");

        var minLength3   = Guard.<String>of(s -> s.length() >= 3,            "min 3 chars");
        var alphanumeric = Guard.<String>of(s -> s.matches("[a-zA-Z0-9]+"),   "must be alphanumeric");

        var username = notBlank
            .and(minLength3)
            .and(alphanumeric);

        IO.println(username.check("alice").isValid());               // true
        IO.println(username.check("a!").getError().toList());        // [min 3 chars, must be alphanumeric]
        IO.println(username.check("  ").getError().size());          // 3 — all three fail

        // ---- or — first passing guard short-circuits ----

        IO.println("\n=== or (short-circuit) ===");

        var email = Guard.<String>of(s -> s.contains("@"),   "must contain @");
        var phone = Guard.<String>of(s -> s.matches("\\d+"), "must be all digits");

        var contact = email.or(phone);

        IO.println(contact.check("alice@example.com").isValid());    // true — email passes
        IO.println(contact.check("12345").isValid());                // true — phone passes
        IO.println(contact.check("hello").getError().toList());      // [must contain @, must be all digits]

        // ---- negate ----

        IO.println("\n=== negate ===");

        var noAdmin = Guard.<String>of(s -> s.equals("admin"), "is admin")
            .negate("username must not be 'admin'");

        IO.println(noAdmin.check("alice").isValid());                // true
        IO.println(noAdmin.check("admin").getError().head());        // username must not be 'admin'

        // ---- Integration with Validated.combine ----

        IO.println("\n=== Validated.combine ===");

        var positive  = Guard.<Integer>of(n -> n > 0,   "age must be positive");
        var maxAge    = Guard.<Integer>of(n -> n <= 120, "age must be ≤ 120");
        var ageGuard  = positive.and(maxAge);

        var usernameV = username.check("al!");
        var emailV    = email.check("not-an-email");
        var ageV      = ageGuard.check(-5);

        // Combine all three — errors accumulate across fields
        var form = usernameV
            .combine(
                emailV,
                NonEmptyList::concat,
                Tuple2::new
            )
            .combine(
                ageV,
                NonEmptyList::concat,
                (ue, age)
                    -> new RegistrationForm(ue._1(), ue._2(), age)
            );

        IO.println("Valid: " + form.isValid());                       // false
        form.peekError(errors -> errors.toList()
            .forEach(e -> IO.println("Error: " + e)));
        // Error: min 3 chars
        // Error: must be alphanumeric
        // Error: must contain @
        // Error: age must be positive

        // ---- asPredicate — Java stdlib interop ----

        IO.println("\n=== asPredicate ===");

        var valid = Stream.of("alice", "  ", "bob", "")
            .filter(notBlank.asPredicate())
            .toList();
        IO.println("Valid names: " + valid); // [alice, bob]

        // ---- contramap — adapt a field guard to a whole object ----

        IO.println("\n=== contramap ===");

        var userGuard = notBlank.contramap(User::name);
        IO.println(userGuard.check(new User("alice")).isValid());  // true
        IO.println(userGuard.check(new User("  ")).isValid());     // false

        // ---- checkToResult — Result integration ----

        IO.println("\n=== checkToResult ===");

        var r1 = username.checkToResult("alice");
        IO.println("checkToResult ok: " + r1.isSuccess()); // true

        var r2 = username.checkToResult(
            "a!", errors -> String.join("; ", errors.toList())
        );
        IO.println("checkToResult err: " + r2.getError()); // min 3 chars; must be alphanumeric

        // ---- checkToOption — discard errors ----

        IO.println("\n=== checkToOption ===");

        var opt1 = username.checkToOption("alice");
        IO.println("checkToOption some: " + opt1.isDefined()); // true

        var validOnly = Stream.of("alice", "a!", "bob")
            .flatMap(s -> username.checkToOption(s).stream())
            .toList();
        IO.println("Valid only: " + validOnly); // [alice, bob]
    }
}
