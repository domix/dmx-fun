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

class TransactionalValidatedAspectTest {

    static AnnotationConfigApplicationContext ctx;
    static JdbcTemplate jdbc;
    static ValidatedService service;
    static RecordingTxManager recording;

    @BeforeAll
    static void startContext() {
        ctx = new AnnotationConfigApplicationContext(TestConfig.class);
        jdbc = ctx.getBean(JdbcTemplate.class);
        service = ctx.getBean(ValidatedService.class);
        recording = ctx.getBean("secondaryTxManager", RecordingTxManager.class);
    }

    @BeforeEach
    void clearTable() {
        jdbc.execute("DELETE FROM events");
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
    void onValid_commits() {
        var result = service.insertAndValid(1);
        assertThat(result).isValid().containsValue(1);
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onInvalid_rollsBack() {
        var result = service.insertAndInvalid(2);
        assertThat(result).isInvalid().hasError("domain-error");
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
    void namedTransactionManager_onValid_commits() {
        var result = service.insertWithNamedTxManager(5);
        assertThat(result).isValid();
        assertThat(countRows()).isEqualTo(1);
        assertThat(recording.wasUsed()).isTrue();
    }

    @Test
    void namedTransactionManager_onInvalid_rollsBack() {
        var result = service.insertWithNamedTxManagerAndInvalid(6);
        assertThat(result).isInvalid();
        assertThat(countRows()).isEqualTo(0);
        assertThat(recording.wasUsed()).isTrue();
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
        DataSourceTransactionManager txManager(EmbeddedDatabase ds) {
            return new DataSourceTransactionManager(ds);
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
        ValidatedServiceImpl validatedService(JdbcTemplate jdbcTemplate) {
            return new ValidatedServiceImpl(jdbcTemplate);
        }
    }

}
