package dmx.fun.spring.webflux;

import org.jspecify.annotations.NullMarked;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Maps a {@link Throwable} (a {@code Try.Failure} cause) to an HTTP {@link ServerResponse}.
 *
 * <p>Supplied to {@link WebfluxFun#fromTry} so a failed {@code Try} is rendered with a
 * status and body the application controls (for example translating known exception types
 * to {@code 4xx} and unknown ones to {@code 500}).
 */
@NullMarked
@FunctionalInterface
public interface ThrowableHttpMapper {

    /**
     * Renders the given cause as an HTTP response.
     *
     * @param cause the failure cause
     * @return the response to send for this failure
     */
    Mono<ServerResponse> apply(Throwable cause);
}
