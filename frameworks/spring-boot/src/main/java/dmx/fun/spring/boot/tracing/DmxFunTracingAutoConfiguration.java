package dmx.fun.spring.boot.tracing;

import dmx.fun.tracing.DmxTracing;
import io.micrometer.tracing.Tracer;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for {@link DmxTracing}.
 *
 * <p>Registers a {@link DmxTracing} bean bound to the application's {@link Tracer} when:
 * <ul>
 *   <li>both {@code micrometer-tracing} and {@code fun-tracing} are on the classpath, and</li>
 *   <li>a {@link Tracer} bean is present in the context (Spring Boot registers one
 *       automatically when a Micrometer Tracing bridge is on the classpath).</li>
 * </ul>
 *
 * <p>All beans are guarded by {@link ConditionalOnMissingBean} so application code can
 * override them with a custom {@code @Bean} declaration. The bean can also be disabled
 * via {@code dmx.fun.tracing.enabled=false} in {@code application.properties}.
 *
 * @see DmxTracing
 */
@AutoConfiguration
@ConditionalOnClass({Tracer.class, DmxTracing.class})
@ConditionalOnBean(Tracer.class)
@NullMarked
public class DmxFunTracingAutoConfiguration {

    /** Default constructor required for Spring Boot auto-configuration instantiation. */
    public DmxFunTracingAutoConfiguration() {}

    /**
     * Registers a {@link DmxTracing} bean bound to the application's {@link Tracer}.
     * Can be disabled via {@code dmx.fun.tracing.enabled=false}.
     *
     * @param tracer the tracer to open spans with
     * @return a ready-to-use {@link DmxTracing} instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "dmx.fun.tracing.enabled", havingValue = "true", matchIfMissing = true)
    public DmxTracing dmxTracing(Tracer tracer) {
        return DmxTracing.of(tracer);
    }
}
