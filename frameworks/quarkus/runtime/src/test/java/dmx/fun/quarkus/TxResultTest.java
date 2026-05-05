package dmx.fun.quarkus;

import dmx.fun.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TxResultTest {

    StubTransactionManager tx;
    TxResult txResult;

    @BeforeEach
    void setUp() {
        tx = new StubTransactionManager();
        txResult = new TxResult(tx);
    }

    // ── construction ──────────────────────────────────────────────────────────

    @Test
    void constructor_nullTransactionManager_throwsNPE() {
        assertThatThrownBy(() -> new TxResult(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("transactionManager");
    }

    // ── execute — commit path ─────────────────────────────────────────────────

    @Test
    void execute_okResult_commits() {
        var result = txResult.execute(() -> Result.ok("value"));

        assertThat(result).isOk().containsValue("value");
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    // ── execute — rollback path ───────────────────────────────────────────────

    @Test
    void execute_errResult_rollsBack() {
        var result = txResult.execute(() -> Result.err("domain error"));

        assertThat(result).isErr().containsError("domain error");
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    @Test
    void execute_actionThrows_rollsBackAndRethrows() {
        var boom = new RuntimeException("boom");

        assertThatThrownBy(() -> txResult.execute(() -> { throw boom; }))
            .isSameAs(boom);

        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    // ── execute — null-argument guards ────────────────────────────────────────

    @Test
    void execute_nullAction_throwsNPE() {
        assertThatThrownBy(() -> txResult.execute(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    @Test
    void execute_actionReturnsNull_throwsNPEAndRollsBack() {
        assertThatThrownBy(() -> txResult.execute(() -> null))
            .isInstanceOf(NullPointerException.class);

        assertThat(tx.rollbackCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
    }

    // ── CDI proxy no-arg constructor ──────────────────────────────────────────

    @Test
    void noArgConstructor_executeWithValidAction_throwsNPEFromNullExecutor() {
        var proxy = new TxResult();
        assertThatThrownBy(() -> proxy.execute(() -> Result.ok("x")))
            .isInstanceOf(NullPointerException.class);
    }

    // ── multiple sequential executions ────────────────────────────────────────

    @Test
    void execute_calledMultipleTimes_eachHasItsOwnTransaction() {
        txResult.execute(() -> Result.ok(1));
        txResult.execute(() -> Result.err("e"));
        txResult.execute(() -> Result.ok(2));

        assertThat(tx.beginCount).isEqualTo(3);
        assertThat(tx.commitCount).isEqualTo(2);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    // ── executeNew — REQUIRES_NEW semantics ───────────────────────────────────

    @Test
    void executeNew_okResult_commitsInner_noOuterToResume() {
        tx.transactionToReturn = null;

        var result = txResult.executeNew(() -> Result.ok("inner"));

        assertThat(result).isOk().containsValue("inner");
        assertThat(tx.suspendCount).isEqualTo(1);
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
        assertThat(tx.resumeCount).isEqualTo(0);
    }

    @Test
    void executeNew_errResult_rollsBackInner_resumesOuter() {
        var outer = new StubTransaction();
        tx.transactionToReturn = outer;

        var result = txResult.executeNew(() -> Result.err("inner error"));

        assertThat(result).isErr().containsError("inner error");
        assertThat(tx.suspendCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.resumeCount).isEqualTo(1);
        assertThat(tx.lastResumedTx).isSameAs(outer);
    }

    @Test
    void executeNew_okResult_commitsInner_resumesOuter() {
        var outer = new StubTransaction();
        tx.transactionToReturn = outer;

        txResult.executeNew(() -> Result.ok("inner"));

        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
        assertThat(tx.resumeCount).isEqualTo(1);
        assertThat(tx.lastResumedTx).isSameAs(outer);
    }

    @Test
    void executeNew_nullAction_throwsNPE() {
        assertThatThrownBy(() -> txResult.executeNew(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }
}
