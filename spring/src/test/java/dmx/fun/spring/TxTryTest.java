package dmx.fun.spring;

import dmx.fun.Try;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import org.assertj.core.api.Assertions;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TxTryTest {

    EmbeddedDatabase db;
    JdbcTemplate jdbc;
    TxTry txTry;

    @BeforeEach
    void setUp() {
        db = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .generateUniqueName(true)
            .build();
        jdbc = new JdbcTemplate(db);
        jdbc.execute("CREATE TABLE events (id INT PRIMARY KEY, label VARCHAR(255))");
        var txManager = new DataSourceTransactionManager(db);
        txTry = new TxTry(txManager);
    }

    @AfterEach
    void tearDown() {
        db.shutdown();
    }

    private int countRows() {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
        return count != null ? count : 0;
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    @Test
    void constructor_shouldThrowNPE_whenTxManagerIsNull() {
        assertThatThrownBy(() -> new TxTry(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("txManager");
    }

    // -------------------------------------------------------------------------
    // execute(Supplier)
    // -------------------------------------------------------------------------

    @Test
    void execute_onSuccess_commits() {
        var result = txTry.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (1, 'test')");
            return Try.success(1);
        });

        assertThat(result).isSuccess().containsValue(1);
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void execute_onFailure_rollsBack() {
        var ex = new RuntimeException("domain failure");
        var result = txTry.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (2, 'test')");
            return Try.failure(ex);
        });

        assertThat(result).isFailure();
        Assertions.assertThat(result.getCause()).isSameAs(ex);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_onException_rollsBackAndPropagates() {
        assertThatThrownBy(() -> txTry.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (3, 'test')");
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class)
           .hasMessage("boom");

        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_shouldThrowNPE_whenActionIsNull() {
        assertThatThrownBy(() -> txTry.execute(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    @Test
    void execute_shouldThrowNPE_whenActionReturnsNull() {
        assertThatThrownBy(() -> txTry.execute(() -> null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // execute(TransactionDefinition, Supplier)
    // -------------------------------------------------------------------------

    @Test
    void executeWithDef_onSuccess_commits() {
        var def = new DefaultTransactionDefinition();
        var result = txTry.execute(def, () -> {
            jdbc.update("INSERT INTO events VALUES (4, 'def')");
            return Try.success(4);
        });

        assertThat(result).isSuccess();
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void executeWithDef_onFailure_rollsBack() {
        var def = new DefaultTransactionDefinition();
        var result = txTry.execute(def, () -> {
            jdbc.update("INSERT INTO events VALUES (5, 'def')");
            return Try.failure(new RuntimeException("rollback"));
        });

        assertThat(result).isFailure();
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void executeWithDef_shouldThrowNPE_whenDefIsNull() {
        assertThatThrownBy(() -> txTry.execute(null, () -> Try.success(1)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("def");
    }

    @Test
    void executeWithDef_shouldThrowNPE_whenActionIsNull() {
        assertThatThrownBy(() -> txTry.execute(new DefaultTransactionDefinition(), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    // -------------------------------------------------------------------------
    // Multiple steps
    // -------------------------------------------------------------------------

    @Test
    void execute_multipleInserts_allRollBackOnFailure() {
        var result = txTry.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (10, 'a')");
            jdbc.update("INSERT INTO events VALUES (11, 'b')");
            return Try.failure(new RuntimeException("partial work"));
        });

        assertThat(result).isFailure();
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_multipleInserts_commitAll() {
        var result = txTry.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (20, 'x')");
            jdbc.update("INSERT INTO events VALUES (21, 'y')");
            return Try.success(2);
        });

        assertThat(result).isSuccess();
        Assertions.assertThat(countRows()).isEqualTo(2);
    }
}
