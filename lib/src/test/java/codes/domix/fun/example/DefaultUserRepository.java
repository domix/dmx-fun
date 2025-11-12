package codes.domix.fun.example;

import codes.domix.fun.Result;

public class DefaultUserRepository implements UserRepository {
    @Override
    public Result<User, String> createUser(CreateUserCommand command) {
        boolean error = false;
        if (error) {
            return Result.err("Error creating user");
        }
        return Result.ok(new User("", command.email(), command.password()));
    }
}
