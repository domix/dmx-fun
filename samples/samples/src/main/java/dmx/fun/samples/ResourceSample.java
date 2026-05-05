package dmx.fun.samples;

import dmx.fun.Resource;
import dmx.fun.Result;
import dmx.fun.Try;
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

    sealed interface DbError permits DbError.QueryFailed {
        record QueryFailed(String message) implements DbError {}
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

        List<String> events = new ArrayList<>();
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

        // ---- eval — factory from a pre-computed Try ----

        System.out.println("\n=== eval ===");

        // Acquisition already happened; lift the result into a Resource
        var tryConn = Try.of(() -> new Connection("jdbc:h2:mem:eval"));
        var evalResource = Resource.eval(tryConn, Connection::close);
        var evalResult = evalResource.use(conn -> conn.query("SELECT 'eval'"));
        System.out.println("eval result: " + evalResult.getOrElse("none")); // result[SELECT 'eval'@...]

        // If the Try was a failure, release is never called
        var failedTry = Try.<Connection>failure(new RuntimeException("could not open"));
        var failedEval = Resource.eval(failedTry, Connection::close);
        var failedResult = failedEval.use(conn -> conn.query("SELECT 1"));
        System.out.println("eval failure: " + failedResult.isFailure()); // true — release not called

        // ---- useAsResult — Result-integrated execution ----

        System.out.println("\n=== useAsResult ===");

        // Body returns Result<R, E> — infrastructure Throwables are mapped to E via onError
        Result<String, DbError> queryResult = connResource.useAsResult(
            conn  -> Result.ok(conn.query("SELECT 'hello'")),
            ex    -> new DbError.QueryFailed(ex.getMessage())
        );
        System.out.println("useAsResult ok: " + queryResult.isSuccess()); // true
        System.out.println("value: " + queryResult.getOrElse("none"));    // result[SELECT 'hello'@...]

        // Domain-level error (Err returned by body) passes through as-is
        Result<String, DbError> domainErr = connResource.useAsResult(
            _     -> Result.err(new DbError.QueryFailed("table not found")),
            ex    -> new DbError.QueryFailed(ex.getMessage())
        );
        System.out.println("useAsResult err: " + domainErr.isError());    // true
        System.out.println("error: " + domainErr.getError());             // QueryFailed[table not found]

        // ---- mapTry — transform resource value with a Try-returning function ----

        System.out.println("\n=== mapTry ===");

        // Parse the query result string as JSON-like integer — simulate a fallible transform
        var rawResource = Resource.of(() -> "42", s -> {});
        var parsedResource = rawResource.mapTry(s -> Try.of(() -> Integer.parseInt(s)));
        var parsedResult = parsedResource.use(n -> n * 2);
        System.out.println("mapTry success: " + parsedResult.getOrElse(-1)); // 84

        // If fn returns a failure the resource is still released
        var badResource = Resource.of(() -> "not-a-number", s -> System.out.println("released"));
        var badParsed = badResource.mapTry(s -> Try.of(() -> Integer.parseInt(s)));
        var badResult = badParsed.use(n -> n * 2);
        System.out.println("mapTry failure: " + badResult.isFailure()); // true (released was printed)
    }
}
