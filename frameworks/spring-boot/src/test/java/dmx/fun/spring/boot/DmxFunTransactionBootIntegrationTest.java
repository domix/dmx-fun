package dmx.fun.spring.boot;

import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import dmx.fun.spring.TxResult;
import dmx.fun.spring.TxTry;
import dmx.fun.spring.TxValidated;
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
 * End-to-end integration tests for auto-configured {@link TxResult}, {@link TxTry}, and
 * {@link TxValidated} in a Spring Boot context backed by an H2 embedded database.
 *
 * <p>These tests verify commit/rollback semantics of the programmatic transaction helpers
 * as wired by {@link DmxFunSpringAutoConfiguration}. Each scenario uses a fresh H2
 * instance (unique database name) so tests are fully independent.
 *
 * @see DmxFunSpringAutoConfigurationTest
 * @see DmxFunAspectBootIntegrationTest
 */
class DmxFunTransactionBootIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DmxFunSpringAutoConfiguration.class))
        .withUserConfiguration(H2Config.class);

    // ── TxResult ──────────────────────────────────────────────────────────────

    @Test
    void txResult_ok_commits() {
        runner.run(ctx -> {
            var txResult = ctx.getBean(TxResult.class);
            var jdbc     = ctx.getBean(JdbcTemplate.class);

            txResult.execute(() -> {
                jdbc.update("INSERT INTO events VALUES (1, 'a')");
                return Result.ok("done");
            });

            assertThat(rowCount(jdbc)).isEqualTo(1);
        });
    }

    @Test
    void txResult_error_rollsBack() {
        runner.run(ctx -> {
            var txResult = ctx.getBean(TxResult.class);
            var jdbc     = ctx.getBean(JdbcTemplate.class);

            txResult.execute(() -> {
                jdbc.update("INSERT INTO events VALUES (1, 'a')");
                return Result.err("domain failure");
            });

            assertThat(rowCount(jdbc)).isEqualTo(0);
        });
    }

    @Test
    void txResult_exception_rollsBackAndPropagates() {
        runner.run(ctx -> {
            var txResult = ctx.getBean(TxResult.class);
            var jdbc     = ctx.getBean(JdbcTemplate.class);

            assertThatThrownBy(() ->
                txResult.execute(() -> {
                    jdbc.update("INSERT INTO events VALUES (1, 'a')");
                    throw new RuntimeException("unexpected");
                })
            ).isInstanceOf(RuntimeException.class);

            assertThat(rowCount(jdbc)).isEqualTo(0);
        });
    }

    // ── TxTry ─────────────────────────────────────────────────────────────────

    @Test
    void txTry_success_commits() {
        runner.run(ctx -> {
            var txTry = ctx.getBean(TxTry.class);
            var jdbc  = ctx.getBean(JdbcTemplate.class);

            txTry.execute(() -> {
                jdbc.update("INSERT INTO events VALUES (1, 'a')");
                return Try.success("done");
            });

            assertThat(rowCount(jdbc)).isEqualTo(1);
        });
    }

    @Test
    void txTry_failure_rollsBack() {
        runner.run(ctx -> {
            var txTry = ctx.getBean(TxTry.class);
            var jdbc  = ctx.getBean(JdbcTemplate.class);

            txTry.execute(() -> {
                jdbc.update("INSERT INTO events VALUES (1, 'a')");
                return Try.failure(new RuntimeException("caught failure"));
            });

            assertThat(rowCount(jdbc)).isEqualTo(0);
        });
    }

    // ── TxValidated ───────────────────────────────────────────────────────────

    @Test
    void txValidated_valid_commits() {
        runner.run(ctx -> {
            var txValidated = ctx.getBean(TxValidated.class);
            var jdbc        = ctx.getBean(JdbcTemplate.class);

            txValidated.execute(() -> {
                jdbc.update("INSERT INTO events VALUES (1, 'a')");
                return Validated.valid("done");
            });

            assertThat(rowCount(jdbc)).isEqualTo(1);
        });
    }

    @Test
    void txValidated_invalid_rollsBack() {
        runner.run(ctx -> {
            var txValidated = ctx.getBean(TxValidated.class);
            var jdbc        = ctx.getBean(JdbcTemplate.class);

            txValidated.execute(() -> {
                jdbc.update("INSERT INTO events VALUES (1, 'a')");
                return Validated.invalid("validation failed");
            });

            assertThat(rowCount(jdbc)).isEqualTo(0);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int rowCount(JdbcTemplate jdbc) {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
        return count != null ? count : 0;
    }

    // ── Test configuration ────────────────────────────────────────────────────

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
}
