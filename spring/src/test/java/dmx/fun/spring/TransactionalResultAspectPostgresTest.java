package dmx.fun.spring;

import dmx.fun.Result;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import org.assertj.core.api.Assertions;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates {@link TransactionalResult} commit/rollback behaviour against a real PostgreSQL
 * database via Testcontainers. Tests are skipped automatically when Docker is unavailable.
 */
class TransactionalResultAspectPostgresTest extends AbstractPostgresTestBase {

    static ResultService service;

    @BeforeAll
    static void startContext() {
        initContext(ServiceConfig.class);
        service = ctx.getBean(ResultService.class);
    }

    @Test
    void onOk_commits() {
        var result = service.insertAndOk(1);
        assertThat(result).isOk();
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onErr_rollsBack() {
        var result = service.insertAndErr(2);
        assertThat(result).isErr();
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
    void multipleInserts_allRollBackOnErr() {
        var result = service.multiInsertAndErr();
        assertThat(result).isErr();
        Assertions.assertThat(countRows()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Test service
    // -------------------------------------------------------------------------

    interface ResultService {
        Result<Integer, String> insertAndOk(int id);
        Result<Integer, String> insertAndErr(int id);
        Result<Integer, String> insertAndThrow(int id);
        Result<Integer, String> multiInsertAndErr();
    }

    static class ResultServiceImpl implements ResultService {
        private final JdbcTemplate jdbc;

        ResultServiceImpl(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        @TransactionalResult
        public Result<Integer, String> insertAndOk(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'ok')", id);
            return Result.ok(id);
        }

        @Override
        @TransactionalResult
        public Result<Integer, String> insertAndErr(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'err')", id);
            return Result.err("domain-error");
        }

        @Override
        @TransactionalResult
        public Result<Integer, String> insertAndThrow(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'throw')", id);
            throw new RuntimeException("boom");
        }

        @Override
        @TransactionalResult
        public Result<Integer, String> multiInsertAndErr() {
            jdbc.update("INSERT INTO events VALUES (10, 'a')");
            jdbc.update("INSERT INTO events VALUES (11, 'b')");
            return Result.err("partial");
        }
    }

    // -------------------------------------------------------------------------
    // Service configuration
    // -------------------------------------------------------------------------

    @Configuration
    static class ServiceConfig {
        @Bean
        ResultServiceImpl resultService(JdbcTemplate jdbcTemplate) {
            return new ResultServiceImpl(jdbcTemplate);
        }
    }
}
