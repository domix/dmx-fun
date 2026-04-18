package dmx.fun.spring.boot.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import dmx.fun.jackson.DmxFunModule;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration that registers {@link DmxFunModule} with the application's
 * Jackson {@link ObjectMapper}.
 *
 * <p>Activates only when both {@code jackson-databind} and {@code fun-jackson} are on the
 * classpath. Spring Boot's {@code JacksonAutoConfiguration} detects all {@link com.fasterxml.jackson.databind.Module}
 * beans and registers them automatically, so no manual {@code ObjectMapper.registerModule(...)}
 * call is needed.
 *
 * <p>Back-off: if the application already declares a {@link DmxFunModule} bean, this
 * auto-configuration does not register a second one.
 *
 * <p>Opt out: set {@code dmx.fun.jackson.enabled=false} in {@code application.properties}.
 */
@AutoConfiguration
@ConditionalOnClass({ObjectMapper.class, DmxFunModule.class})
@ConditionalOnProperty(name = "dmx.fun.jackson.enabled", havingValue = "true", matchIfMissing = true)
@NullMarked
public class DmxFunJacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DmxFunModule.class)
    public DmxFunModule dmxFunModule() {
        return new DmxFunModule();
    }
}
