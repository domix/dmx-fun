package dmx.fun.quarkus.it;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class TransactionIntegrationTest {

    static final long ID = 1L;

    @Inject
    CounterService service;

    @BeforeEach
    void setUp() throws Exception {
        service.createTable();
        service.upsert(ID, 0L);
    }

    // ── TxResult.execute() ────────────────────────────────────────────────────

    @Test
    void txResult_okResult_commitsToDb() throws Exception {
        service.incrementExecuteOk(ID);
        assertThat(service.getValue(ID)).isEqualTo(1L);
    }

    @Test
    void txResult_errResult_rollsBack() throws Exception {
        service.incrementExecuteErr(ID);
        assertThat(service.getValue(ID)).isEqualTo(0L);
    }

    // ── TxTry.execute() ───────────────────────────────────────────────────────

    @Test
    void txTry_successTry_commitsToDb() throws Exception {
        service.incrementTryExecuteOk(ID);
        assertThat(service.getValue(ID)).isEqualTo(1L);
    }

    @Test
    void txTry_failureTry_rollsBack() throws Exception {
        service.incrementTryExecuteErr(ID);
        assertThat(service.getValue(ID)).isEqualTo(0L);
    }

    // ── @TransactionalResult declarative ─────────────────────────────────────

    @Test
    void transactionalResult_okResult_commitsToDb() throws Exception {
        service.incrementDeclarativeResultOk(ID);
        assertThat(service.getValue(ID)).isEqualTo(1L);
    }

    @Test
    void transactionalResult_errResult_rollsBack() throws Exception {
        service.incrementDeclarativeResultErr(ID);
        assertThat(service.getValue(ID)).isEqualTo(0L);
    }

    // ── @TransactionalTry declarative ────────────────────────────────────────

    @Test
    void transactionalTry_successTry_commitsToDb() throws Exception {
        service.incrementDeclarativeTryOk(ID);
        assertThat(service.getValue(ID)).isEqualTo(1L);
    }

    @Test
    void transactionalTry_failureTry_rollsBack() throws Exception {
        service.incrementDeclarativeTryErr(ID);
        assertThat(service.getValue(ID)).isEqualTo(0L);
    }

    // ── TxResult.executeNew() — REQUIRES_NEW semantics ────────────────────────

    @Test
    void executeNew_innerTxCommits_evenWhenOuterRollsBack() throws Exception {
        // outer tx returns Err (rolls back) but the inner executeNew already committed
        var result = service.outerErrInnerNewOk(ID);
        assertThat(result).isErr();
        assertThat(service.getValue(ID)).isEqualTo(1L);
    }
}
