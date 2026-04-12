package dmx.fun.samples;

import dmx.fun.NonEmptyList;
import dmx.fun.Tuple2;
import dmx.fun.Validated;

/**
 * Demonstrates Validated<E, A>: accumulates all validation errors instead of failing on the first.
 * Use Validated for form/DTO validation where you want to report every problem at once.
 */
public class ValidatedSample {

    record RegistrationForm(String username, String email) {}

    static Validated<NonEmptyList<String>, String> validateUsername(String username) {
        if (username == null || username.isBlank())
            return Validated.invalidNel("Username must not be blank");
        if (username.length() < 3)
            return Validated.invalidNel("Username must be at least 3 characters");
        return Validated.valid(username);
    }

    static Validated<NonEmptyList<String>, String> validateEmail(String email) {
        if (email == null || !email.contains("@"))
            return Validated.invalidNel("Email must contain @");
        return Validated.valid(email);
    }

    static Validated<NonEmptyList<String>, RegistrationForm> validate(String username, String email) {
        return validateUsername(username)
            .combine(
                validateEmail(email),
                NonEmptyList::concat,
                RegistrationForm::new
            );
    }

    static void main(String[] args) {
        // All fields valid
        Validated<NonEmptyList<String>, RegistrationForm> ok =
            validate("alice", "alice@example.com");
        ok.peek(form -> System.out.println("Registered: " + form.username())); // alice

        // Multiple errors — all collected
        Validated<NonEmptyList<String>, RegistrationForm> errors =
            validate("al", "not-an-email");
        errors.peekError(nel ->
            nel.toList().forEach(e -> System.out.println("Error: " + e)));
        // Error: Username must be at least 3 characters
        // Error: Email must contain @

        // Pattern match
        switch (validate("bob", "bob@example.com")) {
            case Validated.Valid<NonEmptyList<String>, RegistrationForm> v ->
                System.out.println("Valid: " + v.value().username());
            case Validated.Invalid<NonEmptyList<String>, RegistrationForm> inv ->
                System.out.println("Invalid: " + inv.error().toList());
        }
    }
}
