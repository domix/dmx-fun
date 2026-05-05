package dmx.fun.spring.boot;

import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import dmx.fun.spring.TransactionalResult;
import dmx.fun.spring.TransactionalTry;
import dmx.fun.spring.TransactionalValidated;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the auto-configured {@link dmx.fun.spring.DmxTransactionalAspect}
 * in a Spring Boot context.
 *
 * <p>Verifies that {@code @TransactionalResult}, {@code @TransactionalTry}, and
 * {@code @TransactionalValidated} annotations are intercepted by the auto-configured aspect
 * and that the correct commit/rollback semantics are applied for each outcome.
 *
 * <p>Also documents the proxy self-call limitation: a method that calls another
 * annotated method within the same bean bypasses the AOP proxy, so the inner
 * annotation has no effect and the INSERT auto-commits regardless of the returned
 * value.
 *
 * @see DmxFunTransactionBootIntegrationTest
 */
class DmxFunAspectBootIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DmxFunSpringAutoConfiguration.class))
        .withUserConfiguration(H2Config.class, EventServiceConfig.class);

    // ── @TransactionalResult ─────────────────────────────────────────────────

    @Test
    void transactionalResult_ok_commits() {
        runner.run(ctx -> {
            var service = ctx.getBean(EventService.class);
            var jdbc    = ctx.getBean(JdbcTemplate.class);

            service.insert(1);

            assertThat(rowCount(jdbc)).isEqualTo(1);
        });
    }

    @Test
    void transactionalResult_error_rollsBack() {
        runner.run(ctx -> {
            var service = ctx.getBean(EventService.class);
            var jdbc    = ctx.getBean(JdbcTemplate.class);

            service.insertAndFail(1);

            assertThat(rowCount(jdbc)).isEqualTo(0);
        });
    }

    @Test
    void transactionalResult_exception_rollsBackAndPropagates() {
        runner.run(ctx -> {
            var service = ctx.getBean(EventService.class);
            var jdbc    = ctx.getBean(JdbcTemplate.class);

            assertThatThrownBy(() -> service.insertAndThrow(1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("unexpected");

            assertThat(rowCount(jdbc)).isEqualTo(0);
        });
    }

    // ── @TransactionalTry ─────────────────────────────────────────────────────

    @Test
    void transactionalTry_success_commits() {
        runner.run(ctx -> {
            var service = ctx.getBean(EventService.class);
            var jdbc    = ctx.getBean(JdbcTemplate.class);

            service.insertAsTry(1);

            assertThat(rowCount(jdbc)).isEqualTo(1);
        });
    }

    @Test
    void transactionalTry_failure_rollsBack() {
        runner.run(ctx -> {
            var service = ctx.getBean(EventService.class);
            var jdbc    = ctx.getBean(JdbcTemplate.class);

            service.insertAsTryFailure(1);

            assertThat(rowCount(jdbc)).isEqualTo(0);
        });
    }

    // ── @TransactionalValidated ───────────────────────────────────────────────

    @Test
    void transactionalValidated_valid_commits() {
        runner.run(ctx -> {
            var service = ctx.getBean(EventService.class);
            var jdbc    = ctx.getBean(JdbcTemplate.class);

            service.insertAsValidated(1);

            assertThat(rowCount(jdbc)).isEqualTo(1);
        });
    }

    @Test
    void transactionalValidated_invalid_rollsBack() {
        runner.run(ctx -> {
            var service = ctx.getBean(EventService.class);
            var jdbc    = ctx.getBean(JdbcTemplate.class);

            service.insertAsValidatedInvalid(1);

            assertThat(rowCount(jdbc)).isEqualTo(0);
        });
    }

    // ── Proxy self-call boundary ──────────────────────────────────────────────

    @Test
    void selfCall_doesNotIntercept_insertAutoCommits() {
        // Spring AOP proxies only intercept calls coming in from outside the bean.
        // When a method calls another @TransactionalResult method on the same instance
        // ('this.insertAndFail(...)'), the call bypasses the proxy and the aspect is
        // never invoked. The INSERT runs without a managed transaction and auto-commits,
        // so the row is present even though the returned Result is an error.
        runner.run(ctx -> {
            var service = ctx.getBean(EventService.class);
            var jdbc    = ctx.getBean(JdbcTemplate.class);

            var result = service.selfCallInsertAndFail(1);

            assertThat(result.isError()).isTrue();
            assertThat(rowCount(jdbc)).isEqualTo(1);  // not rolled back — proxy bypassed
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int rowCount(JdbcTemplate jdbc) {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
        return count != null ? count : 0;
    }

    // ── Test configurations ───────────────────────────────────────────────────

    @Configuration
    static class H2Config {

        @Bean(destroyMethod = "shutdown")
        EmbeddedDatabase dataSource() {
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
        }

        @Bean
        JdbcTemplate jdbcTemplate(EmbeddedDatabase ds) {
            var jdbc = new JdbcTemplate(ds);
            jdbc.execute("CREATE TABLE events (id INT PRIMARY KEY, label VARCHAR(255))");
            return jdbc;
        }

        @Bean
        PlatformTransactionManager txManager(EmbeddedDatabase ds) {
            return new DataSourceTransactionManager(ds);
        }
    }

    @Configuration
    static class EventServiceConfig {
        @Bean
        EventService eventService(JdbcTemplate jdbc) {
            return new EventService(jdbc);
        }
    }

    // ── Service under test ────────────────────────────────────────────────────

    static class EventService {

        private final JdbcTemplate jdbc;

        EventService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

        @TransactionalResult
        public Result<String, String> insert(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'label')", id);
            return Result.ok("inserted");
        }

        @TransactionalResult
        public Result<String, String> insertAndFail(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'label')", id);
            return Result.err("intentional failure");
        }

        @TransactionalResult
        public Result<String, String> insertAndThrow(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'label')", id);
            throw new RuntimeException("unexpected");
        }

        @TransactionalTry
        public Try<String> insertAsTry(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'label')", id);
            return Try.success("inserted");
        }

        @TransactionalTry
        public Try<String> insertAsTryFailure(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'label')", id);
            return Try.failure(new RuntimeException("caught failure"));
        }

        @TransactionalValidated
        public Validated<String, String> insertAsValidated(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'label')", id);
            return Validated.valid("inserted");
        }

        @TransactionalValidated
        public Validated<String, String> insertAsValidatedInvalid(int id) {
            jdbc.update("INSERT INTO events VALUES (?, 'label')", id);
            return Validated.invalid("intentional invalid");
        }

        // No annotation here. Calls annotated insertAndFail directly on 'this',
        // which bypasses the proxy — the aspect is never invoked.
        public Result<String, String> selfCallInsertAndFail(int id) {
            return insertAndFail(id);
        }
    }
}
