package dmx.fun.samples;

import dmx.fun.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Demonstrates Resource<T>: a composable managed resource that guarantees acquisition
 * and release are always paired, even in the presence of exceptions.
 * Use Resource when you need to bracket an operation around a resource (file, connection, lock)
 * and want to compose or chain multiple resources without nested try-with-resources blocks.
 */
public class ResourceSample {

    // Simulated DB connection
    static class Connection {
        final String url;
        boolean closed = false;

        Connection(String url) {
            this.url = url;
            System.out.println("Connection opened: " + url);
        }

        void close() {
            closed = true;
            System.out.println("Connection closed: " + url);
        }

        String query(String sql) {
            return "result[" + sql + "@" + url + "]";
        }
    }

    // Simulated prepared statement
    static class Statement {
        final String sql;
        boolean closed = false;

        Statement(Connection c, String sql) {
            this.sql = sql;
            System.out.println("Statement prepared: " + sql);
        }

        void close() {
            closed = true;
            System.out.println("Statement closed: " + sql);
        }

        List<String> execute() {
            return List.of("row1", "row2");
        }
    }

    static void main() {

        // ---- Basic use — of / use ----

        System.out.println("=== Basic use ===");

        var connResource = Resource.of(
            () -> new Connection("jdbc:h2:mem:sample"),
            Connection::close
        );

        var result = connResource.use(
            conn -> conn.query("SELECT 1")
        );
        System.out.println("Query result: " + result.getOrElse("none")); // result[SELECT 1@jdbc:h2:mem:sample]
        // Connection is closed here even if query threw

        // ---- fromAutoCloseable ----

        System.out.println("\n=== fromAutoCloseable ===");

        var readerClosed = new AtomicBoolean(false);
        AutoCloseable fakeReader = () -> {
            readerClosed.set(true);
            System.out.println("Reader closed");
        };
        var readerResource = Resource.fromAutoCloseable(() -> fakeReader);

        readerResource.use(_ -> "ok");
        System.out.println("Reader closed after use: " + readerClosed.get()); // true

        // ---- map — transform the resource value ----

        System.out.println("\n=== map ===");

        // Transform Connection → its URL string; the underlying Connection is still acquired/released
        var urlResource = connResource.map(c -> c.url.toUpperCase());
        var urlLength = urlResource.use(String::length);
        System.out.println("URL length: " + urlLength.getOrElse(-1)); // 22
        // Connection opened and closed automatically

        // ---- flatMap — sequence two resources ----

        System.out.println("\n=== flatMap (composed resources) ===");

        var queryResource = connResource.flatMap(conn ->
            Resource.of(
                () -> new Statement(conn, "SELECT * FROM users"),
                Statement::close
            )
        ).map(Statement::execute);

        // Acquire order:  Connection → Statement
        // Release order:  Statement  → Connection  (reverse)
        var rowCount = queryResource.use(rows -> {
            System.out.println("Rows: " + rows);
            return rows.size();
        });
        System.out.println("Row count: " + rowCount.getOrElse(-1)); // 2

        // ---- Release always runs, even when body throws ----

        System.out.println("\n=== guaranteed release on failure ===");

        var events = new ArrayList<>();
        var tracked = Resource.of(
            () -> {
                events.add("acquire");
                return "value";
            },
            _ -> events.add("release")
        );

        var failed = tracked.use(_ -> {
            throw new RuntimeException("oops");
        });
        System.out.println("Failed: " + failed.isFailure());                  // true
        System.out.println("Released anyway: " + events.contains("release")); // true

        // ---- Exception suppression — body wins, release suppressed ----

        System.out.println("\n=== exception suppression ===");

        var bodyEx = new RuntimeException("body error");
        var releaseEx = new RuntimeException("release error");
        var bothThrow = Resource.of(
            () -> "res",
            _ -> {
                throw releaseEx;
            }
        );

        var bothFailed = bothThrow.use(_ -> {
            throw bodyEx;
        });
        var cause = bothFailed.getCause();
        System.out.println("Primary cause: " + cause.getMessage());                       // body error
        System.out.println("Suppressed cause: " + cause.getSuppressed()[0].getMessage()); // release error

        // ---- Reusability ----

        System.out.println("\n=== reusability ===");

        // Each call to use() independently acquires and releases
        connResource.use(conn -> conn.query("SELECT 'first'"));
        connResource.use(conn -> conn.query("SELECT 'second'"));
        // Two connections opened and closed
    }
}
