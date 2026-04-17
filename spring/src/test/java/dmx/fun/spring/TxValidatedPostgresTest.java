package dmx.fun.spring;

import dmx.fun.Validated;
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
 * Validates {@link TxValidated} commit/rollback behaviour against a real PostgreSQL database
 * via Testcontainers. Tests are skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class TxValidatedPostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcTemplate jdbc;
    static TxValidated txValidated;

    @BeforeAll
    static void setUpOnce() {
        var dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS events (id INT PRIMARY KEY, label VARCHAR(255))");
        txValidated = new TxValidated(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void truncate() {
        jdbc.execute("TRUNCATE TABLE events");
    }

    private int countRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class).intValue();
    }

    @Test
    void execute_onValid_commits() {
        Validated<String, Integer> result = txValidated.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (1, 'pg-test')");
            return Validated.valid(1);
        });

        assertThat(result.isValid()).isTrue();
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void execute_onInvalid_rollsBack() {
        Validated<String, Integer> result = txValidated.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (2, 'pg-test')");
            return Validated.invalid("validation error");
        });

        assertThat(result.isInvalid()).isTrue();
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_onException_rollsBackAndPropagates() {
        assertThatThrownBy(() -> txValidated.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (3, 'pg-test')");
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class)
           .hasMessage("boom");

        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_withExplicitDef_onInvalid_rollsBack() {
        var def = new DefaultTransactionDefinition();
        Validated<String, Integer> result = txValidated.execute(def, () -> {
            jdbc.update("INSERT INTO events VALUES (4, 'pg-def')");
            return Validated.invalid("rollback");
        });

        assertThat(result.isInvalid()).isTrue();
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_multipleInserts_allRollBackOnInvalid() {
        Validated<String, Integer> result = txValidated.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (10, 'a')");
            jdbc.update("INSERT INTO events VALUES (11, 'b')");
            return Validated.invalid("partial");
        });

        assertThat(result.isInvalid()).isTrue();
        assertThat(countRows()).isEqualTo(0);
    }
}
