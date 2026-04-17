package dmx.fun.spring;

import dmx.fun.Validated;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TxValidatedTest {

    EmbeddedDatabase db;
    JdbcTemplate jdbc;
    TxValidated txValidated;

    @BeforeEach
    void setUp() {
        db = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .generateUniqueName(true)
            .build();
        jdbc = new JdbcTemplate(db);
        jdbc.execute("CREATE TABLE events (id INT PRIMARY KEY, label VARCHAR(255))");
        txValidated = new TxValidated(new DataSourceTransactionManager(db));
    }

    @AfterEach
    void tearDown() {
        db.shutdown();
    }

    private int countRows() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
        return count != null ? count : 0;
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    @Test
    void constructor_shouldThrowNPE_whenTxManagerIsNull() {
        assertThatThrownBy(() -> new TxValidated(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("txManager");
    }

    // -------------------------------------------------------------------------
    // execute(Supplier)
    // -------------------------------------------------------------------------

    @Test
    void execute_onValid_commits() {
        Validated<String, Integer> result = txValidated.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (1, 'test')");
            return Validated.valid(1);
        });

        assertThat(result.isValid()).isTrue();
        assertThat(result.get()).isEqualTo(1);
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void execute_onInvalid_rollsBack() {
        Validated<String, Integer> result = txValidated.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (2, 'test')");
            return Validated.invalid("validation failed");
        });

        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getError()).isEqualTo("validation failed");
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_onException_rollsBackAndPropagates() {
        assertThatThrownBy(() -> txValidated.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (3, 'test')");
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class)
           .hasMessage("boom");

        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_shouldThrowNPE_whenActionIsNull() {
        assertThatThrownBy(() -> txValidated.execute(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    @Test
    void execute_shouldThrowNPE_whenActionReturnsNull() {
        assertThatThrownBy(() -> txValidated.execute(() -> null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // execute(TransactionDefinition, Supplier)
    // -------------------------------------------------------------------------

    @Test
    void executeWithDef_onValid_commits() {
        var def = new DefaultTransactionDefinition();
        Validated<String, Integer> result = txValidated.execute(def, () -> {
            jdbc.update("INSERT INTO events VALUES (4, 'def')");
            return Validated.valid(4);
        });

        assertThat(result.isValid()).isTrue();
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void executeWithDef_onInvalid_rollsBack() {
        var def = new DefaultTransactionDefinition();
        Validated<String, Integer> result = txValidated.execute(def, () -> {
            jdbc.update("INSERT INTO events VALUES (5, 'def')");
            return Validated.invalid("rollback me");
        });

        assertThat(result.isInvalid()).isTrue();
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void executeWithDef_shouldThrowNPE_whenDefIsNull() {
        assertThatThrownBy(() -> txValidated.execute(null, () -> Validated.valid(1)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("def");
    }

    @Test
    void executeWithDef_shouldThrowNPE_whenActionIsNull() {
        assertThatThrownBy(() -> txValidated.execute(new DefaultTransactionDefinition(), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    // -------------------------------------------------------------------------
    // Multiple steps
    // -------------------------------------------------------------------------

    @Test
    void execute_multipleInserts_allRollBackOnInvalid() {
        Validated<String, Integer> result = txValidated.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (10, 'a')");
            jdbc.update("INSERT INTO events VALUES (11, 'b')");
            return Validated.invalid("partial work");
        });

        assertThat(result.isInvalid()).isTrue();
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void execute_multipleInserts_commitAll() {
        Validated<String, Integer> result = txValidated.execute(() -> {
            jdbc.update("INSERT INTO events VALUES (20, 'x')");
            jdbc.update("INSERT INTO events VALUES (21, 'y')");
            return Validated.valid(2);
        });

        assertThat(result.isValid()).isTrue();
        assertThat(countRows()).isEqualTo(2);
    }
}
