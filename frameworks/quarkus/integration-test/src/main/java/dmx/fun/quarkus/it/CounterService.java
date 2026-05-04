package dmx.fun.quarkus.it;

import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.quarkus.TransactionalResult;
import dmx.fun.quarkus.TransactionalTry;
import dmx.fun.quarkus.TxResult;
import dmx.fun.quarkus.TxTry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.sql.SQLException;
import javax.sql.DataSource;

@ApplicationScoped
public class CounterService {

    @Inject
    DataSource dataSource;

    @Inject
    TxResult txResult;

    @Inject
    TxTry txTry;

    /** Self-reference through the CDI proxy so interceptors fire on internal calls. */
    @Inject
    CounterService self;

    public void createTable() throws SQLException {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS counter (id BIGINT PRIMARY KEY, val BIGINT NOT NULL DEFAULT 0)"
            );
        }
    }

    public void upsert(long id, long val) throws SQLException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO counter (id, val) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET val = EXCLUDED.val"
             )) {
            ps.setLong(1, id);
            ps.setLong(2, val);
            ps.executeUpdate();
        }
    }

    public long getValue(long id) throws SQLException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT val FROM counter WHERE id = ?"
             )) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("No row found for id=" + id);
            }
        }
    }

    private long getAndIncrement(long id) throws SQLException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(
                 "UPDATE counter SET val = val + 1 WHERE id = ? RETURNING val"
             )) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("No row found for id=" + id);
            }
        }
    }

    private Result<Long, String> increment(long id) {
        try {
            return Result.ok(getAndIncrement(id));
        } catch (SQLException e) {
            return Result.err(e.getMessage());
        }
    }

    // ── Programmatic API ──────────────────────────────────────────────────────

    public Result<Long, String> incrementExecuteOk(long id) {
        return txResult.execute(() -> increment(id));
    }

    public Result<Long, String> incrementExecuteErr(long id) {
        return txResult.execute(
            () -> increment(id)
                .flatMap(_ -> Result.err("forced error"))
        );
    }

    public Try<Long> incrementTryExecuteOk(long id) {
        return txTry.execute(() -> Try.of(() -> getAndIncrement(id)));
    }

    public Try<Long> incrementTryExecuteErr(long id) {
        return txTry.execute(() ->
            Try.of(() -> getAndIncrement(id))
               .flatMap(_ -> Try.failure(new RuntimeException("forced failure")))
        );
    }

    // ── @TransactionalResult — REQUIRED (default) ─────────────────────────────

    @TransactionalResult
    public Result<Long, String> incrementDeclarativeResultOk(long id) {
        return increment(id);
    }

    @TransactionalResult
    public Result<Long, String> incrementDeclarativeResultErr(long id) {
        return increment(id).flatMap(_ -> Result.err("declarative error"));
    }

    // ── @TransactionalTry — REQUIRED (default) ────────────────────────────────

    @TransactionalTry
    public Try<Long> incrementDeclarativeTryOk(long id) {
        return Try.of(() -> getAndIncrement(id));
    }

    @TransactionalTry
    public Try<Long> incrementDeclarativeTryErr(long id) {
        return Try.of(() -> getAndIncrement(id))
                  .flatMap(_ -> Try.failure(new RuntimeException("declarative failure")));
    }

    // ── TxResult.executeNew() — REQUIRES_NEW (programmatic) ───────────────────

    public Result<Long, String> outerErrInnerNewOk(long id) {
        return txResult.execute(() -> {
            txResult.executeNew(() -> increment(id));
            return Result.err("outer forced error");
        });
    }

    // ── @TransactionalResult(REQUIRES_NEW) ────────────────────────────────────

    /**
     * Increments and returns ok — annotated with REQUIRES_NEW so the outer tx is
     * always suspended and a fresh tx is started.
     */
    @TransactionalResult(Transactional.TxType.REQUIRES_NEW)
    public Result<Long, String> incrementDeclarativeResultRequiresNewOk(long id) {
        return increment(id);
    }

    /**
     * Outer tx (from txResult.execute) wraps an inner REQUIRES_NEW call.
     * Even though the outer returns Err (rollback), the inner tx already committed.
     */
    public Result<Long, String> outerErrInnerRequiresNewOk(long id) {
        return txResult.execute(() -> {
            self.incrementDeclarativeResultRequiresNewOk(id);
            return Result.err("outer forced error");
        });
    }

    // ── @TransactionalResult — REQUIRED join (via self-call) ──────────────────

    /**
     * Outer tx (from txResult.execute) wraps an inner REQUIRED call that returns Ok.
     * The inner REQUIRED method joins the outer tx. When the outer tx rolls back,
     * the joined inner changes are rolled back too.
     */
    public Result<Long, String> outerErrInnerRequiredOk(long id) {
        return txResult.execute(() -> {
            self.incrementDeclarativeResultOk(id); // REQUIRED join — same tx as outer
            return Result.err("outer forced error");
        });
    }

    // ── @TransactionalResult(MANDATORY) ──────────────────────────────────────

    /** Requires an existing transaction; throws TransactionalException if none. */
    @TransactionalResult(Transactional.TxType.MANDATORY)
    public Result<Long, String> incrementDeclarativeResultMandatory(long id) {
        return increment(id);
    }

    /**
     * Outer tx (from txResult.execute) wraps a MANDATORY inner call.
     * The inner MANDATORY method joins the outer tx; when the outer commits, the increment
     * is persisted.
     */
    public Result<Long, String> outerOkInnerMandatory(long id) {
        return txResult.execute(() -> self.incrementDeclarativeResultMandatory(id));
    }

    // ── @TransactionalResult(NOT_SUPPORTED) ──────────────────────────────────

    /**
     * Suspends any active transaction and runs without one (auto-commit semantics).
     * Changes are visible immediately after the method returns, regardless of any outer tx.
     */
    @TransactionalResult(Transactional.TxType.NOT_SUPPORTED)
    public Result<Long, String> incrementDeclarativeResultNotSupported(long id) {
        return increment(id);
    }

    /**
     * Outer tx (from txResult.execute) wraps a NOT_SUPPORTED inner call.
     * The inner call suspends the outer tx, increments (auto-commits), then resumes the outer.
     * Even when the outer rolls back, the inner increment persists.
     */
    public Result<Long, String> outerErrInnerNotSupported(long id) {
        return txResult.execute(() -> {
            self.incrementDeclarativeResultNotSupported(id);
            return Result.err("outer forced error");
        });
    }

    // ── @TransactionalResult(NEVER) ──────────────────────────────────────────

    /** Throws TransactionalException if called with an active transaction. */
    @TransactionalResult(Transactional.TxType.NEVER)
    public Result<Long, String> incrementDeclarativeResultNever(long id) {
        return increment(id);
    }

    /**
     * Outer tx (from txResult.execute) wraps a NEVER inner call.
     * The inner NEVER method throws TransactionalException because an active tx is present;
     * the outer tx rolls back and the exception propagates to the caller.
     */
    public Result<Long, String> outerWithInnerNever(long id) {
        return txResult.execute(() -> {
            self.incrementDeclarativeResultNever(id); // throws TransactionalException
            return Result.ok(0L);
        });
    }
}
