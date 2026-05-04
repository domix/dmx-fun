package dmx.fun.quarkus.it;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // ── @TransactionalResult — REQUIRED ──────────────────────────────────────

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

    // ── @TransactionalTry — REQUIRED ─────────────────────────────────────────

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

    // ── TxResult.executeNew() — REQUIRES_NEW (programmatic) ──────────────────

    @Test
    void executeNew_innerTxCommits_evenWhenOuterRollsBack() throws Exception {
        var result = service.outerErrInnerNewOk(ID);
        assertThat(result).isErr();
        assertThat(service.getValue(ID)).isEqualTo(1L);
    }

    // ── @TransactionalResult(REQUIRES_NEW) ────────────────────────────────────

    @Test
    void requiresNew_innerTxCommits_evenWhenOuterRollsBack() throws Exception {
        // inner @TransactionalResult(REQUIRES_NEW) suspends the outer tx and commits
        // independently; the outer rolls back but cannot undo the inner commit
        var result = service.outerErrInnerRequiresNewOk(ID);
        assertThat(result).isErr();
        assertThat(service.getValue(ID)).isEqualTo(1L);
    }

    // ── @TransactionalResult — REQUIRED join ─────────────────────────────────

    @Test
    void required_innerJoinsOuterTx_rollsBackWithOuter() throws Exception {
        // inner @TransactionalResult(REQUIRED) joins the outer txResult.execute tx;
        // when the outer rolls back, the inner's increment is rolled back too
        var result = service.outerErrInnerRequiredOk(ID);
        assertThat(result).isErr();
        assertThat(service.getValue(ID)).isEqualTo(0L);
    }

    // ── @TransactionalResult(MANDATORY) ──────────────────────────────────────

    @Test
    void mandatory_withoutActiveTx_throwsTransactionalException() throws Exception {
        // No outer transaction → MANDATORY must throw
        assertThatThrownBy(() -> service.incrementDeclarativeResultMandatory(ID))
            .isInstanceOf(TransactionalException.class);
        // DB must be unchanged
        assertThat(service.getValue(ID)).isEqualTo(0L);
    }

    // ── @TransactionalResult(NOT_SUPPORTED) ──────────────────────────────────

    @Test
    void notSupported_innerRunsWithoutTx_committedEvenWhenOuterRollsBack() throws Exception {
        // inner @TransactionalResult(NOT_SUPPORTED) suspends the outer tx and runs
        // without one (auto-commit); the outer tx rolls back but cannot touch the
        // already-committed inner increment
        var result = service.outerErrInnerNotSupported(ID);
        assertThat(result).isErr();
        assertThat(service.getValue(ID)).isEqualTo(1L);
    }

    // ── @TransactionalResult(NEVER) ──────────────────────────────────────────

    @Test
    void never_withoutActiveTx_executesNormally() throws Exception {
        // No outer transaction → NEVER proceeds normally
        var result = service.incrementDeclarativeResultNever(ID);
        assertThat(result).isOk();
        assertThat(service.getValue(ID)).isEqualTo(1L);
    }
}
