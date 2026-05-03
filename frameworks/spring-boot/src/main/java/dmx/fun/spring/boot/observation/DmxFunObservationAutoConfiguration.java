package dmx.fun.spring.boot.observation;

import dmx.fun.observation.DmxObservation;
import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot autoconfiguration for {@link DmxObservation}.
 *
 * <p>Registers a {@link DmxObservation} bean bound to the application's
 * {@link ObservationRegistry} when:
 * <ul>
 *   <li>both {@code micrometer-core} and {@code fun-observation} are on the classpath, and</li>
 *   <li>an {@link ObservationRegistry} bean is present in the context (Spring Boot 3.0+
 *       registers one automatically when {@code micrometer-core} is on the classpath).</li>
 * </ul>
 *
 * <p>The registered {@code DmxObservation} delivers both metrics and distributed
 * tracing spans in a single call, depending on the handlers wired into the
 * {@link ObservationRegistry} (Spring Boot wires both when a Micrometer Tracing
 * bridge is also present).
 *
 * <p>All beans are guarded by {@link ConditionalOnMissingBean} so application code can
 * override them with a custom {@code @Bean} declaration. The bean can also be disabled
 * via {@code dmx.fun.observation.enabled=false} in {@code application.properties}.
 *
 * @see DmxObservation
 */
@AutoConfiguration
@ConditionalOnClass({ObservationRegistry.class, DmxObservation.class})
@ConditionalOnBean(ObservationRegistry.class)
@NullMarked
public class DmxFunObservationAutoConfiguration {

    /** Default constructor required for Spring Boot autoconfiguration instantiation. */
    public DmxFunObservationAutoConfiguration() {}

    /**
     * Registers a {@link DmxObservation} bean bound to the application's
     * {@link ObservationRegistry}. Can be disabled via
     * {@code dmx.fun.observation.enabled=false}.
     *
     * @param registry the observation registry to create observations with
     * @return a ready-to-use {@link DmxObservation} instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "dmx.fun.observation.enabled", havingValue = "true", matchIfMissing = true)
    public DmxObservation dmxObservation(ObservationRegistry registry) {
        return DmxObservation.of(registry);
    }
}
