package dmx.fun.spring;

import dmx.fun.Try;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
 * Validates {@link TransactionalTry} commit/rollback behaviour against a real PostgreSQL
 * database via Testcontainers. Tests are skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class TransactionalTryAspectPostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.6-alpine");

    static AnnotationConfigApplicationContext ctx;
    static JdbcTemplate jdbc;
    static TryService service;

    @BeforeAll
    static void startContext() {
        ctx = new AnnotationConfigApplicationContext(TestConfig.class);
        jdbc = ctx.getBean(JdbcTemplate.class);
        service = ctx.getBean(TryService.class);
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
        return jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
    }

    @Test
    void onSuccess_commits() {
        var result = service.insertAndSucceed(1);
        assertThat(result).isSuccess();
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onFailure_rollsBack() {
        var result = service.insertAndFail(2);
        assertThat(result).isFailure();
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
    void multipleInserts_allRollBackOnFailure() {
        var result = service.multiInsertAndFail();
        assertThat(result).isFailure();
        assertThat(countRows()).isEqualTo(0);
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
        TryServiceImpl tryService(JdbcTemplate jdbcTemplate) {
            return new TryServiceImpl(jdbcTemplate);
        }
    }
}
