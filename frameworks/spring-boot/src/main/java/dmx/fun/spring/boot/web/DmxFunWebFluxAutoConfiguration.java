package dmx.fun.spring.boot.web;

import dmx.fun.spring.webflux.ThrowableHttpMapper;
import dmx.fun.spring.webflux.WebfluxProblem;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Spring Boot autoconfiguration for {@code fun-spring-webflux}.
 *
 * <p>{@code WebfluxFun}/{@code WebfluxProblem} are static utilities, so there are no adapters to
 * register. What this contributes is a ready-made default: a {@link ThrowableHttpMapper} bean
 * that renders any failure as an RFC 7807 {@code application/problem+json} response, so reactive
 * handlers can inject one (and pass it to {@code WebfluxFun.fromTry(...)}) instead of constructing
 * it at each call site.
 *
 * <p>The status defaults to {@code 500} and is configurable via
 * {@code dmx.fun.webflux.problem.status}. The bean is guarded by {@code @ConditionalOnMissingBean}
 * (declare your own {@code ThrowableHttpMapper} to override it) and can be disabled with
 * {@code dmx.fun.webflux.problem.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnClass({ServerResponse.class, WebfluxProblem.class})
@NullMarked
public class DmxFunWebFluxAutoConfiguration {

    /** Default constructor required for Spring Boot auto-configuration instantiation. */
    public DmxFunWebFluxAutoConfiguration() {
    }

    /**
     * Registers a default {@link ThrowableHttpMapper} rendering failures as an RFC 7807 problem
     * response with the configured status (default {@code 500}).
     *
     * @param status the HTTP status code for the problem, from {@code dmx.fun.webflux.problem.status};
     *               must be a valid HTTP status code or startup fails with a clear message
     * @return the default problem-detail throwable mapper
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "dmx.fun.webflux.problem.enabled", havingValue = "true", matchIfMissing = true)
    ThrowableHttpMapper dmxFunProblemDetailMapper(
        @Value("${dmx.fun.webflux.problem.status:500}") int status
    ) {
        HttpStatus resolved = HttpStatus.resolve(status);
        if (resolved == null) {
            throw new IllegalStateException(
                "Invalid dmx.fun.webflux.problem.status: " + status + " — must be a valid HTTP status code");
        }
        return WebfluxProblem.problemDetail(resolved);
    }
}
