package domix.fun.example;

import domix.fun.Result;

public interface UserRepository {
    Result<User, String> createUser(CreateUserCommand command);
}
