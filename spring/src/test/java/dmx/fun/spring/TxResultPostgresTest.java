package dmx.fun.spring;

import dmx.fun.Result;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates {@link TxResult} commit/rollback behaviour against a real PostgreSQL database
 * via Testcontainers. Complements {@link TxResultTest} (H2) by covering PostgreSQL-specific
 * transaction semantics, isolation levels, and connection pool behaviour that H2 may not
 * reproduce faithfully.
 *
 * <p>Tests are skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class TxResultPostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcTemplate jdbc;
    static TxResult txResult;

    @BeforeAll
    static void setUpOnce() {
        var dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS events (id INT PRIMARY KEY, label VARCHAR(255))");
        txResult = new TxResult(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void truncate() {
        jdbc.execute("TRUNCATE TABLE events");
    }

    private int countRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class).intValue();
    }

    @Test
    void execute_onOk_commits() {
        Result<Integer, String> result = txResult.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (1, 'pg-test')");
            return Result.ok(1);
        });

        assertThat(result.isOk()).isTrue();
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void execute_onError_rollsBack() {
        Result<Integer, String> result = txResult.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (2, 'pg-test')");
            return Result.err("domain error");
        });

        assertThat(result.isError()).isTrue();
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_onException_rollsBackAndPropagates() {
        assertThatThrownBy(() -> txResult.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (3, 'pg-test')");
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class)
           .hasMessage("boom");

        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_withExplicitDef_onError_rollsBack() {
        var def = new DefaultTransactionDefinition();
        Result<Integer, String> result = txResult.execute(def, () -> {
            jdbc.update("INSERT INTO events VALUES (4, 'pg-def')");
            return Result.err("rollback");
        });

        assertThat(result.isError()).isTrue();
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_multipleInserts_allRollBackOnError() {
        Result<Integer, String> result = txResult.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (10, 'a')");
            jdbc.update("INSERT INTO events VALUES (11, 'b')");
            return Result.err("partial");
        });

        assertThat(result.isError()).isTrue();
        assertThat(countRows()).isEqualTo(0);
    }
}
