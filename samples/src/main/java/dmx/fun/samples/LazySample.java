package dmx.fun.samples;

import dmx.fun.Lazy;
import java.time.Instant;

/**
 * Demonstrates Lazy<T>: a value computed at most once, on first access.
 * Use Lazy to defer expensive computations and cache the result thread-safely.
 */
public class LazySample {

    // Simulates an expensive lookup (e.g. reading config, loading a resource)
    static String loadDatabaseUrl() {
        IO.println("  [loading DB URL — this runs only once]");
        return "jdbc:postgresql://localhost:5432/mydb";
    }

    static void main(String[] args) {
        // The supplier is NOT called here — computation is deferred
        var dbUrl = Lazy.of(LazySample::loadDatabaseUrl);
        IO.println("Lazy created, nothing computed yet");

        // First access — supplier is called
        IO.println("First get: " + dbUrl.get());

        // Second access — cached, supplier is NOT called again
        IO.println("Second get: " + dbUrl.get());

        // Map over a Lazy value — evaluation is still deferred
        var host = Lazy.of(LazySample::loadDatabaseUrl)
            .map(url -> url.split("/")[2]); // extract host:port

        IO.println("Host (triggers evaluation): " + host.get());

        // Practical example: application config loaded once at startup
        var config = Lazy.of(() -> {
            IO.println("  [initialising config]");
            return new AppConfig("prod", 8080, Instant.now());
        });

        IO.println("Config port: " + config.get().port());
        IO.println("Config port again (cached): " + config.get().port());
    }

    record AppConfig(String env, int port, Instant startedAt) {}
}
