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

class TransactionalTryAspectTest {

    static AnnotationConfigApplicationContext ctx;
    static JdbcTemplate jdbc;
    static TryService service;
    static RecordingTxManager recording;

    @BeforeAll
    static void startContext() {
        ctx = new AnnotationConfigApplicationContext(TestConfig.class);
        jdbc = ctx.getBean(JdbcTemplate.class);
        service = ctx.getBean(TryService.class);
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
    void onSuccess_commits() {
        var result = service.insertAndSucceed(1);
        assertThat(result).isSuccess().containsValue(1);
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void onFailure_rollsBack() {
        var ex = new RuntimeException("domain-failure");
        var result = service.insertAndFail(2, ex);
        assertThat(result).isFailure();
        assertThat(result.getCause()).isSameAs(ex);
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
    void namedTransactionManager_onSuccess_commits() {
        var result = service.insertWithNamedTxManager(5);
        assertThat(result).isSuccess();
        assertThat(countRows()).isEqualTo(1);
        assertThat(recording.wasUsed()).isTrue();
    }

    @Test
    void namedTransactionManager_onFailure_rollsBack() {
        var result = service.insertWithNamedTxManagerAndFail(6);
        assertThat(result).isFailure();
        assertThat(countRows()).isEqualTo(0);
        assertThat(recording.wasUsed()).isTrue();
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
        TryServiceImpl tryService(JdbcTemplate jdbcTemplate) {
            return new TryServiceImpl(jdbcTemplate);
        }
    }

}
