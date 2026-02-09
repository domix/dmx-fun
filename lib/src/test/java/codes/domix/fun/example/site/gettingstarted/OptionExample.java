package codes.domix.fun.example.site.gettingstarted;

import codes.domix.fun.Option;

 class OptionExample {
    void main() {
        // Create an Option
        Option<String> some = Option.some("Hello");
        Option<String> none = Option.none();

        // Map over Option
        Option<Integer> length = some.map(String::length); // Some(5)

        // Provide default value
        String value = none.getOrElse("Default"); // "Default"

        // Chain operations
        Option<String> result = some
            .filter(s -> s.length() > 3)
            .map(String::toUpperCase); // Some("HELLO")
    }
}
