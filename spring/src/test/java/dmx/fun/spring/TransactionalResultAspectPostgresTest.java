package dmx.fun.spring;

import dmx.fun.Result;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates {@link TransactionalResult} commit/rollback behaviour against a real PostgreSQL
 * database via Testcontainers. Tests are skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class TransactionalResultAspectPostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.6-alpine");

    static AnnotationConfigApplicationContext ctx;
    static JdbcTemplate jdbc;
    static ResultService service;

    @BeforeAll
    static void startContext() {
        ctx = new AnnotationConfigApplicationContext(TestConfig.class);
        jdbc = ctx.getBean(JdbcTemplate.class);
        service = ctx.getBean(ResultService.class);
    }

    @BeforeEach
    void truncate() {
        jdbc.execute("TRUNCATE TABLE events");
    }

    @AfterAll
    static void stopContext() {
        ctx.close();
    }

    private int countRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class).intValue();
    }

    @Test
    void onOk_commits() {
        var result = service.insertAndOk(1);
        assertThat(result).isOk();
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onErr_rollsBack() {
        var result = service.insertAndErr(2);
        assertThat(result).isErr();
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
    void multipleInserts_allRollBackOnErr() {
        var result = service.multiInsertAndErr();
        assertThat(result).isErr();
        assertThat(countRows()).isEqualTo(0);
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
    // Spring configuration
    // -------------------------------------------------------------------------

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        DriverManagerDataSource dataSource() {
            return new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
        }

        @Bean
        JdbcTemplate jdbcTemplate(DriverManagerDataSource ds) {
            var tmpl = new JdbcTemplate(ds);
            tmpl.execute("CREATE TABLE IF NOT EXISTS events (id INT PRIMARY KEY, label VARCHAR(255))");
            return tmpl;
        }

        @Bean
        DataSourceTransactionManager txManager(DriverManagerDataSource ds) {
            return new DataSourceTransactionManager(ds);
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
