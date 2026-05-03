package dmx.fun.quarkus;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TxExecutorTest {

    StubUserTransaction tx;
    TxExecutor executor;

    @BeforeEach
    void setUp() {
        tx = new StubUserTransaction();
        executor = new TxExecutor(tx);
    }

    // ── construction ──────────────────────────────────────────────────────────

    @Test
    void constructor_nullUserTransaction_throwsNPE() {
        assertThatThrownBy(() -> new TxExecutor(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("userTransaction");
    }

    // ── happy path ────────────────────────────────────────────────────────────

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

    // ── action throws ─────────────────────────────────────────────────────────

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
            .hasMessage("boom"); // SystemException swallowed, original propagates

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

    // ── null argument guards ──────────────────────────────────────────────────

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

    // ── JTA infrastructure exceptions → wrapped RuntimeException ─────────────

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
}
