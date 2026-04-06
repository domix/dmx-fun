package codes.domix.fun.example;

import module codes.domix.fun;

public interface UserRepository {
    Result<User, String> createUser(CreateUserCommand command);
}
