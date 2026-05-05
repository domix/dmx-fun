package dmx.fun.example;

import module dmx.fun;

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
