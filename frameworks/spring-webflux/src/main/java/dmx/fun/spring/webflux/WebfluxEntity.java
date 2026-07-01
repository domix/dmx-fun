package dmx.fun.spring.webflux;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * Maps dmx-fun outcomes to a {@link ResponseEntity} for Spring WebFlux <strong>annotation</strong>
 * controllers ({@code @RestController}), the counterpart of {@link WebfluxFun} (which targets
 * {@code ServerResponse} for functional endpoints).
 *
 * <p>A controller method returns the adapter's {@code Mono<ResponseEntity<…>>} directly, so it maps
 * a domain outcome instead of re-implementing status/body conventions:
 *
 * <pre>{@code
 * @GetMapping("/users/{id}")
 * Mono<ResponseEntity<User>> user(@PathVariable String id) {
 *     return WebfluxEntity.fromOption(userService.find(id));   // Some -> 200, None/empty -> 404
 * }
 *
 * @GetMapping("/orders/{id}")
 * Mono<ResponseEntity<?>> order(@PathVariable String id) {
 *     return WebfluxEntity.fromResult(orderService.findById(id),   // Mono<Result<Order, ApiError>>
 *         error -> ResponseEntity.status(error.status()).body(error.detail()));
 * }
 * }</pre>
 *
 * <p>HTTP conventions mirror {@link WebfluxFun}: {@code Ok}/{@code Some}/{@code Success}/{@code Valid}
 * → {@code 200} with the value as the body; {@code None}, an empty source {@code Mono}
 * → {@code 404}; {@code Invalid} → {@code 400} with the accumulated errors; {@code Err}/{@code Failure}
 * → your mapper.
 */
@NullMarked
public final class WebfluxEntity {

    private WebfluxEntity() {
    }

    /**
     * Maps an {@link Option}: {@code Some(v)} → {@code 200} with {@code v}; {@code None} or an empty
     * source {@code Mono} → {@code 404}.
     *
     * @param source the upstream option
     * @param <V>    the body type
     * @return a {@code Mono} emitting the response entity
     */
    public static <V> Mono<ResponseEntity<V>> fromOption(Mono<Option<V>> source) {
        Objects.requireNonNull(source, "source");
        return source.map(option -> option.<ResponseEntity<V>>fold(
                () -> ResponseEntity.notFound().build(),
                value -> ResponseEntity.ok(value)))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Maps a {@link Result}: {@code Ok(v)} → {@code 200} with {@code v}; {@code Err(e)} → {@code onError};
     * an empty source {@code Mono} → {@code 404}.
     *
     * @param source  the upstream result
     * @param onError maps the error to a response entity (status and body of your choosing)
     * @param <V>     the success body type
     * @param <E>     the error type
     * @return a {@code Mono} emitting the response entity
     */
    public static <V, E> Mono<ResponseEntity<?>> fromResult(
        Mono<Result<V, E>> source, Function<? super E, ResponseEntity<?>> onError) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(onError, "onError");
        return source.<ResponseEntity<?>>map(result -> switch (result) {
                case Result.Ok<V, E> ok -> ResponseEntity.ok(ok.value());
                case Result.Err<V, E> err -> onError.apply(err.error());
            })
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Maps a {@link Try}: {@code Success(v)} → {@code 200} with {@code v}; {@code Failure(t)}
     * → {@code onFailure}; an empty source {@code Mono} → {@code 404}.
     *
     * @param source    the upstream try
     * @param onFailure maps the throwable to a response entity
     * @param <V>       the success body type
     * @return a {@code Mono} emitting the response entity
     */
    public static <V> Mono<ResponseEntity<?>> fromTry(
        Mono<Try<V>> source, Function<? super Throwable, ResponseEntity<?>> onFailure) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(onFailure, "onFailure");
        return source.<ResponseEntity<?>>map(aTry -> aTry.<ResponseEntity<?>>fold(
                value -> ResponseEntity.ok(value),
                onFailure))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Maps a {@link Validated}: {@code Valid(v)} → {@code 200} with {@code v}; {@code Invalid(errors)}
     * → {@code 400} carrying every error (as a list); an empty source {@code Mono} → {@code 404}.
     *
     * @param source the upstream validated outcome accumulating errors in a {@link NonEmptyList}
     * @param <E>    the error type
     * @param <V>    the valid body type
     * @return a {@code Mono} emitting the response entity
     */
    public static <E, V> Mono<ResponseEntity<?>> fromValidated(
        Mono<Validated<NonEmptyList<E>, V>> source) {
        Objects.requireNonNull(source, "source");
        return source.<ResponseEntity<?>>map(validated -> switch (validated) {
                case Validated.Valid<NonEmptyList<E>, V> valid -> ResponseEntity.ok(valid.value());
                case Validated.Invalid<NonEmptyList<E>, V> invalid ->
                    ResponseEntity.badRequest().body(invalid.error().toList());
            })
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
