package dmx.fun.samples;

import dmx.fun.Result;

/**
 * Demonstrates Result<V, E>: an operation that can succeed or fail with a typed error.
 * Prefer Result over exceptions when the caller must handle the failure case.
 */
public class ResultSample {

    sealed interface UserError {
        record NotFound(String id)        implements UserError {}
        record InvalidEmail(String email) implements UserError {}
        record Inactive(String id)        implements UserError {}
    }

    record User(String id, String email, boolean active) {}

    static Result<User, UserError> findUser(String id) {
        return switch (id) {
            case "u1" -> Result.ok(new User("u1", "alice@example.com", true));
            case "u2" -> Result.ok(new User("u2", "bob@example.com",   false));
            default   -> Result.err(new UserError.NotFound(id));
        };
    }

    static Result<User, UserError> validateActive(User user) {
        return user.active()
            ? Result.ok(user)
            : Result.err(new UserError.Inactive(user.id()));
    }

    static Result<String, UserError> extractEmail(User user) {
        String email = user.email();
        return email.contains("@")
            ? Result.ok(email)
            : Result.err(new UserError.InvalidEmail(email));
    }

    static void main(String[] args) {
        // Happy path — three steps chained with flatMap
        Result<String, UserError> email = findUser("u1")
            .flatMap(ResultSample::validateActive)
            .flatMap(ResultSample::extractEmail);
        System.out.println("Email: " + email.getOrElse("none")); // alice@example.com

        // Inactive user — error propagates automatically
        Result<String, UserError> inactive = findUser("u2")
            .flatMap(ResultSample::validateActive)
            .flatMap(ResultSample::extractEmail);
        System.out.println("Inactive is error: " + inactive.isError()); // true

        // Side-effect on success
        findUser("u1")
            .peek(u -> System.out.println("Found user: " + u.email()));

        // Side-effect on error
        findUser("unknown")
            .peekError(e -> System.out.println("Error: " + e));

        // Pattern match on the result variant
        switch (findUser("unknown")) {
            case Result.Ok<User, UserError>  ok  -> System.out.println("Found: "  + ok.value());
            case Result.Err<User, UserError> err -> System.out.println("Error: "  + err.error());
        }
    }
}
