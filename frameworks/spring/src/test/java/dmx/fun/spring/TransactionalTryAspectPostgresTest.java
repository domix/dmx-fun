package dmx.fun.spring;

import dmx.fun.Try;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import org.assertj.core.api.Assertions;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates {@link TransactionalTry} commit/rollback behaviour against a real PostgreSQL
 * database via Testcontainers. Tests are skipped automatically when Docker is unavailable.
 */
class TransactionalTryAspectPostgresTest extends AbstractPostgresTestBase {

    static TryService service;

    @BeforeAll
    static void startContext() {
        initContext(ServiceConfig.class);
        service = ctx.getBean(TryService.class);
    }

    @Test
    void onSuccess_commits() {
        var result = service.insertAndSucceed(1);
        assertThat(result).isSuccess();
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onFailure_rollsBack() {
        var result = service.insertAndFail(2);
        assertThat(result).isFailure();
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
    void multipleInserts_allRollBackOnFailure() {
        var result = service.multiInsertAndFail();
        assertThat(result).isFailure();
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Test service
    // -------------------------------------------------------------------------

    interface TryService {
        Try<Integer> insertAndSucceed(int id);
        Try<Integer> insertAndFail(int id);
        Try<Integer> insertAndThrow(int id);
        Try<Integer> multiInsertAndFail();
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
        public Try<Integer> insertAndFail(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'fail')", id);
            return Try.failure(new RuntimeException("domain-failure"));
        }

        @Override
        @TransactionalTry
        public Try<Integer> insertAndThrow(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'throw')", id);
            throw new RuntimeException("boom");
        }

        @Override
        @TransactionalTry
        public Try<Integer> multiInsertAndFail() {
            jdbc.update("INSERT INTO events VALUES (10, 'a')");
            jdbc.update("INSERT INTO events VALUES (11, 'b')");
            return Try.failure(new RuntimeException("partial"));
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
