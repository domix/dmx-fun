package codes.domix.fun.example;

import codes.domix.fun.Result;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    protected Result<CreateUserCommand, String> isValidEmail(CreateUserCommand command) {
        if (command.email().contains("@")) {
            return Result.ok(command);
        }
        return Result.err("Invalid email");
    }

    protected Result<CreateUserCommand, String> isValidPassword(CreateUserCommand command) {

        if (command.password().length() >= 6) {
            return Result.ok(command);
        }

        return Result.err("Invalid password");
    }

    public Result<User, String> createUser(CreateUserCommand command) {
        Result<Result<CreateUserCommand, String>, String> map = this.isValidEmail(command)
            .map(this::isValidPassword);

        Result<CreateUserCommand, String> validPassword = this.isValidPassword(command);

        if (validPassword.isOk()) {
            CreateUserCommand createUserCommand = validPassword.get();
        } else {

        }

        final var result = this.isValidEmail(command)
            .flatMap(this::isValidPassword)
            .flatMap(this.userRepository::createUser);

        // 1. Validar email
        if (this.isValidEmail(command).isError()) {
            throw new IllegalArgumentException("Invalid email");
        }
        // 2. Validar password
        if (this.isValidPassword(command).isError()) {
            throw new IllegalArgumentException("Invalid password");
        }

        // 3. Guardar en la Base de datos
        return this.userRepository.createUser(command);
    }
}
