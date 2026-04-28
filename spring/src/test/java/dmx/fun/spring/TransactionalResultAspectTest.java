package dmx.fun.spring;

import dmx.fun.Result;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalResultAspectTest extends AbstractH2AspectTestBase {

    static ResultService service;

    @BeforeAll
    static void startContext() {
        initContext(ServiceConfig.class);
        service = ctx.getBean(ResultService.class);
    }

    // -------------------------------------------------------------------------
    // Default transaction manager
    // -------------------------------------------------------------------------

    @Test
    void onOk_commits() {
        var result = service.insertAndOk(1);
        assertThat(result).isOk().containsValue(1);
        Assertions.assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onErr_rollsBack() {
        var result = service.insertAndErr(2);
        assertThat(result).isErr().containsError("domain-error");
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
    void namedTransactionManager_onOk_commits() {
        var result = service.insertWithNamedTxManager(5);
        assertThat(result).isOk();
        Assertions.assertThat(countRows()).isEqualTo(1);
        Assertions.assertThat(secondaryRecording.wasUsed()).isTrue();
        Assertions.assertThat(primaryRecording.wasUsed()).isFalse();
    }

    @Test
    void namedTransactionManager_onErr_rollsBack() {
        var result = service.insertWithNamedTxManagerAndErr(6);
        assertThat(result).isErr();
        Assertions.assertThat(countRows()).isEqualTo(0);
        Assertions.assertThat(secondaryRecording.wasUsed()).isTrue();
        Assertions.assertThat(primaryRecording.wasUsed()).isFalse();
    }

    // -------------------------------------------------------------------------
    // readOnly flag
    // -------------------------------------------------------------------------

    @Test
    void readOnly_forwardsHintToTransactionManager() {
        service.selectReadOnly(1);
        Assertions.assertThat(primaryRecording.lastDefinition().isReadOnly()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Test service
    // -------------------------------------------------------------------------

    interface ResultService {
        Result<Integer, String> insertAndOk(int id);
        Result<Integer, String> insertAndErr(int id);
        Result<Integer, String> insertAndThrow(int id);
        Result<Integer, String> insertAndReturnNull(int id);
        Result<Integer, String> insertWithNamedTxManager(int id);
        Result<Integer, String> insertWithNamedTxManagerAndErr(int id);
        Result<Integer, String> selectReadOnly(int id);
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
        @SuppressWarnings("NullAway")
        public Result<Integer, String> insertAndReturnNull(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'null')", id);
            return null;
        }

        @Override
        @TransactionalResult(transactionManager = "secondaryTxManager")
        public Result<Integer, String> insertWithNamedTxManager(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'named-ok')", id);
            return Result.ok(id);
        }

        @Override
        @TransactionalResult(transactionManager = "secondaryTxManager")
        public Result<Integer, String> insertWithNamedTxManagerAndErr(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'named-err')", id);
            return Result.err("named-error");
        }

        @Override
        @TransactionalResult(readOnly = true)
        public Result<Integer, String> selectReadOnly(int id) {
            return Result.ok(id);
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
