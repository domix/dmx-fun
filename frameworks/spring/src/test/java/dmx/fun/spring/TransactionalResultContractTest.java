package dmx.fun.spring;

import dmx.fun.Result;
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
 * Contract tests verifying that {@link TransactionalResult} propagation settings
 * behave consistently with Spring's {@link org.springframework.transaction.annotation.Transactional}
 * contract.
 *
 * <p>Each propagation mode is exercised in both its success and failure paths to confirm
 * that commit/rollback semantics match Spring's documented behaviour:
 * <ul>
 *   <li>{@code REQUIRED} — joins an existing outer transaction; inner error marks the
 *       shared transaction globally rollback-only, causing the outer commit to throw
 *       {@link UnexpectedRollbackException}.</li>
 *   <li>{@code REQUIRES_NEW} — suspends the outer transaction and starts an independent
 *       one; commit/rollback of the inner transaction does not affect the outer.</li>
 *   <li>{@code MANDATORY} — throws {@link IllegalTransactionStateException} when no
 *       active transaction is present.</li>
 *   <li>{@code NEVER} — throws {@link IllegalTransactionStateException} when an active
 *       transaction is present.</li>
 *   <li>{@code SUPPORTS} — runs non-transactionally (auto-commit) when no outer tx is
 *       present; an error return cannot roll back what was already auto-committed.</li>
 *   <li>{@code NOT_SUPPORTED} — suspends the outer transaction; the inner method runs
 *       without a transaction (auto-commit), so its work persists regardless of what
 *       the outer transaction does.</li>
 * </ul>
 */
class TransactionalResultContractTest extends AbstractH2AspectTestBase {

    static ContractService service;

    @BeforeAll
    static void startContext() {
        initContext(ServiceConfig.class);
        service = ctx.getBean(ContractService.class);
    }

    // ── Propagation.REQUIRED ─────────────────────────────────────────────────

    @Test
    void required_joinsOuterTx_outerRollback_undoesInnerWork() {
        // Inner joins the outer tx; outer rollback cascades to inner work.
        new TransactionTemplate(primaryRecording).execute(status -> {
            service.insertRequiredAndOk(1);
            status.setRollbackOnly();
            return null;
        });
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void required_joinsOuterTx_innerErr_causesUnexpectedRollbackOnOuterCommit() {
        // Inner error calls setRollbackOnly() on the shared tx (global rollback-only).
        // The outer template then throws UnexpectedRollbackException on commit.
        assertThatThrownBy(() ->
            new TransactionTemplate(primaryRecording).execute(status -> {
                service.insertRequiredAndErr(1);
                return null;
            })
        ).isInstanceOf(UnexpectedRollbackException.class);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    // ── Propagation.REQUIRES_NEW ─────────────────────────────────────────────

    @Test
    void requiresNew_innerOk_commitsImmediately_outerRollbackDoesNotUndoInner() {
        // Inner tx commits independently; outer rollback cannot undo already-committed work.
        new TransactionTemplate(primaryRecording).execute(status -> {
            jdbc.update("INSERT INTO events VALUES (100, 'outer')");
            service.insertRequiresNewAndOk(101);
            status.setRollbackOnly();
            return null;
        });
        Assertions.assertThat(countRows()).isEqualTo(1);
        Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM events WHERE id = 101", Integer.class)
        ).isEqualTo(1);
    }

    @Test
    void requiresNew_innerErr_rollsBackInnerOnly_outerCommitsItsWork() {
        // Inner error rolls back only the inner tx; outer is unaffected.
        new TransactionTemplate(primaryRecording).execute(status -> {
            jdbc.update("INSERT INTO events VALUES (200, 'outer')");
            service.insertRequiresNewAndErr(201);
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
        assertThatThrownBy(() -> service.insertMandatoryAndOk(1))
            .isInstanceOf(IllegalTransactionStateException.class);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void mandatory_participatesInActiveTx() {
        new TransactionTemplate(primaryRecording).execute(status -> {
            var result = service.insertMandatoryAndOk(1);
            assertThat(result).isOk();
            return null;
        });
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    // ── Propagation.NEVER ────────────────────────────────────────────────────

    @Test
    void never_throwsWhenActiveTxPresent() {
        // The thrown IllegalTransactionStateException propagates through the outer
        // template, which rolls the outer tx back before rethrowing.
        assertThatThrownBy(() ->
            new TransactionTemplate(primaryRecording).execute(status -> {
                service.insertNeverAndOk(1);
                return null;
            })
        ).isInstanceOf(IllegalTransactionStateException.class);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void never_runsWithoutTx_whenNoActiveTx() {
        // No outer tx: method executes and the insert auto-commits.
        var result = service.insertNeverAndOk(1);
        assertThat(result).isOk();
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    // ── Propagation.SUPPORTS ─────────────────────────────────────────────────

    @Test
    void supports_withoutActiveTx_errDoesNotRollBack() {
        // Without an outer tx the method runs on an auto-commit connection.
        // An error return has nothing to roll back, so the insert persists.
        var result = service.insertSupportsAndErr(1);
        assertThat(result).isErr();
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void supports_withinActiveTx_errMarksGlobalRollbackOnly() {
        // Within an outer tx, SUPPORTS behaves like REQUIRED: the inner error marks
        // the shared tx globally rollback-only, causing UnexpectedRollbackException.
        assertThatThrownBy(() ->
            new TransactionTemplate(primaryRecording).execute(status -> {
                service.insertSupportsAndErr(1);
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
            service.insertNotSupportedAndErr(301);
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
        Result<Integer, String> insertRequiredAndOk(int id);
        Result<Integer, String> insertRequiredAndErr(int id);
        Result<Integer, String> insertRequiresNewAndOk(int id);
        Result<Integer, String> insertRequiresNewAndErr(int id);
        Result<Integer, String> insertMandatoryAndOk(int id);
        Result<Integer, String> insertNeverAndOk(int id);
        Result<Integer, String> insertSupportsAndErr(int id);
        Result<Integer, String> insertNotSupportedAndErr(int id);
    }

    static class ContractServiceImpl implements ContractService {
        private final JdbcTemplate jdbc;

        ContractServiceImpl(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        @TransactionalResult
        public Result<Integer, String> insertRequiredAndOk(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'req-ok')", id);
            return Result.ok(id);
        }

        @Override
        @TransactionalResult
        public Result<Integer, String> insertRequiredAndErr(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'req-err')", id);
            return Result.err("err");
        }

        @Override
        @TransactionalResult(propagation = Propagation.REQUIRES_NEW)
        public Result<Integer, String> insertRequiresNewAndOk(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'new-ok')", id);
            return Result.ok(id);
        }

        @Override
        @TransactionalResult(propagation = Propagation.REQUIRES_NEW)
        public Result<Integer, String> insertRequiresNewAndErr(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'new-err')", id);
            return Result.err("err");
        }

        @Override
        @TransactionalResult(propagation = Propagation.MANDATORY)
        public Result<Integer, String> insertMandatoryAndOk(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'mand-ok')", id);
            return Result.ok(id);
        }

        @Override
        @TransactionalResult(propagation = Propagation.NEVER)
        public Result<Integer, String> insertNeverAndOk(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'never-ok')", id);
            return Result.ok(id);
        }

        @Override
        @TransactionalResult(propagation = Propagation.SUPPORTS)
        public Result<Integer, String> insertSupportsAndErr(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'supp-err')", id);
            return Result.err("err");
        }

        @Override
        @TransactionalResult(propagation = Propagation.NOT_SUPPORTED)
        public Result<Integer, String> insertNotSupportedAndErr(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'notsup-err')", id);
            return Result.err("err");
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
