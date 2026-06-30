package dmx.fun.spring.webflux;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import dmx.fun.reactor.ReactorFlux;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Maps dmx-fun outcomes to Spring WebFlux {@link ServerResponse} for functional endpoints,
 * so a handler can return {@code Result}, {@code Try}, {@code Option}, or {@code Validated}
 * without re-implementing status/body conventions per service.
 *
 * <h2>Default HTTP mapping</h2>
 * <ul>
 *   <li>{@code Result.Ok(v)} / {@code Try.Success(v)} / {@code Option.Some(v)} /
 *       {@code Validated.Valid(v)} → {@code 200 OK} with {@code v} as the body</li>
 *   <li>{@code Option.None} → {@code 404 Not Found}</li>
 *   <li>{@code Validated.Invalid(errors)} → {@code 400 Bad Request} with the accumulated
 *       errors (as a {@code List}) as the body</li>
 *   <li>{@code Result.Err(e)} → mapped by the supplied {@link ErrorHttpMapper}</li>
 *   <li>{@code Try.Failure(t)} → mapped by the supplied {@link ThrowableHttpMapper}</li>
 *   <li>an <strong>empty</strong> source {@code Mono} (it completes without emitting an
 *       outcome) → {@code 404 Not Found}</li>
 * </ul>
 *
 * <p>The success body is written with
 * {@code ServerResponse.ok().bodyValue(v)}, so the value must be encodable by the
 * application's WebFlux codecs. Compose with {@code fun-reactor}'s {@code ReactorResult}
 * operators on the {@code Mono<Result<...>>} before calling these adapters to stay on the
 * Result track.
 */
@NullMarked
public final class WebfluxFun {

    private WebfluxFun() {
    }

    /**
     * Maps a {@code Mono<Result<V, E>>} to a response: {@code Ok} → {@code 200} with the
     * value, {@code Err} → {@code errorMapper}, empty → {@code 404}.
     *
     * @param source      the upstream outcome
     * @param errorMapper renders a {@code Result.Err} value as a response
     * @param <V>         the success value type
     * @param <E>         the error type
     * @return the HTTP response
     */
    public static <V, E> Mono<ServerResponse> fromResult(
        Mono<Result<V, E>> source,
        ErrorHttpMapper<E> errorMapper
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(errorMapper, "errorMapper");
        return source.flatMap(result -> switch (result) {
            case Result.Ok<V, E> ok -> okBody(ok.value());
            case Result.Err<V, E> err -> errorMapper.apply(err.error());
        }).switchIfEmpty(notFound());
    }

    /**
     * Maps a {@code Mono<Option<V>>} to a response: {@code Some} → {@code 200} with the
     * value, {@code None} (or empty) → {@code 404}.
     *
     * @param source the upstream option
     * @param <V>    the value type
     * @return the HTTP response
     */
    public static <V> Mono<ServerResponse> fromOption(Mono<Option<V>> source) {
        Objects.requireNonNull(source, "source");
        return source.<ServerResponse>flatMap(option -> option.fold(WebfluxFun::notFound, WebfluxFun::okBody))
            .switchIfEmpty(notFound());
    }

    /**
     * Maps a {@code Mono<Try<V>>} to a response: {@code Success} → {@code 200} with the
     * value, {@code Failure} → {@code failureMapper}, empty → {@code 404}.
     *
     * @param source        the upstream try
     * @param failureMapper renders a {@code Try.Failure} cause as a response
     * @param <V>           the value type
     * @return the HTTP response
     */
    public static <V> Mono<ServerResponse> fromTry(
        Mono<Try<V>> source,
        ThrowableHttpMapper failureMapper
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(failureMapper, "failureMapper");
        return source.<ServerResponse>flatMap(aTry -> aTry.fold(WebfluxFun::okBody, failureMapper::apply))
            .switchIfEmpty(notFound());
    }

    /**
     * Maps a {@code Mono<Validated<NonEmptyList<E>, V>>} to a response: {@code Valid} →
     * {@code 200} with the value, {@code Invalid} → {@code 400} with the accumulated errors
     * as the body, empty → {@code 404}.
     *
     * @param source the upstream validated value
     * @param <V>    the value type
     * @param <E>    the element type of the accumulated errors
     * @return the HTTP response
     */
    public static <V, E> Mono<ServerResponse> fromValidated(
        Mono<Validated<NonEmptyList<E>, V>> source
    ) {
        Objects.requireNonNull(source, "source");
        return source.<ServerResponse>flatMap(validated -> switch (validated) {
            case Validated.Valid<NonEmptyList<E>, V> valid -> okBody(valid.value());
            case Validated.Invalid<NonEmptyList<E>, V> invalid ->
                ServerResponse.badRequest().bodyValue(invalid.error().toList());
        }).switchIfEmpty(notFound());
    }

    /**
     * Aggregates a {@code Flux<Result<V, E>>} into a single response by sequencing it
     * fail-fast (via {@code fun-reactor}'s {@code ReactorFlux.sequence}): all {@code Ok} →
     * {@code 200} with the list of values, the first {@code Err} → {@code errorMapper}.
     *
     * @param source      the stream of outcomes
     * @param errorMapper renders the first {@code Err} as a response
     * @param <V>         the success value type
     * @param <E>         the error type
     * @return the HTTP response
     */
    public static <V, E> Mono<ServerResponse> fromResultStream(
        Flux<Result<V, E>> source,
        ErrorHttpMapper<E> errorMapper
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(errorMapper, "errorMapper");
        return ReactorFlux.sequence(source).flatMap(result -> switch (result) {
            case Result.Ok<List<V>, E> ok -> okBody(ok.value());
            case Result.Err<List<V>, E> err -> errorMapper.apply(err.error());
        });
    }

    /**
     * Aggregates a {@code Flux<Result<V, E>>} into a single response by accumulating it (via
     * {@code fun-reactor}'s {@code ReactorFlux.collectValidated}): all {@code Ok} → {@code 200}
     * with the list of values, any {@code Err} → {@code 400} with <em>every</em> accumulated
     * error as the body. Unlike {@link #fromResultStream}, this does not short-circuit on the
     * first failure — it reports them all.
     *
     * @param source the stream of outcomes
     * @param <V>    the success value type
     * @param <E>    the error type
     * @return the HTTP response
     */
    public static <V, E> Mono<ServerResponse> fromResultStreamAccumulating(Flux<Result<V, E>> source) {
        Objects.requireNonNull(source, "source");
        return ReactorFlux.collectValidated(source).flatMap(validated -> switch (validated) {
            case Validated.Valid<NonEmptyList<E>, List<V>> valid -> okBody(valid.value());
            case Validated.Invalid<NonEmptyList<E>, List<V>> invalid ->
                ServerResponse.badRequest().bodyValue(invalid.error().toList());
        });
    }

    /**
     * Streams a {@code Flux<V>} as the response body with the given {@code mediaType} (for
     * example {@link MediaType#APPLICATION_NDJSON} or {@link MediaType#TEXT_EVENT_STREAM}),
     * encoding each element as it arrives instead of collecting first.
     *
     * <p><strong>HTTP note:</strong> the {@code 200} status and headers are sent before the body
     * streams, so a terminal error cannot change the status. Use {@link #stream(Flux, MediaType,
     * Class, java.util.function.Function)} to append a typed fallback element on error, or collect
     * first with {@link #fromResultStream} / {@link #fromResultStreamAccumulating} when the
     * outcome must drive the status code.
     *
     * @param source      the stream of elements
     * @param mediaType   the response content type (e.g. NDJSON or SSE)
     * @param elementType the element class, required by the WebFlux encoder
     * @param <V>         the element type
     * @return the streaming HTTP response
     */
    public static <V> Mono<ServerResponse> stream(Flux<V> source, MediaType mediaType, Class<V> elementType) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mediaType, "mediaType");
        Objects.requireNonNull(elementType, "elementType");
        return ServerResponse.ok().contentType(mediaType).body(source, elementType);
    }

    /**
     * Streams a {@code Flux<V>} as the response body, appending a typed fallback element produced
     * by {@code onError} if the stream terminates with an error — a graceful degradation that
     * keeps the (already-sent) {@code 200} status instead of abruptly closing the connection.
     *
     * @param source      the stream of elements
     * @param mediaType   the response content type (e.g. NDJSON or SSE)
     * @param elementType the element class, required by the WebFlux encoder
     * @param onError     maps a terminal error to a final fallback element
     * @param <V>         the element type
     * @return the streaming HTTP response
     */
    public static <V> Mono<ServerResponse> stream(
        Flux<V> source,
        MediaType mediaType,
        Class<V> elementType,
        Function<? super Throwable, V> onError
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mediaType, "mediaType");
        Objects.requireNonNull(elementType, "elementType");
        Objects.requireNonNull(onError, "onError");
        Flux<V> withFallback = source.onErrorResume(throwable -> Mono.just(onError.apply(throwable)));
        return ServerResponse.ok().contentType(mediaType).body(withFallback, elementType);
    }

    private static Mono<ServerResponse> okBody(Object value) {
        return ServerResponse.ok().bodyValue(value);
    }

    private static Mono<ServerResponse> notFound() {
        return ServerResponse.notFound().build();
    }
}
