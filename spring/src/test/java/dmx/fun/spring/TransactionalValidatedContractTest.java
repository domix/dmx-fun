package dmx.fun.spring;

import dmx.fun.Validated;
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
 * Contract tests verifying that {@link TransactionalValidated} propagation settings
 * behave consistently with Spring's {@link org.springframework.transaction.annotation.Transactional}
 * contract.
 *
 * <p>Each propagation mode is exercised in both its success and failure paths to confirm
 * that commit/rollback semantics match Spring's documented behaviour:
 * <ul>
 *   <li>{@code REQUIRED} — joins an existing outer transaction; inner invalid result
 *       marks the shared transaction globally rollback-only, causing the outer commit to
 *       throw {@link org.springframework.transaction.UnexpectedRollbackException}.</li>
 *   <li>{@code REQUIRES_NEW} — suspends the outer transaction and starts an independent
 *       one; commit/rollback of the inner transaction does not affect the outer.</li>
 *   <li>{@code MANDATORY} — throws {@link IllegalTransactionStateException} when no
 *       active transaction is present.</li>
 *   <li>{@code NEVER} — throws {@link IllegalTransactionStateException} when an active
 *       transaction is present.</li>
 *   <li>{@code SUPPORTS} — runs non-transactionally (auto-commit) when no outer tx is
 *       present; an invalid return cannot roll back what was already auto-committed.</li>
 *   <li>{@code NOT_SUPPORTED} — suspends the outer transaction; the inner method runs
 *       without a transaction (auto-commit), so its work persists regardless of what
 *       the outer transaction does.</li>
 * </ul>
 */
class TransactionalValidatedContractTest extends AbstractH2AspectTestBase {

    static ContractService service;

    @BeforeAll
    static void startContext() {
        initContext(ServiceConfig.class);
        service = ctx.getBean(ContractService.class);
    }

    // ── Propagation.REQUIRED ─────────────────────────────────────────────────

    @Test
    void required_joinsOuterTx_innerInvalid_causesUnexpectedRollbackOnOuterCommit() {
        assertThatThrownBy(() ->
            new TransactionTemplate(primaryRecording).execute(status -> {
                service.insertRequiredAndInvalid(1);
                return null;
            })
        ).isInstanceOf(UnexpectedRollbackException.class);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void required_joinsOuterTx_outerRollback_undoesInnerWork() {
        new TransactionTemplate(primaryRecording).execute(status -> {
            service.insertRequiredAndValid(1);
            status.setRollbackOnly();
            return null;
        });
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    // ── Propagation.REQUIRES_NEW ─────────────────────────────────────────────

    @Test
    void requiresNew_innerValid_commitsImmediately_outerRollbackDoesNotUndoInner() {
        new TransactionTemplate(primaryRecording).execute(status -> {
            jdbc.update("INSERT INTO events VALUES (100, 'outer')");
            service.insertRequiresNewAndValid(101);
            status.setRollbackOnly();
            return null;
        });
        Assertions.assertThat(countRows()).isEqualTo(1);
        Assertions.assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM events WHERE id = 101", Integer.class)
        ).isEqualTo(1);
    }

    @Test
    void requiresNew_innerInvalid_rollsBackInnerOnly_outerCommitsItsWork() {
        new TransactionTemplate(primaryRecording).execute(status -> {
            jdbc.update("INSERT INTO events VALUES (200, 'outer')");
            service.insertRequiresNewAndInvalid(201);
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
        assertThatThrownBy(() -> service.insertMandatoryAndValid(1))
            .isInstanceOf(IllegalTransactionStateException.class);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void mandatory_participatesInActiveTx() {
        new TransactionTemplate(primaryRecording).execute(status -> {
            var result = service.insertMandatoryAndValid(1);
            assertThat(result).isValid();
            return null;
        });
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    // ── Propagation.NEVER ────────────────────────────────────────────────────

    @Test
    void never_throwsWhenActiveTxPresent() {
        assertThatThrownBy(() ->
            new TransactionTemplate(primaryRecording).execute(status -> {
                service.insertNeverAndValid(1);
                return null;
            })
        ).isInstanceOf(IllegalTransactionStateException.class);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void never_runsWithoutTx_whenNoActiveTx() {
        var result = service.insertNeverAndValid(1);
        assertThat(result).isValid();
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    // ── Propagation.SUPPORTS ─────────────────────────────────────────────────

    @Test
    void supports_withoutActiveTx_invalidDoesNotRollBack() {
        // Without an outer tx the method runs on an auto-commit connection.
        // An invalid return has nothing to roll back, so the insert persists.
        var result = service.insertSupportsAndInvalid(1);
        assertThat(result).isInvalid();
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void supports_withinActiveTx_invalidMarksGlobalRollbackOnly() {
        // Within an outer tx, SUPPORTS behaves like REQUIRED: the inner invalid result
        // marks the shared tx globally rollback-only, causing UnexpectedRollbackException.
        assertThatThrownBy(() ->
            new TransactionTemplate(primaryRecording).execute(status -> {
                service.insertSupportsAndInvalid(1);
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
            service.insertNotSupportedAndInvalid(301);
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
        Validated<String, Integer> insertRequiredAndValid(int id);
        Validated<String, Integer> insertRequiredAndInvalid(int id);
        Validated<String, Integer> insertRequiresNewAndValid(int id);
        Validated<String, Integer> insertRequiresNewAndInvalid(int id);
        Validated<String, Integer> insertMandatoryAndValid(int id);
        Validated<String, Integer> insertNeverAndValid(int id);
        Validated<String, Integer> insertSupportsAndInvalid(int id);
        Validated<String, Integer> insertNotSupportedAndInvalid(int id);
    }

    static class ContractServiceImpl implements ContractService {
        private final JdbcTemplate jdbc;

        ContractServiceImpl(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        @TransactionalValidated
        public Validated<String, Integer> insertRequiredAndValid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'req-ok')", id);
            return Validated.valid(id);
        }

        @Override
        @TransactionalValidated
        public Validated<String, Integer> insertRequiredAndInvalid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'req-inv')", id);
            return Validated.invalid("invalid");
        }

        @Override
        @TransactionalValidated(propagation = Propagation.REQUIRES_NEW)
        public Validated<String, Integer> insertRequiresNewAndValid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'new-ok')", id);
            return Validated.valid(id);
        }

        @Override
        @TransactionalValidated(propagation = Propagation.REQUIRES_NEW)
        public Validated<String, Integer> insertRequiresNewAndInvalid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'new-inv')", id);
            return Validated.invalid("invalid");
        }

        @Override
        @TransactionalValidated(propagation = Propagation.MANDATORY)
        public Validated<String, Integer> insertMandatoryAndValid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'mand-ok')", id);
            return Validated.valid(id);
        }

        @Override
        @TransactionalValidated(propagation = Propagation.NEVER)
        public Validated<String, Integer> insertNeverAndValid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'never-ok')", id);
            return Validated.valid(id);
        }

        @Override
        @TransactionalValidated(propagation = Propagation.SUPPORTS)
        public Validated<String, Integer> insertSupportsAndInvalid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'supp-inv')", id);
            return Validated.invalid("invalid");
        }

        @Override
        @TransactionalValidated(propagation = Propagation.NOT_SUPPORTED)
        public Validated<String, Integer> insertNotSupportedAndInvalid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'notsup-inv')", id);
            return Validated.invalid("invalid");
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
