package dmx.fun.samples;

import dmx.fun.NonEmptyList;
import dmx.fun.Validated;

/**
 * Demonstrates Validated<E, A>: accumulates all validation errors instead of failing on the first.
 * Use Validated for form/DTO validation where you want to report every problem at once.
 */
public class ValidatedSample {

    record RegistrationForm(String username, String email) {}

    record FullProfile(String username, String email, int age) {}

    record UserAccount(String username, String email, int age, String country) {}

    // -------------------------------------------------------------------------
    // Validators
    // -------------------------------------------------------------------------

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

    static Validated<NonEmptyList<String>, Integer> validateAge(int age) {
        if (age < 0)
            return Validated.invalidNel("Age must not be negative");
        if (age > 150)
            return Validated.invalidNel("Age is unrealistically large");
        return Validated.valid(age);
    }

    static Validated<NonEmptyList<String>, String> validateCountry(String country) {
        if (country == null || country.length() != 2)
            return Validated.invalidNel("Country must be a 2-letter ISO code");
        return Validated.valid(country.toUpperCase());
    }

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /** combine — two independent validations (original). */
    static Validated<NonEmptyList<String>, RegistrationForm> validateRegistration(
            String username, String email) {
        return validateUsername(username)
            .combine(
                validateEmail(email),
                NonEmptyList::concat,
                RegistrationForm::new
            );
    }

    /** combine3 — three independent validations in one flat call. */
    static Validated<NonEmptyList<String>, FullProfile> validateFullProfile(
            String username, String email, int age) {
        return Validated.combine3(
            validateUsername(username),
            validateEmail(email),
            validateAge(age),
            NonEmptyList::concat,
            FullProfile::new
        );
    }

    /** combine4 — four independent validations in one flat call. */
    static Validated<NonEmptyList<String>, UserAccount> validateUserAccount(
            String username, String email, int age, String country) {
        return Validated.combine4(
            validateUsername(username),
            validateEmail(email),
            validateAge(age),
            validateCountry(country),
            NonEmptyList::concat,
            UserAccount::new
        );
    }

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------

    public static void main(String[] args) {

        // ---- combine (2 fields) ----

        System.out.println("=== combine (2 fields) ===");

        Validated<NonEmptyList<String>, RegistrationForm> ok =
            validateRegistration("alice", "alice@example.com");
        ok.peek(form -> System.out.println("Registered: " + form.username())); // alice

        Validated<NonEmptyList<String>, RegistrationForm> twoErrors =
            validateRegistration("al", "not-an-email");
        twoErrors.peekError(nel ->
            nel.toList().forEach(e -> System.out.println("Error: " + e)));
        // Error: Username must be at least 3 characters
        // Error: Email must contain @

        // ---- combine3 (3 fields) ----

        System.out.println("\n=== combine3 (3 fields) ===");

        Validated<NonEmptyList<String>, FullProfile> profile =
            validateFullProfile("alice", "alice@example.com", 30);
        profile.peek(p -> System.out.println("Profile: " + p)); // FullProfile[alice, alice@example.com, 30]

        // All three fields invalid — all errors collected in one pass
        Validated<NonEmptyList<String>, FullProfile> threeErrors =
            validateFullProfile("x", "bad-email", -5);
        threeErrors.peekError(nel -> {
            System.out.println("Errors (" + nel.size() + "):");
            nel.toList().forEach(e -> System.out.println("  - " + e));
        });
        // Errors (3):
        //   - Username must be at least 3 characters
        //   - Email must contain @
        //   - Age must not be negative

        // ---- combine4 (4 fields) ----

        System.out.println("\n=== combine4 (4 fields) ===");

        Validated<NonEmptyList<String>, UserAccount> account =
            validateUserAccount("bob", "bob@example.com", 25, "mx");
        account.peek(a -> System.out.println("Account: " + a)); // UserAccount[bob, bob@example.com, 25, MX]

        // All four fields invalid — all errors collected in one pass
        Validated<NonEmptyList<String>, UserAccount> fourErrors =
            validateUserAccount("", "no-at-sign", 200, "INVALID");
        fourErrors.peekError(nel -> {
            System.out.println("Errors (" + nel.size() + "):");
            nel.toList().forEach(e -> System.out.println("  - " + e));
        });
        // Errors (4):
        //   - Username must not be blank
        //   - Email must contain @
        //   - Age is unrealistically large
        //   - Country must be a 2-letter ISO code

        // ---- Pattern match ----

        System.out.println("\n=== Pattern match ===");

        switch (validateRegistration("bob", "bob@example.com")) {
            case Validated.Valid<NonEmptyList<String>, RegistrationForm> v ->
                System.out.println("Valid: " + v.value().username());
            case Validated.Invalid<NonEmptyList<String>, RegistrationForm> inv ->
                System.out.println("Invalid: " + inv.error().toList());
        }
    }
}
