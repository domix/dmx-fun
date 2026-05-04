package dmx.fun.quarkus.it;

import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.quarkus.TransactionalResult;
import dmx.fun.quarkus.TransactionalTry;
import dmx.fun.quarkus.TxResult;
import dmx.fun.quarkus.TxTry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

@ApplicationScoped
public class CounterService {

    @Inject
    DataSource dataSource;

    @Inject
    TxResult txResult;

    @Inject
    TxTry txTry;

    public void createTable() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS counter (id BIGINT PRIMARY KEY, val BIGINT NOT NULL DEFAULT 0)"
            );
        }
    }

    public void upsert(long id, long val) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO counter (id, val) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET val = EXCLUDED.val"
             )) {
            ps.setLong(1, id);
            ps.setLong(2, val);
            ps.executeUpdate();
        }
    }

    public long getValue(long id) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT val FROM counter WHERE id = ?"
             )) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("No row found for id=" + id);
            }
        }
    }

    private long getAndIncrement(long id) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE counter SET val = val + 1 WHERE id = ? RETURNING val"
             )) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
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

    public Result<Long, String> incrementExecuteOk(long id) {
        return txResult.execute(() -> increment(id));
    }

    public Result<Long, String> incrementExecuteErr(long id) {
        return txResult.execute(() -> increment(id).flatMap(ignored -> Result.err("forced error")));
    }

    public Try<Long> incrementTryExecuteOk(long id) {
        return txTry.execute(() -> Try.of(() -> getAndIncrement(id)));
    }

    public Try<Long> incrementTryExecuteErr(long id) {
        return txTry.execute(() ->
            Try.of(() -> getAndIncrement(id))
               .flatMap(ignored -> Try.failure(new RuntimeException("forced failure")))
        );
    }

    @TransactionalResult
    public Result<Long, String> incrementDeclarativeResultOk(long id) {
        return increment(id);
    }

    @TransactionalResult
    public Result<Long, String> incrementDeclarativeResultErr(long id) {
        return increment(id).flatMap(ignored -> Result.err("declarative error"));
    }

    @TransactionalTry
    public Try<Long> incrementDeclarativeTryOk(long id) {
        return Try.of(() -> getAndIncrement(id));
    }

    @TransactionalTry
    public Try<Long> incrementDeclarativeTryErr(long id) {
        return Try.of(() -> getAndIncrement(id))
                  .flatMap(ignored -> Try.failure(new RuntimeException("declarative failure")));
    }

    public Result<Long, String> outerErrInnerNewOk(long id) {
        return txResult.execute(() -> {
            txResult.executeNew(() -> increment(id));
            return Result.err("outer forced error");
        });
    }
}
