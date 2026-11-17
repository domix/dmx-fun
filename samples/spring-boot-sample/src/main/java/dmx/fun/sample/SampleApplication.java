package dmx.fun.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample application entry point.
 *
 * <p>No Jackson configuration is needed: {@code fun-spring-boot} registers {@code DmxFunModule}
 * as a module bean and Spring Boot's {@code JacksonAutoConfiguration} applies it to the
 * application's mapper (Jackson 3 via {@code fun-jackson3} on Boot 4, Jackson 2 via
 * {@code fun-jackson2} on Boot 3).
 */
@SpringBootApplication
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
