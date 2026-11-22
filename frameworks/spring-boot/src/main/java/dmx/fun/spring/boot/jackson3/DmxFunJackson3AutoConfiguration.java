package dmx.fun.spring.boot.jackson3;

import dmx.fun.jackson3.DmxFunModule;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring Boot autoconfiguration that registers the Jackson 3 {@link DmxFunModule} with the
 * application's Jackson 3 ({@code tools.jackson}) mappers.
 *
 * <p>Activates only when both {@code tools.jackson.core:jackson-databind} and
 * {@code fun-jackson3} are on the classpath. Spring Boot 4's {@code JacksonAutoConfiguration}
 * collects every {@link JacksonModule} bean and adds it to the {@code JsonMapper} it builds,
 * so no manual {@code JsonMapper.builder().addModule(...)} call is needed.
 *
 * <p>This autoconfiguration is independent of {@code DmxFunJackson2AutoConfiguration}: an
 * application that has both Jackson lines on the classpath gets both modules, each registered
 * with its own mapper.
 *
 * <p>Back-off: if the application already declares a {@link DmxFunModule} bean, this
 * autoconfiguration does not register a second one.
 *
 * <p>Opt out: set {@code dmx.fun.jackson3.enabled=false} in {@code application.properties}.
 */
@AutoConfiguration
@ConditionalOnClass({ObjectMapper.class, DmxFunModule.class})
@ConditionalOnProperty(name = "dmx.fun.jackson3.enabled", havingValue = "true", matchIfMissing = true)
@NullMarked
public class DmxFunJackson3AutoConfiguration {

    /** Default constructor required for Spring Boot auto-configuration instantiation. */
    public DmxFunJackson3AutoConfiguration() {}

    /**
     * Registers a Jackson 3 {@link DmxFunModule} bean so that Spring Boot's
     * {@code JacksonAutoConfiguration} can discover it as a {@link JacksonModule} and apply it
     * to the application's mappers automatically.
     *
     * <p>Skipped when the application already declares its own {@link DmxFunModule} bean
     * ({@code @ConditionalOnMissingBean}).
     *
     * @return a ready-to-use {@link DmxFunModule} instance
     */
    @Bean
    @ConditionalOnMissingBean(DmxFunModule.class)
    public DmxFunModule dmxFunJackson3Module() {
        return new DmxFunModule();
    }
}
