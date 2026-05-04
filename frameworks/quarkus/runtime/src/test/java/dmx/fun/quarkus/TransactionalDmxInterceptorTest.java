package dmx.fun.quarkus;

import dmx.fun.Result;
import dmx.fun.Try;
import jakarta.transaction.Status;
import jakarta.transaction.Transactional;
import jakarta.transaction.TransactionalException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalDmxInterceptorTest {

    // ── Target helpers ────────────────────────────────────────────────────────

    /** Method-level @TransactionalResult */
    static class ResultMethodTarget {
        @TransactionalResult
        Object doWork() { return null; }
    }

    /** Class-level @TransactionalResult */
    @TransactionalResult
    static class ResultClassTarget {
        Object doWork() { return null; }
    }

    /** Method-level @TransactionalTry */
    static class TryMethodTarget {
        @TransactionalTry
        Object doWork() { return null; }
    }

    /** Class-level @TransactionalTry */
    @TransactionalTry
    static class TryClassTarget {
        Object doWork() { return null; }
    }

    /** No annotation — predicate always returns false → always commits */
    static class PlainTarget {
        Object doWork() { return null; }
    }

    /** REQUIRES_NEW explicit */
    static class RequiresNewTarget {
        @TransactionalResult(Transactional.TxType.REQUIRES_NEW)
        Object doWork() { return null; }
    }

    /** MANDATORY explicit */
    static class MandatoryTarget {
        @TransactionalResult(Transactional.TxType.MANDATORY)
        Object doWork() { return null; }
    }

    /** NEVER explicit */
    static class NeverTarget {
        @TransactionalResult(Transactional.TxType.NEVER)
        Object doWork() { return null; }
    }

    private static Method doWork(Class<?> clazz) {
        try {
            return clazz.getDeclaredMethod("doWork");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    // ── Fixture ───────────────────────────────────────────────────────────────

    StubTransactionManager tx;
    TransactionalDmxInterceptor interceptor;

    @BeforeEach
    void setUp() {
        tx = new StubTransactionManager();
        // Default: no outer transaction so REQUIRED starts a new one (matches pre-TxType behaviour).
        tx.statusToReturn = Status.STATUS_NO_TRANSACTION;
        interceptor = new TransactionalDmxInterceptor(tx);
    }

    // ── @TransactionalResult at method level ──────────────────────────────────

    @Test
    void methodResult_okReturn_commits() throws Exception {
        var ctx = new StubInvocationContext(
            new ResultMethodTarget(), doWork(ResultMethodTarget.class),
            () -> Result.ok("value"));

        var returned = interceptor.intercept(ctx);

        assertThat(returned).isEqualTo(Result.ok("value"));
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    @Test
    void methodResult_errReturn_rollsBack() throws Exception {
        var ctx = new StubInvocationContext(
            new ResultMethodTarget(), doWork(ResultMethodTarget.class),
            () -> Result.err("domain error"));

        interceptor.intercept(ctx);

        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    // ── @TransactionalResult at class level ───────────────────────────────────

    @Test
    void classResult_okReturn_commits() throws Exception {
        var ctx = new StubInvocationContext(
            new ResultClassTarget(), doWork(ResultClassTarget.class),
            () -> Result.ok("value"));

        interceptor.intercept(ctx);

        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    @Test
    void classResult_errReturn_rollsBack() throws Exception {
        var ctx = new StubInvocationContext(
            new ResultClassTarget(), doWork(ResultClassTarget.class),
            () -> Result.err("error"));

        interceptor.intercept(ctx);

        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    // ── @TransactionalTry at method level ────────────────────────────────────

    @Test
    void methodTry_successReturn_commits() throws Exception {
        var ctx = new StubInvocationContext(
            new TryMethodTarget(), doWork(TryMethodTarget.class),
            () -> Try.success("value"));

        interceptor.intercept(ctx);

        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    @Test
    void methodTry_failureReturn_rollsBack() throws Exception {
        var ctx = new StubInvocationContext(
            new TryMethodTarget(), doWork(TryMethodTarget.class),
            () -> Try.failure(new IllegalStateException("fail")));

        interceptor.intercept(ctx);

        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    // ── @TransactionalTry at class level ─────────────────────────────────────

    @Test
    void classTry_successReturn_commits() throws Exception {
        var ctx = new StubInvocationContext(
            new TryClassTarget(), doWork(TryClassTarget.class),
            () -> Try.success("value"));

        interceptor.intercept(ctx);

        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    @Test
    void classTry_failureReturn_rollsBack() throws Exception {
        var ctx = new StubInvocationContext(
            new TryClassTarget(), doWork(TryClassTarget.class),
            () -> Try.failure(new RuntimeException("fail")));

        interceptor.intercept(ctx);

        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.rollbackCount).isEqualTo(1);
    }

    // ── No annotation — predicate always false ────────────────────────────────

    @Test
    void noAnnotation_anyReturn_commits() throws Exception {
        var ctx = new StubInvocationContext(
            new PlainTarget(), doWork(PlainTarget.class),
            () -> "some-value");

        interceptor.intercept(ctx);

        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    // ── Annotation present but return type does not match ────────────────────
    // These cover the `instanceof` false branches in the rollback predicate.

    @Test
    void resultAnnotation_nonResultReturn_commits() throws Exception {
        // @TransactionalResult on method, but proceed() returns a plain String — predicate
        // falls through to "return false", so the transaction commits.
        var ctx = new StubInvocationContext(
            new ResultMethodTarget(), doWork(ResultMethodTarget.class),
            () -> "not-a-result");

        interceptor.intercept(ctx);

        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    @Test
    void tryAnnotation_nonTryReturn_commits() throws Exception {
        // @TransactionalTry on method, but proceed() returns a plain String — predicate
        // falls through to "return false", so the transaction commits.
        var ctx = new StubInvocationContext(
            new TryMethodTarget(), doWork(TryMethodTarget.class),
            () -> "not-a-try");

        interceptor.intercept(ctx);

        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.rollbackCount).isEqualTo(0);
    }

    // ── Exception handling ────────────────────────────────────────────────────

    @Test
    void runtimeException_rollsBackAndRethrows() {
        var boom = new RuntimeException("boom");
        var ctx = new StubInvocationContext(
            new ResultMethodTarget(), doWork(ResultMethodTarget.class),
            () -> { throw boom; });

        assertThatThrownBy(() -> interceptor.intercept(ctx))
            .isSameAs(boom);

        assertThat(tx.rollbackCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
    }

    @Test
    void checkedException_wrappedInRuntimeExceptionAndRollsBack() {
        var cause = new Exception("checked");
        var ctx = new StubInvocationContext(
            new ResultMethodTarget(), doWork(ResultMethodTarget.class),
            () -> { throw cause; });

        assertThatThrownBy(() -> interceptor.intercept(ctx))
            .isInstanceOf(RuntimeException.class)
            .hasCause(cause);

        assertThat(tx.rollbackCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(0);
    }

    // ── TxType resolution ─────────────────────────────────────────────────────

    @Test
    void requiresNew_alwaysSuspendsAndStartsFreshTx() throws Exception {
        var outer = new StubTransaction();
        tx.transactionToReturn = outer;
        tx.statusToReturn = Status.STATUS_ACTIVE; // outer tx is active

        var ctx = new StubInvocationContext(
            new RequiresNewTarget(), doWork(RequiresNewTarget.class),
            () -> Result.ok("v"));

        interceptor.intercept(ctx);

        assertThat(tx.suspendCount).isEqualTo(1);
        assertThat(tx.beginCount).isEqualTo(1);
        assertThat(tx.commitCount).isEqualTo(1);
        assertThat(tx.resumeCount).isEqualTo(1);
        assertThat(tx.lastResumedTx).isSameAs(outer);
    }

    @Test
    void mandatory_withoutActiveTx_throwsTransactionalException() {
        // statusToReturn is NO_TRANSACTION (set in setUp)
        var ctx = new StubInvocationContext(
            new MandatoryTarget(), doWork(MandatoryTarget.class),
            () -> Result.ok("v"));

        assertThatThrownBy(() -> interceptor.intercept(ctx))
            .isInstanceOf(TransactionalException.class)
            .hasMessageContaining("MANDATORY");

        assertThat(tx.beginCount).isEqualTo(0);
    }

    @Test
    void never_withActiveTx_throwsTransactionalException() {
        tx.statusToReturn = Status.STATUS_ACTIVE;
        var ctx = new StubInvocationContext(
            new NeverTarget(), doWork(NeverTarget.class),
            () -> Result.ok("v"));

        assertThatThrownBy(() -> interceptor.intercept(ctx))
            .isInstanceOf(TransactionalException.class)
            .hasMessageContaining("NEVER");

        assertThat(tx.beginCount).isEqualTo(0);
    }

    @Test
    void required_withActiveTx_joinsAndSetsRollbackOnlyOnErr() throws Exception {
        tx.statusToReturn = Status.STATUS_ACTIVE;
        var ctx = new StubInvocationContext(
            new ResultMethodTarget(), doWork(ResultMethodTarget.class),
            () -> Result.err("e"));

        interceptor.intercept(ctx);

        assertThat(tx.beginCount).isEqualTo(0);
        assertThat(tx.commitCount).isEqualTo(0);
        assertThat(tx.setRollbackOnlyCount).isEqualTo(1);
    }
}
