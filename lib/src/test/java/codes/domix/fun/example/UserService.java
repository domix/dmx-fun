package codes.domix.fun.example;

import codes.domix.fun.Result;
import java.util.regex.Pattern;


public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{10,30}$"
    );
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    protected Result<CreateUserCommand, String> isValidEmail(
        CreateUserCommand command
    ) {
        var email = command.email();
        if (email == null || email.isBlank()) {
            return Result.err("Provided email is either null or blank");
        }
        boolean emailMatches = EMAIL_PATTERN
            .matcher(email)
            .matches();

        if (!emailMatches) {
            return Result.err("Invalid email");
        }

        return Result.ok(command);
    }

    protected Result<CreateUserCommand, String> isValidPassword(
        CreateUserCommand command
    ) {

        var password = command.password();
        if (password == null) {
            return Result.err("Provided password is null");
        }
        boolean validPassword = PASSWORD_PATTERN
            .matcher(password)
            .matches();

        if (!validPassword) {
            return Result.err("Invalid password.");
        }

        return Result.ok(command);
    }

    public Result<User, String> createUser(CreateUserCommand command) {
        return this.isValidEmail(command)
            .flatMap(this::isValidPassword)
            .flatMap(this.userRepository::createUser);
    }
}
