package dmx.fun.spring;

import dmx.fun.Try;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalTryAspectTest extends AbstractH2AspectTestBase {

    static TryService service;

    @BeforeAll
    static void startContext() {
        initContext(ServiceConfig.class);
        service = ctx.getBean(TryService.class);
    }

    // -------------------------------------------------------------------------
    // Default transaction manager
    // -------------------------------------------------------------------------

    @Test
    void onSuccess_commits() {
        var result = service.insertAndSucceed(1);
        assertThat(result).isSuccess().containsValue(1);
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onFailure_rollsBack() {
        var ex = new RuntimeException("domain-failure");
        var result = service.insertAndFail(2, ex);
        assertThat(result).isFailure();
        Assertions.assertThat(result.getCause()).isSameAs(ex);
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
    void namedTransactionManager_onSuccess_commits() {
        var result = service.insertWithNamedTxManager(5);
        assertThat(result).isSuccess();
        Assertions.assertThat(countRows()).isEqualTo(1);
        Assertions.assertThat(secondaryRecording.wasUsed()).isTrue();
        Assertions.assertThat(primaryRecording.wasUsed()).isFalse();
    }

    @Test
    void namedTransactionManager_onFailure_rollsBack() {
        var result = service.insertWithNamedTxManagerAndFail(6);
        assertThat(result).isFailure();
        Assertions.assertThat(countRows()).isEqualTo(0);
        Assertions.assertThat(secondaryRecording.wasUsed()).isTrue();
        Assertions.assertThat(primaryRecording.wasUsed()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Test service
    // -------------------------------------------------------------------------

    interface TryService {
        Try<Integer> insertAndSucceed(int id);
        Try<Integer> insertAndFail(int id, RuntimeException cause);
        Try<Integer> insertAndThrow(int id);
        Try<Integer> insertAndReturnNull(int id);
        Try<Integer> insertWithNamedTxManager(int id);
        Try<Integer> insertWithNamedTxManagerAndFail(int id);
    }

    static class TryServiceImpl implements TryService {
        private final JdbcTemplate jdbc;

        TryServiceImpl(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        @TransactionalTry
        public Try<Integer> insertAndSucceed(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'success')", id);
            return Try.success(id);
        }

        @Override
        @TransactionalTry
        public Try<Integer> insertAndFail(int id, RuntimeException cause) {
            jdbc.update("INSERT INTO events VALUES (?, 'fail')", id);
            return Try.failure(cause);
        }

        @Override
        @TransactionalTry
        public Try<Integer> insertAndThrow(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'throw')", id);
            throw new RuntimeException("boom");
        }

        @Override
        @TransactionalTry
        @SuppressWarnings("NullAway")
        public Try<Integer> insertAndReturnNull(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'null')", id);
            return null;
        }

        @Override
        @TransactionalTry(transactionManager = "secondaryTxManager")
        public Try<Integer> insertWithNamedTxManager(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'named-ok')", id);
            return Try.success(id);
        }

        @Override
        @TransactionalTry(transactionManager = "secondaryTxManager")
        public Try<Integer> insertWithNamedTxManagerAndFail(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'named-fail')", id);
            return Try.failure(new RuntimeException("named-failure"));
        }
    }

    // -------------------------------------------------------------------------
    // Service configuration
    // -------------------------------------------------------------------------

    @Configuration
    static class ServiceConfig {
        @Bean
        TryServiceImpl tryService(JdbcTemplate jdbcTemplate) {
            return new TryServiceImpl(jdbcTemplate);
        }
    }
}
