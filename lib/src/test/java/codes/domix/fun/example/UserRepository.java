package codes.domix.fun.example;

import codes.domix.fun.Result;

public interface UserRepository {
    Result<User, String> createUser(CreateUserCommand command);
}
