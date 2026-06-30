package dmx.fun.spring.webflux;

import org.jspecify.annotations.NullMarked;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Maps a domain error of type {@code E} to an HTTP {@link ServerResponse}.
 *
 * <p>Supplied to {@link WebfluxFun#fromResult} and {@link WebfluxFun#fromResultStream}
 * so a {@code Result.Err} is rendered with a status and body the application controls.
 *
 * @param <E> the domain error type
 */
@NullMarked
@FunctionalInterface
public interface ErrorHttpMapper<E> {

    /**
     * Renders the given error as an HTTP response.
     *
     * @param error the domain error
     * @return the response to send for this error
     */
    Mono<ServerResponse> apply(E error);
}
