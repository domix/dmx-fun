package codes.domix.fun;

import java.util.Optional;

public class Foo {
    class Profile { public String getDisplayName() { return "caca";}}
    static class User { public Option<Profile> getProfile() { return null;}}
    static class User2 { public Profile getProfile() { return null;}}
    static void main() {
        var user = new User();
        // Each step is honest about what it might not have
//        Option<String> name = user.getProfile()
//            .flatMap(profile -> Option.ofNullable(profile.getDisplayName()));

        var user2 = new User2();
        String name2 = Optional.of(user2)
            .map(User2::getProfile)      // getProfile() can return null...
            .map(Profile::getDisplayName)
            .orElse("no name"); //
        System.out.printf("name: %s%n", name2);
    }
}
