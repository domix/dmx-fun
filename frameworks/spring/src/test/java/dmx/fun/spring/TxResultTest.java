package dmx.fun.spring;

import dmx.fun.Result;
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

class TxResultTest {

    EmbeddedDatabase db;
    JdbcTemplate jdbc;
    TxResult txResult;

    @BeforeEach
    void setUp() {
        db = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .generateUniqueName(true)
            .build();
        jdbc = new JdbcTemplate(db);
        jdbc.execute("CREATE TABLE events (id INT PRIMARY KEY, label VARCHAR(255))");
        var txManager = new DataSourceTransactionManager(db);
        txResult = new TxResult(txManager);
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
        assertThatThrownBy(() -> new TxResult(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("txManager");
    }

    // -------------------------------------------------------------------------
    // execute(Supplier)
    // -------------------------------------------------------------------------

    @Test
    void execute_onOk_commits() {
        var result = txResult.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (1, 'test')");
            return Result.ok(1);
        });

        assertThat(result).isOk().containsValue(1);
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void execute_onError_rollsBack() {
        var result = txResult.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (2, 'test')");
            return Result.err("domain error");
        });

        assertThat(result).isErr().containsError("domain error");
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_onException_rollsBackAndPropagates() {
        assertThatThrownBy(() -> txResult.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (3, 'test')");
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class)
           .hasMessage("boom");

        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_shouldThrowNPE_whenActionIsNull() {
        assertThatThrownBy(() -> txResult.execute(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    @Test
    void execute_shouldThrowNPE_whenActionReturnsNull() {
        assertThatThrownBy(() -> txResult.execute(() -> null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // execute(TransactionDefinition, Supplier)
    // -------------------------------------------------------------------------

    @Test
    void executeWithDef_onOk_commits() {
        var def = new DefaultTransactionDefinition();
        var result = txResult.execute(def, () -> {
            jdbc.update("INSERT INTO events VALUES (4, 'def')");
            return Result.ok(4);
        });

        assertThat(result).isOk();
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void executeWithDef_onError_rollsBack() {
        var def = new DefaultTransactionDefinition();
        var result = txResult.execute(def, () -> {
            jdbc.update("INSERT INTO events VALUES (5, 'def')");
            return Result.err("rollback me");
        });

        assertThat(result).isErr();
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void executeWithDef_shouldThrowNPE_whenDefIsNull() {
        assertThatThrownBy(() -> txResult.execute(null, () -> Result.ok(1)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("def");
    }

    @Test
    void executeWithDef_shouldThrowNPE_whenActionIsNull() {
        assertThatThrownBy(() -> txResult.execute(new DefaultTransactionDefinition(), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    // -------------------------------------------------------------------------
    // Multiple steps
    // -------------------------------------------------------------------------

    @Test
    void execute_multipleInserts_allRollBackOnError() {
        var result = txResult.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (10, 'a')");
            jdbc.update("INSERT INTO events VALUES (11, 'b')");
            return Result.err("partial work");
        });

        assertThat(result).isErr();
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_multipleInserts_commitAll() {
        var result = txResult.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (20, 'x')");
            jdbc.update("INSERT INTO events VALUES (21, 'y')");
            return Result.ok(2);
        });

        assertThat(result).isOk();
        Assertions.assertThat(countRows()).isEqualTo(2);
    }
}
