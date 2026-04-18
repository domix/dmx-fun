package dmx.fun.spring;

import dmx.fun.Validated;
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
 * Validates {@link TransactionalValidated} commit/rollback behaviour against a real
 * PostgreSQL database via Testcontainers. Tests are skipped automatically when Docker
 * is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class TransactionalValidatedAspectPostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static AnnotationConfigApplicationContext ctx;
    static JdbcTemplate jdbc;
    static ValidatedService service;

    @BeforeAll
    static void startContext() {
        ctx = new AnnotationConfigApplicationContext(TestConfig.class);
        jdbc = ctx.getBean(JdbcTemplate.class);
        service = ctx.getBean(ValidatedService.class);
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
    void onValid_commits() {
        var result = service.insertAndValid(1);
        assertThat(result).isValid();
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onInvalid_rollsBack() {
        var result = service.insertAndInvalid(2);
        assertThat(result).isInvalid();
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
    void multipleInserts_allRollBackOnInvalid() {
        var result = service.multiInsertAndInvalid();
        assertThat(result).isInvalid();
        assertThat(countRows()).isEqualTo(0);
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
        ValidatedServiceImpl validatedService(JdbcTemplate jdbcTemplate) {
            return new ValidatedServiceImpl(jdbcTemplate);
        }
    }
}
