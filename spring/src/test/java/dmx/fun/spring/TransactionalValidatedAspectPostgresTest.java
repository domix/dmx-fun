package dmx.fun.spring;

import dmx.fun.Validated;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import org.assertj.core.api.Assertions;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates {@link TransactionalValidated} commit/rollback behaviour against a real
 * PostgreSQL database via Testcontainers. Tests are skipped automatically when Docker
 * is unavailable.
 */
class TransactionalValidatedAspectPostgresTest extends AbstractPostgresTestBase {

    static ValidatedService service;

    @BeforeAll
    static void startContext() {
        initContext(ServiceConfig.class);
        service = ctx.getBean(ValidatedService.class);
    }

    @Test
    void onValid_commits() {
        var result = service.insertAndValid(1);
        assertThat(result).isValid();
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onInvalid_rollsBack() {
        var result = service.insertAndInvalid(2);
        assertThat(result).isInvalid();
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void onException_rollsBackAndPropagates() {
        assertThatThrownBy(() -> service.insertAndThrow(3))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void multipleInserts_allRollBackOnInvalid() {
        var result = service.multiInsertAndInvalid();
        assertThat(result).isInvalid();
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Test service
    // -------------------------------------------------------------------------

    interface ValidatedService {
        Validated<String, Integer> insertAndValid(int id);
        Validated<String, Integer> insertAndInvalid(int id);
        Validated<String, Integer> insertAndThrow(int id);
        Validated<String, Integer> multiInsertAndInvalid();
    }

    static class ValidatedServiceImpl implements ValidatedService {
        private final JdbcTemplate jdbc;

        ValidatedServiceImpl(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        @TransactionalValidated
        public Validated<String, Integer> insertAndValid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'valid')", id);
            return Validated.valid(id);
        }

        @Override
        @TransactionalValidated
        public Validated<String, Integer> insertAndInvalid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'invalid')", id);
            return Validated.invalid("domain-error");
        }

        @Override
        @TransactionalValidated
        public Validated<String, Integer> insertAndThrow(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'throw')", id);
            throw new RuntimeException("boom");
        }

        @Override
        @TransactionalValidated
        public Validated<String, Integer> multiInsertAndInvalid() {
            jdbc.update("INSERT INTO events VALUES (10, 'a')");
            jdbc.update("INSERT INTO events VALUES (11, 'b')");
            return Validated.invalid("partial");
        }
    }

    // -------------------------------------------------------------------------
    // Service configuration
    // -------------------------------------------------------------------------

    @Configuration
    static class ServiceConfig {
        @Bean
        ValidatedServiceImpl validatedService(JdbcTemplate jdbcTemplate) {
            return new ValidatedServiceImpl(jdbcTemplate);
        }
    }
}
