package dmx.fun.reactor;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Idiomatic conversions between Project Reactor's {@link Flux} and dmx-fun's
 * {@link Option}, {@link Result}, {@link Try}, and {@link NonEmptyList}.
 *
 * <p>Where {@link ReactorFun} handles a single value ({@code Mono}), this facade
 * handles streams ({@code Flux}):
 *
 * <ul>
 *   <li><b>{@code sequence} / {@code collectResult} / {@code collectValidated}</b>
 *       fold a stream of outcomes into a single {@code Mono<Result<List<…>, …>>} —
 *       the reactive equivalent of {@link Result#sequence(Iterable)} — choosing
 *       fail-fast or error-accumulating semantics.</li>
 *   <li><b>{@code flattenOption}</b> drops absent elements from a
 *       {@code Flux<Option<T>>}.</li>
 *   <li><b>{@code toFlux}</b> emits a dmx-fun container or collection as a
 *       {@code Flux}.</li>
 * </ul>
 *
 * <p>Backpressure and cancellation are preserved by the underlying Reactor operators.
 */
@NullMarked
public final class ReactorFlux {

    private ReactorFlux() {
    }

    // ── Aggregation: Flux -> Mono<Result<List>> ────────────────────────────────

    /**
     * Folds a {@code Flux<Result<T, E>>} into a single {@code Mono<Result<List<T>, E>>},
     * fail-fast: the first {@code Err} short-circuits and cancels the upstream, and an
     * empty stream yields {@code Ok([])}. A Reactor error signal from the source is not
     * absorbed — it propagates as a Reactor error.
     *
     * @param results the stream of results
     * @param <T>     the value type
     * @param <E>     the error type
     * @return a {@code Mono} emitting the sequenced {@link Result}
     */
    public static <T, E> Mono<Result<List<T>, E>> sequence(Flux<Result<T, E>> results) {
        Objects.requireNonNull(results, "results");
        return results
            .concatMap(result -> switch (result) {
                case Result.Ok<T, E> ok -> Mono.just(ok.value());
                case Result.Err<T, E> err -> Mono.<T>error(new ResultError(err.error()));
            })
            .collectList()
            .<Result<List<T>, E>>map(Result::ok)
            .onErrorResume(ResultError.class, e -> Mono.just(Result.err(extractError(e))));
    }

    /**
     * Collects every element of a {@code Flux<T>} into {@code Ok(list)} (empty stream →
     * {@code Ok([])}), turning a Reactor error signal into {@code Err(cause)}.
     *
     * @param source the source stream
     * @param <T>    the value type
     * @return a {@code Mono} emitting the collected {@link Result}
     */
    public static <T> Mono<Result<List<T>, Throwable>> collectResult(Flux<T> source) {
        Objects.requireNonNull(source, "source");
        return source.collectList()
            .<Result<List<T>, Throwable>>map(Result::ok)
            .onErrorResume(throwable -> Mono.just(Result.err(throwable)));
    }

    /**
     * Collects every element of a {@code Flux<T>} into {@code Ok(list)}, mapping a
     * Reactor error signal through {@code errorMapper} to the typed error channel.
     *
     * @param source      the source stream
     * @param errorMapper maps a {@link Throwable} error signal to the error type
     * @param <T>         the value type
     * @param <E>         the typed error type
     * @return a {@code Mono} emitting the collected {@link Result}
     */
    public static <T, E> Mono<Result<List<T>, E>> collectResult(
        Flux<T> source,
        Function<? super Throwable, E> errorMapper
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(errorMapper, "errorMapper");
        return source.collectList()
            .<Result<List<T>, E>>map(Result::ok)
            .onErrorResume(throwable -> Mono.just(Result.err(errorMapper.apply(throwable))));
    }

    /**
     * Folds a {@code Flux<Result<T, E>>} into a {@code Mono<Validated<NonEmptyList<E>, List<T>>>},
     * accumulating: every {@code Err} is gathered into the invalid channel (no
     * short-circuit), and an all-{@code Ok} stream (including empty) is {@code Valid}.
     *
     * @param results the stream of results
     * @param <T>     the value type
     * @param <E>     the error type
     * @return a {@code Mono} emitting the accumulated {@link Validated}
     */
    public static <T, E> Mono<Validated<NonEmptyList<E>, List<T>>> collectValidated(
        Flux<Result<T, E>> results
    ) {
        Objects.requireNonNull(results, "results");
        // Stream straight into the value/error buffers instead of materializing the
        // whole Flux into a List<Result> and re-walking it.
        return results.collect(Accumulator<T, E>::new, Accumulator::add)
            .map(Accumulator::toValidated);
    }

    // ── Per-element helpers ────────────────────────────────────────────────────

    /**
     * Drops the absent elements of a {@code Flux<Option<T>>}, keeping the values of the
     * {@code Some} elements in source order.
     *
     * @param source the stream of options
     * @param <T>    the value type
     * @return a {@code Flux} of the present values
     */
    public static <T> Flux<T> flattenOption(Flux<Option<T>> source) {
        Objects.requireNonNull(source, "source");
        return source.handle((option, sink) -> option.match(() -> { }, sink::next));
    }

    // ── dmx-fun -> Flux ────────────────────────────────────────────────────────

    /**
     * Emits each element of a {@link NonEmptyList} as a {@code Flux} (1..N elements).
     *
     * @param list the non-empty list
     * @param <T>  the element type
     * @return a {@code Flux} over the list
     */
    public static <T> Flux<T> toFlux(NonEmptyList<T> list) {
        Objects.requireNonNull(list, "list");
        return Flux.fromIterable(list);
    }

    /**
     * Emits an {@link Option} as a {@code Flux}: {@code Some} emits one element,
     * {@code None} completes empty.
     *
     * @param option the option
     * @param <T>    the value type
     * @return a {@code Flux} of zero or one element
     */
    public static <T> Flux<T> toFlux(Option<T> option) {
        Objects.requireNonNull(option, "option");
        return option.fold(Flux::empty, Flux::just);
    }

    /**
     * Emits a {@link Try} as a {@code Flux}: {@code Success} emits one element,
     * {@code Failure} errors with the cause.
     *
     * @param aTry the try
     * @param <T>  the value type
     * @return a {@code Flux} of one element or an error
     */
    public static <T> Flux<T> toFlux(Try<T> aTry) {
        Objects.requireNonNull(aTry, "aTry");
        return aTry.fold(Flux::just, Flux::error);
    }

    /**
     * Emits a {@link Result} whose error channel is already a {@link Throwable} as a
     * {@code Flux}: {@code Ok} emits one element, {@code Err} errors with the error.
     *
     * @param result the result
     * @param <T>    the value type
     * @param <E>    the error type, which must be a {@link Throwable}
     * @return a {@code Flux} of one element or an error
     */
    public static <T, E extends Throwable> Flux<T> toFlux(Result<T, E> result) {
        Objects.requireNonNull(result, "result");
        return switch (result) {
            case Result.Ok<T, E> ok -> Flux.just(ok.value());
            case Result.Err<T, E> err -> Flux.error(err.error());
        };
    }

    /**
     * Emits a {@link Result} as a {@code Flux}: {@code Ok} emits one element, {@code Err}
     * errors with the error mapped to a {@link Throwable} by {@code errorMapper}.
     *
     * @param result      the result
     * @param errorMapper maps the typed error to a {@link Throwable} error signal
     * @param <T>         the value type
     * @param <E>         the error type
     * @return a {@code Flux} of one element or an error
     */
    public static <T, E> Flux<T> toFlux(
        Result<T, E> result,
        Function<? super E, ? extends Throwable> errorMapper
    ) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(errorMapper, "errorMapper");
        return switch (result) {
            case Result.Ok<T, E> ok -> Flux.just(ok.value());
            case Result.Err<T, E> err -> Flux.error(
                () -> Objects.requireNonNull(errorMapper.apply(err.error()), "errorMapper returned null"));
        };
    }

    // ── internals ──────────────────────────────────────────────────────────────

    /** Aggregates a stream of {@code Result} into value and error buffers as it is consumed. */
    private static final class Accumulator<T, E> {
        private final List<T> values = new ArrayList<>();
        private final List<E> errors = new ArrayList<>();

        void add(Result<T, E> result) {
            switch (result) {
                case Result.Ok<T, E> ok -> values.add(ok.value());
                case Result.Err<T, E> err -> errors.add(err.error());
            }
        }

        Validated<NonEmptyList<E>, List<T>> toValidated() {
            return NonEmptyList.fromList(errors).fold(
                () -> Validated.valid(values),
                Validated::invalid);
        }
    }

    /** Carries a typed {@code Result.Err} value through Reactor's error channel for {@link #sequence}. */
    private static final class ResultError extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final transient Object error;

        ResultError(Object error) {
            super(null, null, false, false);
            this.error = error;
        }
    }

    @SuppressWarnings("unchecked")
    private static <E> E extractError(ResultError signal) {
        return (E) signal.error;
    }
}
