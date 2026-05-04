package dmx.fun.quarkus;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transactional;
import jakarta.transaction.TransactionalException;
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
        var result = executor.execute(() -> "ok", _ -> false);

        assertThat(result).isEqualTo("ok");
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    @Test
    void execute_predicateTrue_rollsBack() {
        var result = executor.execute(() -> "fail", _ -> true);

        assertThat(result).isEqualTo("fail");
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    // ── execute — action throws ───────────────────────────────────────────────

    @Test
    void execute_actionThrows_rollsBackAndRethrows() {
        var boom = new RuntimeException("boom");

        assertThatThrownBy(() -> executor.execute(() -> { throw boom; }, _ -> false))
            .isSameAs(boom);

        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    @Test
    void execute_actionThrows_rollbackSwallowsSystemException() throws SystemException {
        tx.throwOnRollback = new SystemException("rollback failed");

        assertThatThrownBy(() -> executor.execute(() -> { throw new RuntimeException("boom"); }, _ -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");

        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    @Test
    void execute_actionReturnsNull_rollsBackAndThrowsNPE() {
        assertThatThrownBy(() -> executor.execute(() -> null, _ -> false))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("null");

        assertThat(tx.rollbackCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
    }

    // ── execute — null argument guards ────────────────────────────────────────

    @Test
    void execute_nullAction_throwsNPE() {
        assertThatThrownBy(() -> executor.execute(null, _ -> false))
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

        assertThatThrownBy(() -> executor.execute(() -> "x", _ -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(NotSupportedException.class);
    }

    @Test
    void execute_beginThrowsSystemException_wrapsInRuntimeException() {
        tx.throwOnBegin = new SystemException("tx system error");

        assertThatThrownBy(() -> executor.execute(() -> "x", _ -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(SystemException.class);
    }

    @Test
    void execute_commitThrowsRollbackException_wrapsInRuntimeException() {
        tx.throwOnCommit = new RollbackException("tx rolled back");

        assertThatThrownBy(() -> executor.execute(() -> "x", _ -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(RollbackException.class);
    }

    @Test
    void execute_commitThrowsHeuristicMixedException_wrapsInRuntimeException() {
        tx.throwOnCommit = new HeuristicMixedException("heuristic mixed");

        assertThatThrownBy(() -> executor.execute(() -> "x", _ -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(HeuristicMixedException.class);
    }

    @Test
    void execute_commitThrowsHeuristicRollbackException_wrapsInRuntimeException() {
        tx.throwOnCommit = new HeuristicRollbackException("heuristic rollback");

        assertThatThrownBy(() -> executor.execute(() -> "x", _ -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(HeuristicRollbackException.class);
    }

    @Test
    void execute_commitThrowsSystemException_wrapsInRuntimeException() {
        tx.throwOnCommit = new SystemException("tx system error on commit");

        assertThatThrownBy(() -> executor.execute(() -> "x", _ -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(SystemException.class);
    }

    // ── executeNew — null argument guards ─────────────────────────────────────

    @Test
    void executeNew_nullAction_throwsNPE() {
        assertThatThrownBy(() -> executor.executeNew(null, _ -> false))
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

        var result = executor.executeNew(() -> "value", _ -> false);

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

        var result = executor.executeNew(() -> "value", _ -> false);

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

        var result = executor.executeNew(() -> "fail", _ -> true);

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

        assertThatThrownBy(() -> executor.executeNew(() -> { throw boom; }, _ -> false))
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

        assertThatThrownBy(() -> executor.executeNew(() -> "x", _ -> false))
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

        assertThatThrownBy(() -> executor.executeNew(() -> "x", _ -> false))
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

        assertThatThrownBy(() -> executor.executeNew(() -> "x", _ -> false))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to resume suspended transaction")
            .hasCauseInstanceOf(SystemException.class);

        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.resumeCount).isEqualTo(1);
    }

    // ── execute(TxType) — REQUIRED ────────────────────────────────────────────

    @Test
    void execute_required_noOuterTx_beginsAndCommits() {
        tx.statusToReturn = Status.STATUS_NO_TRANSACTION;

        var result = executor.execute(() -> "ok", _ -> false, Transactional.TxType.REQUIRED);

        assertThat(result).isEqualTo("ok");
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.setRollbackOnlyCount).isEqualTo(0);
    }

    @Test
    void execute_required_noOuterTx_rollsBackOnErr() {
        tx.statusToReturn = Status.STATUS_NO_TRANSACTION;

        executor.execute(() -> "err", _ -> true, Transactional.TxType.REQUIRED);

        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
    }

    @Test
    void execute_required_withOuterTx_joinsAndCommitsViaOuter() {
        // status is ACTIVE by default — should join, not begin

        var result = executor.execute(() -> "ok", _ -> false, Transactional.TxType.REQUIRED);

        assertThat(result).isEqualTo("ok");
        assertThat(tx.beginCount).isEqualTo(0);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.setRollbackOnlyCount).isEqualTo(0);
    }

    @Test
    void execute_required_withOuterTx_setsRollbackOnlyOnErr() {
        executor.execute(() -> "err", _ -> true, Transactional.TxType.REQUIRED);

        assertThat(tx.beginCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(0);
        assertThat(tx.setRollbackOnlyCount).isEqualTo(1);
    }

    @Test
    void execute_required_withOuterTx_setsRollbackOnlyOnException() {
        assertThatThrownBy(() ->
            executor.execute(
                () -> { throw new RuntimeException("boom"); },
                _ -> false,
                Transactional.TxType.REQUIRED))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");

        assertThat(tx.rollbackCount).isEqualTo(0);
        assertThat(tx.setRollbackOnlyCount).isEqualTo(1);
    }

    // ── execute(TxType) — MANDATORY ───────────────────────────────────────────

    @Test
    void execute_mandatory_withoutTx_throwsTransactionalException() {
        tx.statusToReturn = Status.STATUS_NO_TRANSACTION;

        assertThatThrownBy(() -> executor.execute(() -> "x", _-> false, Transactional.TxType.MANDATORY))
            .isInstanceOf(TransactionalException.class)
            .hasMessageContaining("MANDATORY");

        assertThat(tx.beginCount).isEqualTo(0);
    }

    @Test
    void execute_mandatory_withTx_joinsAndSetsRollbackOnlyOnErr() {
        executor.execute(() -> "err", _ -> true, Transactional.TxType.MANDATORY);

        assertThat(tx.beginCount).isEqualTo(0);
        assertThat(tx.setRollbackOnlyCount).isEqualTo(1);
    }

    // ── execute(TxType) — SUPPORTS ────────────────────────────────────────────

    @Test
    void execute_supports_withTx_joinsAndSetsRollbackOnlyOnErr() {
        executor.execute(() -> "err", _ -> true, Transactional.TxType.SUPPORTS);

        assertThat(tx.beginCount).isEqualTo(0);
        assertThat(tx.setRollbackOnlyCount).isEqualTo(1);
    }

    @Test
    void execute_supports_withoutTx_runsWithoutBeginOrCommit() {
        tx.statusToReturn = Status.STATUS_NO_TRANSACTION;

        var result = executor.execute(() -> "ok", _ -> false, Transactional.TxType.SUPPORTS);

        assertThat(result).isEqualTo("ok");
        assertThat(tx.beginCount).isEqualTo(0);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.setRollbackOnlyCount).isEqualTo(0);
    }

    // ── execute(TxType) — NOT_SUPPORTED ──────────────────────────────────────

    @Test
    void execute_notSupported_withTx_suspendsAndResumes() {
        var outer = new StubTransaction();
        tx.transactionToReturn = outer;

        var result = executor.execute(() -> "ok", _ -> false, Transactional.TxType.NOT_SUPPORTED);

        assertThat(result).isEqualTo("ok");
        assertThat(tx.suspendCount).isEqualTo(1);
        assertThat(tx.resumeCount).isEqualTo(1);
        assertThat(tx.lastResumedTx).isSameAs(outer);
        assertThat(tx.beginCount).isEqualTo(0);
    }

    @Test
    void execute_notSupported_withoutTx_doesNotSuspend() {
        tx.statusToReturn = Status.STATUS_NO_TRANSACTION;

        executor.execute(() -> "ok", _ -> false, Transactional.TxType.NOT_SUPPORTED);

        assertThat(tx.suspendCount).isEqualTo(0);
        assertThat(tx.resumeCount).isEqualTo(0);
    }

    // ── execute(TxType) — NEVER ───────────────────────────────────────────────

    @Test
    void execute_never_withTx_throwsTransactionalException() {
        assertThatThrownBy(() -> executor.execute(() -> "x", _ -> false, Transactional.TxType.NEVER))
            .isInstanceOf(TransactionalException.class)
            .hasMessageContaining("NEVER");

        assertThat(tx.beginCount).isEqualTo(0);
    }

    @Test
    void execute_never_withoutTx_runsWithoutBeginOrCommit() {
        tx.statusToReturn = Status.STATUS_NO_TRANSACTION;

        var result = executor.execute(() -> "ok", _ -> false, Transactional.TxType.NEVER);

        assertThat(result).isEqualTo("ok");
        assertThat(tx.beginCount).isEqualTo(0);
        assertThat(tx.commitCount).isEqualTo(0);
    }

    // ── hasActiveTransaction — SystemException ────────────────────────────────

    @Test
    void execute_required_getStatusThrowsSystemException_wrapsInRuntimeException() {
        tx.throwOnGetStatus = new SystemException("getStatus failed");

        assertThatThrownBy(() -> executor.execute(() -> "x", _ -> false, Transactional.TxType.REQUIRED))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(SystemException.class);
    }

    // ── executeNotSupported — suspend SystemException ─────────────────────────

    @Test
    void execute_notSupported_suspendThrowsSystemException_wrapsInRuntimeException() {
        // STATUS_ACTIVE by default → hasActiveTransaction() returns true → suspend() is called
        tx.throwOnSuspend = new SystemException("suspend failed");

        assertThatThrownBy(() -> executor.execute(() -> "x", _ -> false, Transactional.TxType.NOT_SUPPORTED))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(SystemException.class);

        assertThat(tx.beginCount).isEqualTo(0);
    }

    // ── executeJoined — Error handling (Finding 1) ───────────────────────────

    @Test
    void execute_required_actionThrowsError_setsRollbackOnlyAndRethrows() {
        // Error must be caught the same as RuntimeException — marks rollback-only then rethrows
        var oom = new OutOfMemoryError("simulated OOM");

        assertThatThrownBy(() -> executor.execute(() -> { throw oom; }, _ -> false, Transactional.TxType.REQUIRED))
            .isSameAs(oom);

        assertThat(tx.setRollbackOnlyCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    @Test
    void execute_required_predicateThrowsError_setsRollbackOnlyAndRethrows() {
        // Error thrown by the predicate must also mark rollback-only
        var assertionError = new AssertionError("predicate bug");

        assertThatThrownBy(() -> executor.execute(() -> "ok", _ -> { throw assertionError; }, Transactional.TxType.REQUIRED))
            .isSameAs(assertionError);

        assertThat(tx.setRollbackOnlyCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    // ── setRollbackOnlyQuietly — SystemException propagated (Finding 2) ──────

    @Test
    void execute_required_setRollbackOnlyThrowsSystemException_propagatesWhenPredicateTrue() {
        // Predicate returns true → setRollbackOnlyQuietly(); no original exception to suppress
        tx.throwOnSetRollbackOnly = new SystemException("setRollbackOnly failed");

        assertThatThrownBy(() -> executor.execute(() -> "err", _ -> true, Transactional.TxType.REQUIRED))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JTA transaction management failed")
            .hasCauseInstanceOf(SystemException.class);

        assertThat(tx.setRollbackOnlyCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    @Test
    void execute_required_actionThrows_setRollbackOnlyFails_infraExceptionSuppressed() {
        // Action throws + setRollbackOnly fails → infra exception is suppressed into the app exception
        tx.throwOnSetRollbackOnly = new SystemException("setRollbackOnly failed");
        var appEx = new RuntimeException("app error");

        assertThatThrownBy(() -> executor.execute(() -> { throw appEx; }, _ -> false, Transactional.TxType.REQUIRED))
            .isSameAs(appEx)
            .satisfies(e -> {
                assertThat(e.getSuppressed()).hasSize(1);
                assertThat(e.getSuppressed()[0])
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(SystemException.class);
            });

        assertThat(tx.setRollbackOnlyCount).isEqualTo(1);
    }

    @Test
    void execute_required_predicateThrows_setRollbackOnlyFails_infraExceptionSuppressed() {
        // Predicate throws + setRollbackOnly fails → infra exception is suppressed into predicate exception
        tx.throwOnSetRollbackOnly = new SystemException("setRollbackOnly failed");
        var predicateEx = new RuntimeException("predicate error");

        assertThatThrownBy(() -> executor.execute(() -> "ok", _ -> { throw predicateEx; }, Transactional.TxType.REQUIRED))
            .isSameAs(predicateEx)
            .satisfies(e -> {
                assertThat(e.getSuppressed()).hasSize(1);
                assertThat(e.getSuppressed()[0])
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(SystemException.class);
            });

        assertThat(tx.setRollbackOnlyCount).isEqualTo(1);
    }

    // ── execute(TxType) — STATUS_MARKED_ROLLBACK regression ──────────────────
    // After setRollbackOnly() the status is MARKED_ROLLBACK, not ACTIVE.
    // All propagation decisions must treat MARKED_ROLLBACK as "has active tx".

    @Test
    void execute_required_markedRollback_joinsInsteadOfBeginningNew() {
        tx.statusToReturn = Status.STATUS_MARKED_ROLLBACK;

        executor.execute(() -> "ok", _ -> false, Transactional.TxType.REQUIRED);

        assertThat(tx.beginCount).isEqualTo(0);  // joined, not started
        assertThat(tx.commitCount).isEqualTo(0);
    }

    @Test
    void execute_mandatory_markedRollback_joinsInsteadOfThrowing() {
        tx.statusToReturn = Status.STATUS_MARKED_ROLLBACK;

        executor.execute(() -> "ok", _ -> false, Transactional.TxType.MANDATORY);

        assertThat(tx.beginCount).isEqualTo(0);  // joined, not thrown
    }

    @Test
    void execute_supports_markedRollback_joinsInsteadOfRunningWithoutTx() {
        tx.statusToReturn = Status.STATUS_MARKED_ROLLBACK;

        executor.execute(() -> "ok", _ -> false, Transactional.TxType.SUPPORTS);

        assertThat(tx.beginCount).isEqualTo(0);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.setRollbackOnlyCount).isEqualTo(0);
    }

    @Test
    void execute_notSupported_markedRollback_suspendsLikeActiveTx() {
        tx.statusToReturn = Status.STATUS_MARKED_ROLLBACK;
        tx.transactionToReturn = new StubTransaction();

        executor.execute(() -> "ok", _ -> false, Transactional.TxType.NOT_SUPPORTED);

        assertThat(tx.suspendCount).isEqualTo(1);
        assertThat(tx.resumeCount).isEqualTo(1);
    }

    @Test
    void execute_never_markedRollback_throwsLikeActiveTx() {
        tx.statusToReturn = Status.STATUS_MARKED_ROLLBACK;

        assertThatThrownBy(() -> executor.execute(() -> "x", _ -> false, Transactional.TxType.NEVER))
            .isInstanceOf(TransactionalException.class)
            .hasMessageContaining("NEVER");
    }

    // ── execute(TxType) — null checks ─────────────────────────────────────────

    @Test
    void execute_txType_nullAction_throwsNPE() {
        assertThatThrownBy(() -> executor.execute(null, _ -> false, Transactional.TxType.REQUIRED))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("action");
    }

    @Test
    void execute_txType_nullPredicate_throwsNPE() {
        assertThatThrownBy(() -> executor.execute(() -> "x", null, Transactional.TxType.REQUIRED))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("shouldRollback");
    }

    @Test
    void execute_txType_nullTxType_throwsNPE() {
        assertThatThrownBy(() -> executor.execute(() -> "x", _ -> false, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("txType");
    }
}
