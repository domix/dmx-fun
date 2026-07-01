package dmx.fun.spring.boot.web;

import dmx.fun.spring.webflux.ThrowableHttpMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;

class DmxFunWebFluxAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DmxFunWebFluxAutoConfiguration.class));

    @Test
    void registersDefaultProblemMapper_byDefault() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(ThrowableHttpMapper.class)
            .hasBean("dmxFunProblemDetailMapper"));
    }

    @Test
    void defaultMapper_rendersConfiguredStatus() {
        runner.run(ctx -> {
            ThrowableHttpMapper mapper = ctx.getBean(ThrowableHttpMapper.class);
            ServerResponse response = mapper.apply(new RuntimeException("boom")).block();
            assertThat(response.statusCode().value()).isEqualTo(500);
        });
    }

    @Test
    void status_isConfigurable() {
        runner.withPropertyValues("dmx.fun.webflux.problem.status=404").run(ctx -> {
            ThrowableHttpMapper mapper = ctx.getBean(ThrowableHttpMapper.class);
            ServerResponse response = mapper.apply(new RuntimeException("boom")).block();
            assertThat(response.statusCode().value()).isEqualTo(404);
        });
    }

    @Test
    void disabled_byProperty() {
        runner.withPropertyValues("dmx.fun.webflux.problem.enabled=false")
            .run(ctx -> assertThat(ctx).doesNotHaveBean(ThrowableHttpMapper.class));
    }

    @Test
    void backsOff_whenUserDeclaresOwnMapper() {
        runner.withUserConfiguration(CustomMapperConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(ThrowableHttpMapper.class).hasBean("customMapper");
            assertThat(ctx).doesNotHaveBean("dmxFunProblemDetailMapper");
        });
    }

    @Configuration
    static class CustomMapperConfig {
        @Bean
        ThrowableHttpMapper customMapper() {
            return cause -> ServerResponse.status(HttpStatus.I_AM_A_TEAPOT).build();
        }
    }
}
