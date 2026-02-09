package codes.domix.fun.example.site;

import codes.domix.fun.Option;
import codes.domix.fun.Try;
import java.util.function.Function;

public class QuickExample {
    record User(String name, String email, String password) {
    }

    static Function<String, Option<User>> findUser = email
        -> Option.some(new User("User", email, "secret"));

    private static String getUserName(String email) {
        // Handle options safely

        return findUser.apply(email)
            .map(User::email)
            .getOrElse("Anonymous");
    }

    private static void parseInt(String name) {
        // Handle errors functionally
        Try.of(() -> Integer.parseInt(name))
            .recover(throwable -> {
                System.out.println("Error parsing: " + throwable.getMessage());
                return 0;
            })
            .onSuccess(System.out::println);
    }
}
