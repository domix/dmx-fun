package dmx.fun.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@SpringBootApplication
public class SampleApplication {

    static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    // Spring Boot 4.x uses Jackson 3.x by default; this sample pins Jackson 2.x.
    // DmxFunModule is auto-configured by DmxFunJacksonAutoConfiguration — no manual
    // new DmxFunModule() needed here.
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .findAndRegisterModules();
    }

    // Explicit HTTP message converter so Spring MVC uses the Jackson 2.x ObjectMapper above.
    // MappingJackson2HttpMessageConverter is deprecated for removal in Spring Framework 7 (Jackson 3.x
    // migration); suppressed here because fun-jackson still targets Jackson 2.x.
    @SuppressWarnings("removal")
    @Bean
    public MappingJackson2HttpMessageConverter jacksonHttpMessageConverter(ObjectMapper objectMapper) {
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
