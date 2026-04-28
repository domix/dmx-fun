package dmx.fun.spring;

import dmx.fun.Try;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract tests verifying that {@link TransactionalTry} propagation settings
 * behave consistently with Spring's {@link org.springframework.transaction.annotation.Transactional}
 * contract.
 *
 * <p>Each propagation mode is exercised in both its success and failure paths to confirm
 * that commit/rollback semantics match Spring's documented behaviour:
 * <ul>
 *   <li>{@code REQUIRED} — joins an existing outer transaction; inner failure marks
 *       the shared transaction globally rollback-only, causing the outer commit to throw
 *       {@link org.springframework.transaction.UnexpectedRollbackException}.</li>
 *   <li>{@code REQUIRES_NEW} — suspends the outer transaction and starts an independent
 *       one; commit/rollback of the inner transaction does not affect the outer.</li>
 *   <li>{@code MANDATORY} — throws {@link IllegalTransactionStateException} when no
 *       active transaction is present.</li>
 *   <li>{@code NEVER} — throws {@link IllegalTransactionStateException} when an active
 *       transaction is present.</li>
 *   <li>{@code SUPPORTS} — runs non-transactionally (auto-commit) when no outer tx is
 *       present; a failure return cannot roll back what was already auto-committed.</li>
 *   <li>{@code NOT_SUPPORTED} — suspends the outer transaction; the inner method runs
 *       without a transaction (auto-commit), so its work persists regardless of what
 *       the outer transaction does.</li>
 * </ul>
 */
class TransactionalTryContractTest extends AbstractH2AspectTestBase {

    static ContractService service;

    @BeforeAll
    static void startContext() {
        initContext(ServiceConfig.class);
        service = ctx.getBean(ContractService.class);
    }

    // ── Propagation.REQUIRED ─────────────────────────────────────────────────

    @Test
    void required_joinsOuterTx_innerFailure_causesUnexpectedRollbackOnOuterCommit() {
        assertThatThrownBy(() ->
            new TransactionTemplate(primaryRecording).execute(status -> {
                service.insertRequiredAndFail(1);
                return null;
            })
        ).isInstanceOf(UnexpectedRollbackException.class);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void required_joinsOuterTx_outerRollback_undoesInnerWork() {
        new TransactionTemplate(primaryRecording).execute(status -> {
            service.insertRequiredAndSucceed(1);
            status.setRollbackOnly();
            return null;
        });
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    // ── Propagation.REQUIRES_NEW ─────────────────────────────────────────────

    @Test
    void requiresNew_innerSucceed_commitsImmediately_outerRollbackDoesNotUndoInner() {
        new TransactionTemplate(primaryRecording).execute(status -> {
            jdbc.update("INSERT INTO events VALUES (100, 'outer')");
            service.insertRequiresNewAndSucceed(101);
            status.setRollbackOnly();
            return null;
        });
        Assertions.assertThat(countRows()).isEqualTo(1);
        Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM events WHERE id = 101", Integer.class)
        ).isEqualTo(1);
    }

    @Test
    void requiresNew_innerFail_rollsBackInnerOnly_outerCommitsItsWork() {
        new TransactionTemplate(primaryRecording).execute(status -> {
            jdbc.update("INSERT INTO events VALUES (200, 'outer')");
            service.insertRequiresNewAndFail(201);
            return null;
        });
        Assertions.assertThat(countRows()).isEqualTo(1);
        Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM events WHERE id = 200", Integer.class)
        ).isEqualTo(1);
        Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM events WHERE id = 201", Integer.class)
        ).isEqualTo(0);
    }

    // ── Propagation.MANDATORY ────────────────────────────────────────────────

    @Test
    void mandatory_throwsWhenNoActiveTx() {
        assertThatThrownBy(() -> service.insertMandatoryAndSucceed(1))
            .isInstanceOf(IllegalTransactionStateException.class);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void mandatory_participatesInActiveTx() {
        new TransactionTemplate(primaryRecording).execute(status -> {
            var result = service.insertMandatoryAndSucceed(1);
            assertThat(result).isSuccess();
            return null;
        });
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    // ── Propagation.NEVER ────────────────────────────────────────────────────

    @Test
    void never_throwsWhenActiveTxPresent() {
        assertThatThrownBy(() ->
            new TransactionTemplate(primaryRecording).execute(status -> {
                service.insertNeverAndSucceed(1);
                return null;
            })
        ).isInstanceOf(IllegalTransactionStateException.class);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void never_runsWithoutTx_whenNoActiveTx() {
        var result = service.insertNeverAndSucceed(1);
        assertThat(result).isSuccess();
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    // ── Propagation.SUPPORTS ─────────────────────────────────────────────────

    @Test
    void supports_withoutActiveTx_failDoesNotRollBack() {
        // Without an outer tx the method runs on an auto-commit connection.
        // A failure return has nothing to roll back, so the insert persists.
        var result = service.insertSupportsAndFail(1);
        assertThat(result).isFailure();
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void supports_withinActiveTx_failMarksGlobalRollbackOnly() {
        // Within an outer tx, SUPPORTS behaves like REQUIRED: the inner failure
        // marks the shared tx globally rollback-only, causing UnexpectedRollbackException.
        assertThatThrownBy(() ->
            new TransactionTemplate(primaryRecording).execute(status -> {
                service.insertSupportsAndFail(1);
                return null;
            })
        ).isInstanceOf(UnexpectedRollbackException.class);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    // ── Propagation.NOT_SUPPORTED ────────────────────────────────────────────

    @Test
    void notSupported_withinActiveTx_innerInsertPersists_outerRollbackDoesNotAffectInner() {
        // NOT_SUPPORTED suspends the outer tx; the inner method runs on a fresh
        // auto-commit connection, so its insert persists even after the outer rolls back.
        new TransactionTemplate(primaryRecording).execute(status -> {
            jdbc.update("INSERT INTO events VALUES (300, 'outer')");
            service.insertNotSupportedAndFail(301);
            status.setRollbackOnly();
            return null;
        });
        Assertions.assertThat(countRows()).isEqualTo(1);
        Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM events WHERE id = 301", Integer.class)
        ).isEqualTo(1);
    }

    // ── Service ──────────────────────────────────────────────────────────────

    interface ContractService {
        Try<Integer> insertRequiredAndSucceed(int id);
        Try<Integer> insertRequiredAndFail(int id);
        Try<Integer> insertRequiresNewAndSucceed(int id);
        Try<Integer> insertRequiresNewAndFail(int id);
        Try<Integer> insertMandatoryAndSucceed(int id);
        Try<Integer> insertNeverAndSucceed(int id);
        Try<Integer> insertSupportsAndFail(int id);
        Try<Integer> insertNotSupportedAndFail(int id);
    }

    static class ContractServiceImpl implements ContractService {
        private final JdbcTemplate jdbc;

        ContractServiceImpl(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        @TransactionalTry
        public Try<Integer> insertRequiredAndSucceed(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'req-ok')", id);
            return Try.success(id);
        }

        @Override
        @TransactionalTry
        public Try<Integer> insertRequiredAndFail(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'req-fail')", id);
            return Try.failure(new RuntimeException("fail"));
        }

        @Override
        @TransactionalTry(propagation = Propagation.REQUIRES_NEW)
        public Try<Integer> insertRequiresNewAndSucceed(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'new-ok')", id);
            return Try.success(id);
        }

        @Override
        @TransactionalTry(propagation = Propagation.REQUIRES_NEW)
        public Try<Integer> insertRequiresNewAndFail(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'new-fail')", id);
            return Try.failure(new RuntimeException("fail"));
        }

        @Override
        @TransactionalTry(propagation = Propagation.MANDATORY)
        public Try<Integer> insertMandatoryAndSucceed(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'mand-ok')", id);
            return Try.success(id);
        }

        @Override
        @TransactionalTry(propagation = Propagation.NEVER)
        public Try<Integer> insertNeverAndSucceed(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'never-ok')", id);
            return Try.success(id);
        }

        @Override
        @TransactionalTry(propagation = Propagation.SUPPORTS)
        public Try<Integer> insertSupportsAndFail(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'supp-fail')", id);
            return Try.failure(new RuntimeException("fail"));
        }

        @Override
        @TransactionalTry(propagation = Propagation.NOT_SUPPORTED)
        public Try<Integer> insertNotSupportedAndFail(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'notsup-fail')", id);
            return Try.failure(new RuntimeException("fail"));
        }
    }

    @Configuration
    static class ServiceConfig {
        @Bean
        ContractServiceImpl contractService(JdbcTemplate jdbcTemplate) {
            return new ContractServiceImpl(jdbcTemplate);
        }
    }
}
