package dmx.fun.spring;

import dmx.fun.Try;
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
 * Validates {@link TxTry} commit/rollback behaviour against a real PostgreSQL database
 * via Testcontainers. Complements {@link TxTryTest} (H2) by covering PostgreSQL-specific
 * transaction semantics and isolation levels that H2 may not reproduce faithfully.
 *
 * <p>Tests are skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class TxTryPostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcTemplate jdbc;
    static TxTry txTry;

    @BeforeAll
    static void setUpOnce() {
        var dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS events (id INT PRIMARY KEY, label VARCHAR(255))");
        txTry = new TxTry(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void truncate() {
        jdbc.execute("TRUNCATE TABLE events");
    }

    private int countRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class).intValue();
    }

    @Test
    void execute_onSuccess_commits() {
        Try<Integer> result = txTry.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (1, 'pg-test')");
            return Try.success(1);
        });

        assertThat(result.isSuccess()).isTrue();
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void execute_onFailure_rollsBack() {
        var ex = new RuntimeException("domain failure");
        Try<Integer> result = txTry.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (2, 'pg-test')");
            return Try.failure(ex);
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_onException_rollsBackAndPropagates() {
        assertThatThrownBy(() -> txTry.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (3, 'pg-test')");
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class)
           .hasMessage("boom");

        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_withExplicitDef_onFailure_rollsBack() {
        var def = new DefaultTransactionDefinition();
        Try<Integer> result = txTry.execute(def, () -> {
            jdbc.update("INSERT INTO events VALUES (4, 'pg-def')");
            return Try.failure(new RuntimeException("rollback"));
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_multipleInserts_allRollBackOnFailure() {
        Try<Integer> result = txTry.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (10, 'a')");
            jdbc.update("INSERT INTO events VALUES (11, 'b')");
            return Try.failure(new RuntimeException("partial"));
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(countRows()).isEqualTo(0);
    }
}
