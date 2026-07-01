package dmx.fun.spring.webflux;

import org.jspecify.annotations.NullMarked;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Maps a success value of type {@code V} to an HTTP {@link ServerResponse}.
 *
 * <p>Supplied to the success-customizing overloads of {@link WebfluxFun} so the
 * {@code Ok} / {@code Some} / {@code Success} / {@code Valid} branch can control the
 * status, headers, and body — for example {@code 201 Created} with a {@code Location}
 * header on a {@code POST} — instead of the default {@code 200 OK} with the value as
 * the body.
 *
 * @param <V> the success value type
 */
@NullMarked
@FunctionalInterface
public interface SuccessHttpMapper<V> {

    /**
     * Renders the given success value as an HTTP response.
     *
     * @param value the success value
     * @return the response to send for this value
     */
    Mono<ServerResponse> apply(V value);
}
