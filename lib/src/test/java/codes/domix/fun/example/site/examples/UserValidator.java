package codes.domix.fun.example.site.examples;

import codes.domix.fun.Result;

public class UserValidator {
    public record User(String email, String password) {
    }

    public Result<User, String> validateAndCreateUser(
        String email,
        String password
    ) {
        return validateEmail(email)
            .flatMap(e -> validatePassword(password)
                .map(p -> new User(e, p)));
    }

    private Result<String, String> validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            return Result.err("Email is required");
        }
        if (!email.contains("@")) {
            return Result.err("Invalid email format");
        }
        return Result.ok(email);
    }

    private Result<String, String> validatePassword(String password) {
        if (password == null || password.length() < 8) {
            return Result.err("Password must be at least 8 characters");
        }
        return Result.ok(password);
    }

    public Result<User, String> createUser() {
        // Usage
        UserValidator validator = new UserValidator();
        Result<User, String> result = validator.validateAndCreateUser(
            "user@example.com",
            "securepass123"
        );

        String message = result.fold(
            user -> "User created: " + user.email(),
            error -> "Validation failed: " + error
        );
        System.out.println(message);

        return result;
    }

}
