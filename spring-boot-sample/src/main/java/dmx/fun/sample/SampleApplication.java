package dmx.fun.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import dmx.fun.jackson.DmxFunModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@SpringBootApplication
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    // Jackson 2.x ObjectMapper with all dmx-fun type serializers/deserializers registered.
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new DmxFunModule());
    }

    // Explicit HTTP message converter so Spring MVC uses our Jackson 2.x ObjectMapper
    // for serializing Result, Option, Either, Try, Validated, Tuple, NonEmptyList responses.
    @Bean
    public MappingJackson2HttpMessageConverter jacksonHttpMessageConverter(ObjectMapper objectMapper) {
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
