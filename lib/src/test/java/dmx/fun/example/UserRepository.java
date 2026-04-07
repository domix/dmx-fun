package dmx.fun.example;

import module dmx.fun;

public interface UserRepository {
    Result<User, String> createUser(CreateUserCommand command);
}
