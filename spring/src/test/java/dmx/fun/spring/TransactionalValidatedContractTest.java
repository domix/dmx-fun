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
 * <p>Covers the subset of propagation modes that are most likely to vary in
 * behaviour for {@code Validated}-returning methods:
 * <ul>
 *   <li>{@code REQUIRED} — joins an existing outer transaction; inner invalid result
 *       marks the shared transaction globally rollback-only.</li>
 *   <li>{@code REQUIRES_NEW} — independent inner transaction; commit/rollback does
 *       not affect the outer.</li>
 *   <li>{@code MANDATORY} — throws {@link IllegalTransactionStateException} when no
 *       active transaction is present.</li>
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

    // ── Service ──────────────────────────────────────────────────────────────

    interface ContractService {
        Validated<String, Integer> insertRequiredAndValid(int id);
        Validated<String, Integer> insertRequiredAndInvalid(int id);
        Validated<String, Integer> insertRequiresNewAndValid(int id);
        Validated<String, Integer> insertRequiresNewAndInvalid(int id);
        Validated<String, Integer> insertMandatoryAndValid(int id);
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
    }

    @Configuration
    static class ServiceConfig {
        @Bean
        ContractServiceImpl contractService(JdbcTemplate jdbcTemplate) {
            return new ContractServiceImpl(jdbcTemplate);
        }
    }
}
