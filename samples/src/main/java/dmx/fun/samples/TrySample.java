package dmx.fun.samples;

import dmx.fun.Result;
import dmx.fun.Try;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Demonstrates Try<V>: wraps a computation that may throw and turns the exception into a value.
 * Use Try when calling legacy or third-party APIs that throw checked or runtime exceptions.
 */
public class TrySample {

    sealed interface ConfigError {
        record ReadFailed(String path, Throwable cause) implements ConfigError {}
        record ParseFailed(String value)                implements ConfigError {}
    }

    // Legacy API that throws
    static int parsePort(String raw) {
        return Integer.parseInt(raw); // throws NumberFormatException for bad input
    }

    static void main(String[] args) {
        // Wrap a throwing call — no try/catch needed
        Try<Integer> port = Try.of(() -> parsePort("8080"));
        System.out.println("Port: " + port.getOrElse(80)); // 8080

        // Bad input — failure becomes a value
        Try<Integer> badPort = Try.of(() -> parsePort("not-a-number"));
        System.out.println("Bad port failed: " + badPort.isFailure()); // true

        // Recover from a specific failure
        Try<Integer> recovered = Try.of(() -> parsePort("bad"))
            .recover(e -> 80); // default port on any failure
        System.out.println("Recovered port: " + recovered.getOrElse(-1)); // 80

        // Chain transformations — map skips if already a failure
        Try<String> host = Try.of(() -> parsePort("9000"))
            .map(p -> "localhost:" + p);
        System.out.println("Host: " + host.getOrElse("unavailable")); // localhost:9000

        // Convert to Result for the service layer — typed error instead of Throwable
        Result<Integer, ConfigError> result = Try.of(() -> parsePort("abc"))
            .toResult(e -> new ConfigError.ParseFailed("abc"));
        System.out.println("Result is err: " + result.isError()); // true

        // Wrap an IO operation
        Try<String> content = Try.of(() ->
            Files.readString(Path.of("/tmp/dmx-fun-sample.txt")));
        content
            .onSuccess(c -> System.out.println("File content: " + c))
            .onFailure(e -> System.out.println("File not found: " + e.getClass().getSimpleName()));

        // ---- withTimeout ----

        System.out.println("\n=== withTimeout ===");

        // Completes within the deadline
        Try<String> fast = Try.withTimeout(Duration.ofSeconds(5), () -> "quick result");
        fast.onSuccess(v -> System.out.println("Got: " + v)); // Got: quick result

        // Exceeds the deadline
        Try<String> slow = Try.withTimeout(Duration.ofMillis(100), () -> {
            Thread.sleep(10_000);
            return "never";
        });
        System.out.println("Timed out: " + slow.isFailure()); // true
        System.out.println("Cause: " + slow.getCause().getMessage()); // Operation timed out after 100ms

        // Recover from timeout with a fallback
        String value = Try.withTimeout(Duration.ofMillis(50), () -> {
                Thread.sleep(5_000);
                return "live-data";
            })
            .recover(TimeoutException.class, ex -> "cached-data")
            .getOrElse("unknown");
        System.out.println("Value: " + value); // cached-data
    }
}
