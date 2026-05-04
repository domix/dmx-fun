package dmx.fun.quarkus;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TxExecutorTest {

    StubTransactionManager tx;
    TxExecutor executor;

    @BeforeEach
    void setUp() {
        tx = new StubTransactionManager();
        executor = new TxExecutor(tx);
    }

    // ── construction ──────────────────────────────────────────────────────────

    @Test
    void constructor_nullTransactionManager_throwsNPE() {
        assertThatThrownBy(() -> new TxExecutor(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("transactionManager");
    }

    // ── execute — happy path ──────────────────────────────────────────────────

    @Test
    void execute_predicateFalse_commits() {
        var result = executor.execute(() -> "ok", v -> false);

        assertThat(result).isEqualTo("ok");
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    @Test
    void execute_predicateTrue_rollsBack() {
        var result = executor.execute(() -> "fail", v -> true);

        assertThat(result).isEqualTo("fail");
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    // ── execute — action throws ───────────────────────────────────────────────

    @Test
    void execute_actionThrows_rollsBackAndRethrows() {
        var boom = new RuntimeException("boom");

        assertThatThrownBy(() -> executor.execute(() -> { throw boom; }, v -> false))
            .isSameAs(boom);

        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    @Test
    void execute_actionThrows_rollbackSwallowsSystemException() throws SystemException {
        tx.throwOnRollback = new SystemException("rollback failed");

        assertThatThrownBy(() -> executor.execute(() -> { throw new RuntimeException("boom"); }, v -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");

        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    @Test
    void execute_actionReturnsNull_rollsBackAndThrowsNPE() {
        assertThatThrownBy(() -> executor.execute(() -> null, v -> false))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("null");

        assertThat(tx.rollbackCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
    }

    // ── execute — null argument guards ────────────────────────────────────────

    @Test
    void execute_nullAction_throwsNPE() {
        assertThatThrownBy(() -> executor.execute(null, v -> false))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    @Test
    void execute_nullPredicate_throwsNPE() {
        assertThatThrownBy(() -> executor.execute(() -> "ok", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("shouldRollback");
    }

    // ── execute — JTA infrastructure exceptions ───────────────────────────────

    @Test
    void execute_beginThrowsNotSupportedException_wrapsInRuntimeException() {
        tx.throwOnBegin = new NotSupportedException("nested tx not supported");

        assertThatThrownBy(() -> executor.execute(() -> "x", v -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(NotSupportedException.class);
    }

    @Test
    void execute_beginThrowsSystemException_wrapsInRuntimeException() {
        tx.throwOnBegin = new SystemException("tx system error");

        assertThatThrownBy(() -> executor.execute(() -> "x", v -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(SystemException.class);
    }

    @Test
    void execute_commitThrowsRollbackException_wrapsInRuntimeException() {
        tx.throwOnCommit = new RollbackException("tx rolled back");

        assertThatThrownBy(() -> executor.execute(() -> "x", v -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(RollbackException.class);
    }

    @Test
    void execute_commitThrowsHeuristicMixedException_wrapsInRuntimeException() {
        tx.throwOnCommit = new HeuristicMixedException("heuristic mixed");

        assertThatThrownBy(() -> executor.execute(() -> "x", v -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(HeuristicMixedException.class);
    }

    @Test
    void execute_commitThrowsHeuristicRollbackException_wrapsInRuntimeException() {
        tx.throwOnCommit = new HeuristicRollbackException("heuristic rollback");

        assertThatThrownBy(() -> executor.execute(() -> "x", v -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(HeuristicRollbackException.class);
    }

    @Test
    void execute_commitThrowsSystemException_wrapsInRuntimeException() {
        tx.throwOnCommit = new SystemException("tx system error on commit");

        assertThatThrownBy(() -> executor.execute(() -> "x", v -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(SystemException.class);
    }

    // ── executeNew — null argument guards ─────────────────────────────────────

    @Test
    void executeNew_nullAction_throwsNPE() {
        assertThatThrownBy(() -> executor.executeNew(null, v -> false))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    @Test
    void executeNew_nullPredicate_throwsNPE() {
        assertThatThrownBy(() -> executor.executeNew(() -> "ok", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("shouldRollback");
    }

    // ── executeNew — no outer transaction ─────────────────────────────────────

    @Test
    void executeNew_noOuterTx_commitsInnerAndDoesNotResume() {
        // suspend() returns null → no outer tx to resume
        tx.transactionToReturn = null;

        var result = executor.executeNew(() -> "value", v -> false);

        assertThat(result).isEqualTo("value");
        assertThat(tx.suspendCount).isEqualTo(1);
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
        assertThat(tx.resumeCount).isEqualTo(0);
    }

    // ── executeNew — with outer transaction ───────────────────────────────────

    @Test
    void executeNew_withOuterTx_suspends_commits_resumesOuter() {
        var outer = new StubTransaction();
        tx.transactionToReturn = outer;

        var result = executor.executeNew(() -> "value", v -> false);

        assertThat(result).isEqualTo("value");
        assertThat(tx.suspendCount).isEqualTo(1);
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
        assertThat(tx.resumeCount).isEqualTo(1);
        assertThat(tx.lastResumedTx).isSameAs(outer);
    }

    @Test
    void executeNew_withOuterTx_suspends_rollsBack_resumesOuter() {
        var outer = new StubTransaction();
        tx.transactionToReturn = outer;

        var result = executor.executeNew(() -> "fail", v -> true);

        assertThat(result).isEqualTo("fail");
        assertThat(tx.suspendCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.resumeCount).isEqualTo(1);
        assertThat(tx.lastResumedTx).isSameAs(outer);
    }

    @Test
    void executeNew_actionThrows_rollsBackAndResumesOuter() {
        var outer = new StubTransaction();
        tx.transactionToReturn = outer;
        var boom = new RuntimeException("boom");

        assertThatThrownBy(() -> executor.executeNew(() -> { throw boom; }, v -> false))
            .isSameAs(boom);

        assertThat(tx.suspendCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.resumeCount).isEqualTo(1);
        assertThat(tx.lastResumedTx).isSameAs(outer);
    }

    // ── executeNew — JTA infrastructure exceptions ────────────────────────────

    @Test
    void executeNew_suspendThrowsSystemException_wrapsInRuntimeException() {
        tx.throwOnSuspend = new SystemException("suspend failed");

        assertThatThrownBy(() -> executor.executeNew(() -> "x", v -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(SystemException.class);

        assertThat(tx.beginCount).isEqualTo(0);
    }

    @Test
    void executeNew_resumeThrowsInvalidTransactionException_wrapsInRuntimeException()
            throws SystemException {
        var outer = new StubTransaction();
        tx.transactionToReturn = outer;
        tx.throwOnResume = new InvalidTransactionException("invalid tx");

        assertThatThrownBy(() -> executor.executeNew(() -> "x", v -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to resume suspended transaction")
            .hasCauseInstanceOf(InvalidTransactionException.class);

        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.resumeCount).isEqualTo(1);
    }

    @Test
    void executeNew_resumeThrowsSystemException_wrapsInRuntimeException()
            throws SystemException {
        var outer = new StubTransaction();
        tx.transactionToReturn = outer;
        tx.throwOnResume = new SystemException("resume failed");

        assertThatThrownBy(() -> executor.executeNew(() -> "x", v -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to resume suspended transaction")
            .hasCauseInstanceOf(SystemException.class);

        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.resumeCount).isEqualTo(1);
    }
}
