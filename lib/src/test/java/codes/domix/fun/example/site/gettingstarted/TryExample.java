package codes.domix.fun.example.site.gettingstarted;

import codes.domix.fun.Try;

class TryExample {
    void main() {
        // Wrap risky operations
        Try<Integer> result = Try.of(() -> Integer.parseInt("123"));

        // Handle success and failure
        String message = result
            .map(i -> "Success: " + i)
            .recover(throwable -> "Failure: " + throwable.getMessage())
            .get();

        // Chain operations
        Try<Integer> computed = Try.of(() -> "42")
            .map(Integer::parseInt)
            .map(i -> i * 2); // Success(84)
    }
}
