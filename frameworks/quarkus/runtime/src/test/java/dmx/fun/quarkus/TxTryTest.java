package dmx.fun.quarkus;

import dmx.fun.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TxTryTest {

    StubTransactionManager tx;
    TxTry txTry;

    @BeforeEach
    void setUp() {
        tx = new StubTransactionManager();
        txTry = new TxTry(tx);
    }

    // ── construction ──────────────────────────────────────────────────────────

    @Test
    void constructor_nullTransactionManager_throwsNPE() {
        assertThatThrownBy(() -> new TxTry(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("transactionManager");
    }

    // ── execute — commit path ─────────────────────────────────────────────────

    @Test
    void execute_successTry_commits() {
        var result = txTry.execute(() -> Try.success("value"));

        assertThat(result).isSuccess().containsValue("value");
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    // ── execute — rollback path ───────────────────────────────────────────────

    @Test
    void execute_failureTry_rollsBack() {
        var cause = new IllegalStateException("domain failure");
        var result = txTry.execute(() -> Try.failure(cause));

        assertThat(result).isFailure();
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    @Test
    void execute_actionThrows_rollsBackAndRethrows() {
        var boom = new RuntimeException("boom");

        assertThatThrownBy(() -> txTry.execute(() -> { throw boom; }))
            .isSameAs(boom);

        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    // ── execute — null-argument guards ────────────────────────────────────────

    @Test
    void execute_nullAction_throwsNPE() {
        assertThatThrownBy(() -> txTry.execute(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    @Test
    void execute_actionReturnsNull_throwsNPEAndRollsBack() {
        assertThatThrownBy(() -> txTry.execute(() -> null))
            .isInstanceOf(NullPointerException.class);

        assertThat(tx.rollbackCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
    }

    // ── CDI proxy no-arg constructor ──────────────────────────────────────────

    @Test
    void noArgConstructor_executeWithValidAction_throwsNPEFromNullExecutor() {
        var proxy = new TxTry();
        assertThatThrownBy(() -> proxy.execute(() -> Try.success("x")))
            .isInstanceOf(NullPointerException.class);
    }

    // ── multiple sequential executions ────────────────────────────────────────

    @Test
    void execute_calledMultipleTimes_eachHasItsOwnTransaction() {
        txTry.execute(() -> Try.success(1));
        txTry.execute(() -> Try.failure(new RuntimeException()));
        txTry.execute(() -> Try.success(2));

        assertThat(tx.beginCount).isEqualTo(3);
        assertThat(tx.commitCount).isEqualTo(2);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    // ── executeNew — REQUIRES_NEW semantics ───────────────────────────────────

    @Test
    void executeNew_successTry_commitsInner_noOuterToResume() {
        tx.transactionToReturn = null;

        var result = txTry.executeNew(() -> Try.success("inner"));

        assertThat(result).isSuccess().containsValue("inner");
        assertThat(tx.suspendCount).isEqualTo(1);
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
        assertThat(tx.resumeCount).isEqualTo(0);
    }

    @Test
    void executeNew_failureTry_rollsBackInner_resumesOuter() {
        var outer = new StubTransaction();
        tx.transactionToReturn = outer;

        var result = txTry.executeNew(() -> Try.failure(new RuntimeException("inner")));

        assertThat(result).isFailure();
        assertThat(tx.suspendCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.resumeCount).isEqualTo(1);
        assertThat(tx.lastResumedTx).isSameAs(outer);
    }

    @Test
    void executeNew_successTry_commitsInner_resumesOuter() {
        var outer = new StubTransaction();
        tx.transactionToReturn = outer;

        txTry.executeNew(() -> Try.success("inner"));

        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
        assertThat(tx.resumeCount).isEqualTo(1);
        assertThat(tx.lastResumedTx).isSameAs(outer);
    }

    @Test
    void executeNew_nullAction_throwsNPE() {
        assertThatThrownBy(() -> txTry.executeNew(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }
}
