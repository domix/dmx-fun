package dmx.fun.spring;

import dmx.fun.Validated;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalValidatedAspectTest extends AbstractH2AspectTestBase {

    static ValidatedService service;

    @BeforeAll
    static void startContext() {
        initContext(ServiceConfig.class);
        service = ctx.getBean(ValidatedService.class);
    }

    // -------------------------------------------------------------------------
    // Default transaction manager
    // -------------------------------------------------------------------------

    @Test
    void onValid_commits() {
        var result = service.insertAndValid(1);
        assertThat(result).isValid().containsValue(1);
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onInvalid_rollsBack() {
        var result = service.insertAndInvalid(2);
        assertThat(result).isInvalid().hasError("domain-error");
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
    void onNullReturn_rollsBackAndThrowsNPE() {
        assertThatThrownBy(() -> service.insertAndReturnNull(4))
            .isInstanceOf(NullPointerException.class);
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Named transaction manager
    // -------------------------------------------------------------------------

    @Test
    void namedTransactionManager_onValid_commits() {
        var result = service.insertWithNamedTxManager(5);
        assertThat(result).isValid();
        Assertions.assertThat(countRows()).isEqualTo(1);
        Assertions.assertThat(secondaryRecording.wasUsed()).isTrue();
        Assertions.assertThat(primaryRecording.wasUsed()).isFalse();
    }

    @Test
    void namedTransactionManager_onInvalid_rollsBack() {
        var result = service.insertWithNamedTxManagerAndInvalid(6);
        assertThat(result).isInvalid();
        Assertions.assertThat(countRows()).isEqualTo(0);
        Assertions.assertThat(secondaryRecording.wasUsed()).isTrue();
        Assertions.assertThat(primaryRecording.wasUsed()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Test service
    // -------------------------------------------------------------------------

    interface ValidatedService {
        Validated<String, Integer> insertAndValid(int id);
        Validated<String, Integer> insertAndInvalid(int id);
        Validated<String, Integer> insertAndThrow(int id);
        Validated<String, Integer> insertAndReturnNull(int id);
        Validated<String, Integer> insertWithNamedTxManager(int id);
        Validated<String, Integer> insertWithNamedTxManagerAndInvalid(int id);
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
        @SuppressWarnings("NullAway")
        public Validated<String, Integer> insertAndReturnNull(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'null')", id);
            return null;
        }

        @Override
        @TransactionalValidated(transactionManager = "secondaryTxManager")
        public Validated<String, Integer> insertWithNamedTxManager(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'named-ok')", id);
            return Validated.valid(id);
        }

        @Override
        @TransactionalValidated(transactionManager = "secondaryTxManager")
        public Validated<String, Integer> insertWithNamedTxManagerAndInvalid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'named-invalid')", id);
            return Validated.invalid("named-error");
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
