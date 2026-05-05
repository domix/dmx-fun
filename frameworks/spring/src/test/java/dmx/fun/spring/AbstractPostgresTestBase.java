package dmx.fun.spring;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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

@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractPostgresTestBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.6-alpine");

    static AnnotationConfigApplicationContext ctx;
    static JdbcTemplate jdbc;

    static void initContext(Class<?> serviceConfig) {
        ctx = new AnnotationConfigApplicationContext(BaseConfig.class, serviceConfig);
        jdbc = ctx.getBean(JdbcTemplate.class);
    }

    @BeforeEach
    void truncate() {
        jdbc.execute("TRUNCATE TABLE events");
    }

    @AfterAll
    static void stopContext() {
        ctx.close();
    }

    int countRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class BaseConfig {

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
    }
}
