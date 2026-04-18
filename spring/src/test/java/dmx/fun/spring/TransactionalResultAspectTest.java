package dmx.fun.spring;

import dmx.fun.Result;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalResultAspectTest {

    static AnnotationConfigApplicationContext ctx;
    static JdbcTemplate jdbc;
    static ResultService service;
    static RecordingTxManager primaryRecording;
    static RecordingTxManager recording;

    @BeforeAll
    static void startContext() {
        ctx = new AnnotationConfigApplicationContext(TestConfig.class);
        jdbc = ctx.getBean(JdbcTemplate.class);
        service = ctx.getBean(ResultService.class);
        primaryRecording = ctx.getBean("txManager", RecordingTxManager.class);
        recording = ctx.getBean("secondaryTxManager", RecordingTxManager.class);
    }

    @BeforeEach
    void clearTable() {
        jdbc.execute("DELETE FROM events");
        primaryRecording.reset();
        recording.reset();
    }

    @AfterAll
    static void stopContext() {
        ctx.close();
    }

    private int countRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
    }

    // -------------------------------------------------------------------------
    // Default transaction manager
    // -------------------------------------------------------------------------

    @Test
    void onOk_commits() {
        var result = service.insertAndOk(1);
        assertThat(result).isOk().containsValue(1);
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onErr_rollsBack() {
        var result = service.insertAndErr(2);
        assertThat(result).isErr().containsError("domain-error");
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void onException_rollsBackAndPropagates() {
        assertThatThrownBy(() -> service.insertAndThrow(3))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void onNullReturn_rollsBackAndThrowsNPE() {
        assertThatThrownBy(() -> service.insertAndReturnNull(4))
            .isInstanceOf(NullPointerException.class);
        assertThat(countRows()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Named transaction manager
    // -------------------------------------------------------------------------

    @Test
    void namedTransactionManager_onOk_commits() {
        var result = service.insertWithNamedTxManager(5);
        assertThat(result).isOk();
        assertThat(countRows()).isEqualTo(1);
        assertThat(recording.wasUsed()).isTrue();
        assertThat(primaryRecording.wasUsed()).isFalse();
    }

    @Test
    void namedTransactionManager_onErr_rollsBack() {
        var result = service.insertWithNamedTxManagerAndErr(6);
        assertThat(result).isErr();
        assertThat(countRows()).isEqualTo(0);
        assertThat(recording.wasUsed()).isTrue();
        assertThat(primaryRecording.wasUsed()).isFalse();
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
    }

    // -------------------------------------------------------------------------
    // Spring configuration
    // -------------------------------------------------------------------------

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        EmbeddedDatabase dataSource() {
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
        }

        @Bean
        JdbcTemplate jdbcTemplate(EmbeddedDatabase ds) {
            var tmpl = new JdbcTemplate(ds);
            tmpl.execute("CREATE TABLE events (id INT PRIMARY KEY, label VARCHAR(255))");
            return tmpl;
        }

        @Primary
        @Bean
        RecordingTxManager txManager(EmbeddedDatabase ds) {
            return new RecordingTxManager(new DataSourceTransactionManager(ds));
        }

        @Bean
        RecordingTxManager secondaryTxManager(EmbeddedDatabase ds) {
            return new RecordingTxManager(new DataSourceTransactionManager(ds));
        }

        @Bean
        DmxTransactionalAspect dmxTransactionalAspect(
                PlatformTransactionManager txManager, BeanFactory beanFactory) {
            return new DmxTransactionalAspect(txManager, beanFactory);
        }

        @Bean
        ResultServiceImpl resultService(JdbcTemplate jdbcTemplate) {
            return new ResultServiceImpl(jdbcTemplate);
        }
    }

}
