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
        System.out.println("  [loading DB URL — this runs only once]");
        return "jdbc:postgresql://localhost:5432/mydb";
    }

    static void main(String[] args) {
        // The supplier is NOT called here — computation is deferred
        Lazy<String> dbUrl = Lazy.of(LazySample::loadDatabaseUrl);
        System.out.println("Lazy created, nothing computed yet");

        // First access — supplier is called
        System.out.println("First get: " + dbUrl.get());

        // Second access — cached, supplier is NOT called again
        System.out.println("Second get: " + dbUrl.get());

        // Map over a Lazy value — evaluation is still deferred
        Lazy<String> host = Lazy.of(LazySample::loadDatabaseUrl)
            .map(url -> url.split("/")[2]); // extract host:port

        System.out.println("Host (triggers evaluation): " + host.get());

        // Practical example: application config loaded once at startup
        Lazy<AppConfig> config = Lazy.of(() -> {
            System.out.println("  [initialising config]");
            return new AppConfig("prod", 8080, Instant.now());
        });

        System.out.println("Config port: " + config.get().port());
        System.out.println("Config port again (cached): " + config.get().port());
    }

    record AppConfig(String env, int port, Instant startedAt) {}
}
