package dmx.fun.quarkus;

import dmx.fun.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TxResultTest {

    StubUserTransaction tx;
    TxResult txResult;

    @BeforeEach
    void setUp() {
        tx = new StubUserTransaction();
        txResult = new TxResult(tx);
    }

    // ── construction ──────────────────────────────────────────────────────────

    @Test
    void constructor_nullUserTransaction_throwsNPE() {
        assertThatThrownBy(() -> new TxResult(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("userTransaction");
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

    // ── null-argument guards ──────────────────────────────────────────────────

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
        // The protected no-arg ctor is for CDI proxy subclasses only;
        // executor is left null, so any real call fails immediately.
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
}
