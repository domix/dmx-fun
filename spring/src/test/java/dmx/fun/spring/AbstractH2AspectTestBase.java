package dmx.fun.spring;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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

abstract class AbstractH2AspectTestBase {

    static AnnotationConfigApplicationContext ctx;
    static JdbcTemplate jdbc;
    static RecordingTxManager primaryRecording;
    static RecordingTxManager secondaryRecording;

    static void initContext(Class<?> serviceConfig) {
        ctx = new AnnotationConfigApplicationContext(BaseConfig.class, serviceConfig);
        jdbc = ctx.getBean(JdbcTemplate.class);
        primaryRecording = ctx.getBean("txManager", RecordingTxManager.class);
        secondaryRecording = ctx.getBean("secondaryTxManager", RecordingTxManager.class);
    }

    @BeforeEach
    void reset() {
        jdbc.execute("DELETE FROM events");
        primaryRecording.reset();
        secondaryRecording.reset();
    }

    @AfterAll
    static void stopContext() {
        ctx.close();
    }

    int countRows() {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
        return count != null ? count : 0;
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class BaseConfig {

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
    }
}
