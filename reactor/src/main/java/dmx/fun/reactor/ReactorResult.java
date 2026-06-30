package dmx.fun.reactor;

import dmx.fun.Result;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import reactor.core.publisher.Mono;

/**
 * Railway-style operators over a {@code Mono<Result<V, E>>}, so a reactive pipeline
 * can stay on the {@link Result} track without unwrapping at every step.
 *
 * <p>Each operator threads through the {@code Ok} path and passes {@code Err} along
 * untouched (or transforms only the error channel, for {@link #mapErr} and
 * {@link #recover}):
 *
 * <ul>
 *   <li>{@link #mapOk} — transform the success value;</li>
 *   <li>{@link #flatMapOk} — chain a dependent reactive step on the success value;</li>
 *   <li>{@link #mapErr} — transform the error channel;</li>
 *   <li>{@link #recover} — turn an {@code Err} back into an {@code Ok} via a fallback.</li>
 * </ul>
 */
@NullMarked
public final class ReactorResult {

    private ReactorResult() {
    }

    /**
     * Maps the success value of a {@code Mono<Result<V, E>>}, leaving {@code Err} untouched.
     *
     * @param mono   the source
     * @param mapper maps the {@code Ok} value
     * @param <V>    the input value type
     * @param <W>    the output value type
     * @param <E>    the error type
     * @return a {@code Mono} of the transformed {@link Result}
     */
    public static <V, W, E> Mono<Result<W, E>> mapOk(
        Mono<Result<V, E>> mono,
        Function<? super V, ? extends W> mapper
    ) {
        Objects.requireNonNull(mono, "mono");
        Objects.requireNonNull(mapper, "mapper");
        return mono.map(result -> result.map(value -> mapper.apply(value)));
    }

    /**
     * Chains a dependent reactive step on the success value: when the result is
     * {@code Ok}, the mapper's {@code Mono<Result<W, E>>} is sequenced; when it is
     * {@code Err}, the error is passed through without invoking the mapper.
     *
     * @param mono   the source
     * @param mapper maps the {@code Ok} value to the next reactive step
     * @param <V>    the input value type
     * @param <W>    the output value type
     * @param <E>    the error type
     * @return a {@code Mono} of the chained {@link Result}
     */
    public static <V, W, E> Mono<Result<W, E>> flatMapOk(
        Mono<Result<V, E>> mono,
        Function<? super V, Mono<Result<W, E>>> mapper
    ) {
        Objects.requireNonNull(mono, "mono");
        Objects.requireNonNull(mapper, "mapper");
        return mono.flatMap(result -> switch (result) {
            case Result.Ok<V, E> ok -> mapper.apply(ok.value());
            case Result.Err<V, E> err -> Mono.just(Result.err(err.error()));
        });
    }

    /**
     * Maps the error channel of a {@code Mono<Result<V, E>>}, leaving {@code Ok} untouched.
     *
     * @param mono   the source
     * @param mapper maps the {@code Err} value
     * @param <V>    the value type
     * @param <E>    the input error type
     * @param <F>    the output error type
     * @return a {@code Mono} of the {@link Result} with the mapped error channel
     */
    public static <V, E, F> Mono<Result<V, F>> mapErr(
        Mono<Result<V, E>> mono,
        Function<? super E, ? extends F> mapper
    ) {
        Objects.requireNonNull(mono, "mono");
        Objects.requireNonNull(mapper, "mapper");
        return mono.map(result -> result.mapError(error -> mapper.apply(error)));
    }

    /**
     * Recovers from {@code Err} by mapping the error to a fallback value, producing an
     * {@code Ok}; an {@code Ok} is passed through unchanged.
     *
     * @param mono     the source
     * @param fallback maps the {@code Err} value to a recovery value
     * @param <V>      the value type
     * @param <E>      the error type
     * @return a {@code Mono} of the recovered {@link Result}
     */
    public static <V, E> Mono<Result<V, E>> recover(
        Mono<Result<V, E>> mono,
        Function<? super E, ? extends V> fallback
    ) {
        Objects.requireNonNull(mono, "mono");
        Objects.requireNonNull(fallback, "fallback");
        return mono.map(result -> result.recover(error -> fallback.apply(error)));
    }
}
