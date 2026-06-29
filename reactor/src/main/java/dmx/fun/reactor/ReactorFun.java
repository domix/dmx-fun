package dmx.fun.reactor;

import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import reactor.core.publisher.Mono;

/**
 * Idiomatic conversions between Project Reactor's {@link Mono} and dmx-fun's
 * {@link Option}, {@link Result}, and {@link Try}.
 *
 * <p>The conversions come in two flavours:
 *
 * <ul>
 *   <li><b>{@code toMono*}</b> variants stay reactive: they absorb the empty and
 *       error signals into the value channel so the result is a
 *       {@code Mono<Result<...>>} / {@code Mono<Try<...>>} that never errors for a
 *       modeled failure. Prefer these inside reactive pipelines.</li>
 *   <li><b>{@code toTry} / {@code toResult} / {@code toOption}</b> are blocking
 *       extractors: they subscribe and wait for the terminal signal, returning the
 *       dmx-fun value directly. They call {@link Mono#block()} and therefore must
 *       not be used on a non-blocking thread (event loop). They exist for bridging
 *       into non-reactive code and tests.</li>
 *   <li><b>{@code toMono}</b> goes the other way, turning an {@code Option},
 *       {@code Result}, or {@code Try} back into a {@code Mono}.</li>
 * </ul>
 *
 * <p>Empty-Mono policy: a {@code Mono} that completes without a value is treated as
 * a missing value — {@link Option#none()} for the Option conversions, and a failure
 * carrying {@link NoSuchElementException} for the Try and Result conversions.
 */
@NullMarked
public final class ReactorFun {

    private ReactorFun() {
    }

    // ── Reactor -> dmx-fun, reactive (non-blocking) ────────────────────────────

    /**
     * Converts a {@code Mono<V>} into a {@code Mono<Try<V>>} that never errors:
     * a value becomes {@link Try.Success}, an error signal becomes
     * {@link Try.Failure} carrying the cause, and an empty completion becomes a
     * {@code Failure} carrying {@link NoSuchElementException}.
     *
     * @param mono the source publisher
     * @param <V>  the value type
     * @return a {@code Mono} that always emits a {@link Try}
     */
    public static <V> Mono<Try<V>> toMonoTry(Mono<V> mono) {
        Objects.requireNonNull(mono, "mono");
        return mono.<Try<V>>map(Try::success)
            .onErrorResume(throwable -> Mono.just(Try.failure(throwable)))
            .switchIfEmpty(Mono.fromSupplier(() -> Try.failure(emptyMono())));
    }

    /**
     * Converts a {@code Mono<V>} into a {@code Mono<Result<V, Throwable>>} that
     * never errors: a value becomes {@link Result.Ok}, an error signal becomes
     * {@link Result.Err} carrying the cause, and an empty completion becomes an
     * {@code Err} carrying {@link NoSuchElementException}.
     *
     * @param mono the source publisher
     * @param <V>  the value type
     * @return a {@code Mono} that always emits a {@link Result} with the failure on
     *     the error channel as a {@link Throwable}
     */
    public static <V> Mono<Result<V, Throwable>> toMonoResult(Mono<V> mono) {
        Objects.requireNonNull(mono, "mono");
        return mono.<Result<V, Throwable>>map(Result::ok)
            .onErrorResume(throwable -> Mono.just(Result.err(throwable)))
            .switchIfEmpty(Mono.fromSupplier(() -> Result.err(emptyMono())));
    }

    /**
     * Converts a {@code Mono<V>} into a {@code Mono<Result<V, E>>}, mapping any
     * error (and an empty completion, via {@link NoSuchElementException}) through
     * {@code errorMapper} to the typed error channel.
     *
     * @param mono        the source publisher
     * @param errorMapper maps a {@link Throwable} (the cause, or a
     *                    {@link NoSuchElementException} on empty) to the error type
     * @param <V>         the value type
     * @param <E>         the typed error type
     * @return a {@code Mono} that always emits a {@link Result}
     */
    public static <V, E> Mono<Result<V, E>> toMonoResult(
        Mono<V> mono,
        Function<? super Throwable, E> errorMapper
    ) {
        Objects.requireNonNull(mono, "mono");
        Objects.requireNonNull(errorMapper, "errorMapper");
        return mono.<Result<V, E>>map(Result::ok)
            .onErrorResume(throwable -> Mono.just(Result.err(errorMapper.apply(throwable))))
            .switchIfEmpty(Mono.fromSupplier(() -> Result.err(errorMapper.apply(emptyMono()))));
    }

    /**
     * Converts a {@code Mono<V>} into a {@code Mono<Result<V, E>>} with full control
     * over both channels: errors are mapped through {@code errorMapper}, and an empty
     * completion produces the error from {@code onEmpty} instead of the default
     * {@link NoSuchElementException}.
     *
     * @param mono        the source publisher
     * @param errorMapper maps a {@link Throwable} error signal to the error type
     * @param onEmpty     supplies the error value used when the source completes empty
     * @param <V>         the value type
     * @param <E>         the typed error type
     * @return a {@code Mono} that always emits a {@link Result}
     */
    public static <V, E> Mono<Result<V, E>> toMonoResult(
        Mono<V> mono,
        Function<? super Throwable, E> errorMapper,
        Supplier<E> onEmpty
    ) {
        Objects.requireNonNull(mono, "mono");
        Objects.requireNonNull(errorMapper, "errorMapper");
        Objects.requireNonNull(onEmpty, "onEmpty");
        return mono.<Result<V, E>>map(Result::ok)
            .onErrorResume(throwable -> Mono.just(Result.err(errorMapper.apply(throwable))))
            .switchIfEmpty(Mono.fromSupplier(() -> Result.err(onEmpty.get())));
    }

    /**
     * Converts a {@code Mono<V>} into a {@code Mono<Option<V>>}: a value becomes
     * {@link Option#some(Object)} and an empty completion becomes
     * {@link Option#none()}. An error signal is <em>not</em> absorbed — it
     * propagates as a Reactor error, since {@code Option} has no error channel.
     *
     * @param mono the source publisher
     * @param <V>  the value type
     * @return a {@code Mono} that emits an {@link Option}, or errors if the source does
     */
    public static <V> Mono<Option<V>> toMonoOption(Mono<V> mono) {
        Objects.requireNonNull(mono, "mono");
        return mono.<Option<V>>map(Option::some)
            .switchIfEmpty(Mono.fromSupplier(Option::none));
    }

    // ── Reactor -> dmx-fun, blocking extractors ────────────────────────────────

    /**
     * Blocking variant of {@link #toMonoTry(Mono)}: subscribes and waits, returning
     * the {@link Try} directly. Blocks the calling thread — do not call on a
     * non-blocking (event-loop) thread.
     *
     * @param mono the source publisher
     * @param <V>  the value type
     * @return the resulting {@link Try}
     */
    public static <V> Try<V> toTry(Mono<V> mono) {
        return Objects.requireNonNull(toMonoTry(mono).block());
    }

    /**
     * Blocking variant of {@link #toMonoResult(Mono)}: subscribes and waits,
     * returning the {@link Result} directly. Blocks the calling thread.
     *
     * @param mono the source publisher
     * @param <V>  the value type
     * @return the resulting {@link Result} with a {@link Throwable} error channel
     */
    public static <V> Result<V, Throwable> toResult(Mono<V> mono) {
        return Objects.requireNonNull(toMonoResult(mono).block());
    }

    /**
     * Blocking variant of {@link #toMonoResult(Mono, Function)}: subscribes and
     * waits, returning the typed {@link Result} directly. Blocks the calling thread.
     *
     * @param mono        the source publisher
     * @param errorMapper maps a {@link Throwable} to the error type
     * @param <V>         the value type
     * @param <E>         the typed error type
     * @return the resulting {@link Result}
     */
    public static <V, E> Result<V, E> toResult(
        Mono<V> mono,
        Function<? super Throwable, E> errorMapper
    ) {
        return Objects.requireNonNull(toMonoResult(mono, errorMapper).block());
    }

    /**
     * Blocking variant of {@link #toMonoResult(Mono, Function, Supplier)}: subscribes
     * and waits, returning the typed {@link Result} directly. Blocks the calling thread.
     *
     * @param mono        the source publisher
     * @param errorMapper maps a {@link Throwable} error signal to the error type
     * @param onEmpty     supplies the error value used when the source completes empty
     * @param <V>         the value type
     * @param <E>         the typed error type
     * @return the resulting {@link Result}
     */
    public static <V, E> Result<V, E> toResult(
        Mono<V> mono,
        Function<? super Throwable, E> errorMapper,
        Supplier<E> onEmpty
    ) {
        return Objects.requireNonNull(toMonoResult(mono, errorMapper, onEmpty).block());
    }

    /**
     * Blocking variant of {@link #toMonoOption(Mono)}: subscribes and waits,
     * returning the {@link Option} directly. Blocks the calling thread, and a
     * Reactor error signal is rethrown by {@link Mono#block()}.
     *
     * @param mono the source publisher
     * @param <V>  the value type
     * @return the resulting {@link Option}
     */
    public static <V> Option<V> toOption(Mono<V> mono) {
        return Objects.requireNonNull(toMonoOption(mono).block());
    }

    // ── dmx-fun -> Reactor ─────────────────────────────────────────────────────

    /**
     * Turns an {@link Option} into a {@code Mono}: {@code Some} becomes
     * {@link Mono#just(Object)} and {@code None} becomes {@link Mono#empty()}.
     *
     * @param option the option to convert
     * @param <V>    the value type
     * @return a {@code Mono} that emits the value or completes empty
     */
    public static <V> Mono<V> toMono(Option<V> option) {
        Objects.requireNonNull(option, "option");
        return option.fold(Mono::empty, Mono::just);
    }

    /**
     * Turns a {@link Try} into a {@code Mono}: {@code Success} becomes
     * {@link Mono#just(Object)} and {@code Failure} becomes
     * {@link Mono#error(Throwable)} carrying the cause.
     *
     * @param aTry the try to convert
     * @param <V>  the value type
     * @return a {@code Mono} that emits the value or errors with the cause
     */
    public static <V> Mono<V> toMono(Try<V> aTry) {
        Objects.requireNonNull(aTry, "aTry");
        return aTry.fold(Mono::just, Mono::error);
    }

    /**
     * Turns a {@link Result} whose error channel is already a {@link Throwable} into a
     * {@code Mono}: {@code Ok} becomes {@link Mono#just(Object)} and {@code Err}
     * becomes {@link Mono#error(Throwable)} carrying the error directly. Use the
     * {@link #toMono(Result, Function)} overload when the error is a domain type that
     * must be mapped to a {@code Throwable}.
     *
     * @param result the result to convert
     * @param <V>    the value type
     * @param <E>    the error type, which must be a {@link Throwable}
     * @return a {@code Mono} that emits the value or errors with the error
     */
    public static <V, E extends Throwable> Mono<V> toMono(Result<V, E> result) {
        Objects.requireNonNull(result, "result");
        return switch (result) {
            case Result.Ok<V, E> ok -> Mono.just(ok.value());
            case Result.Err<V, E> err -> Mono.error(err.error());
        };
    }

    /**
     * Turns a {@link Result} into a {@code Mono}: {@code Ok} becomes
     * {@link Mono#just(Object)} and {@code Err} becomes
     * {@link Mono#error(Throwable)} with the error mapped to a {@link Throwable} by
     * {@code errorMapper}.
     *
     * @param result      the result to convert
     * @param errorMapper maps the typed error to a {@link Throwable} error signal
     * @param <V>         the value type
     * @param <E>         the typed error type
     * @return a {@code Mono} that emits the value or errors with the mapped throwable
     */
    public static <V, E> Mono<V> toMono(
        Result<V, E> result,
        Function<? super E, ? extends Throwable> errorMapper
    ) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(errorMapper, "errorMapper");
        return switch (result) {
            case Result.Ok<V, E> ok -> Mono.just(ok.value());
            case Result.Err<V, E> err -> Mono.error(errorMapper.apply(err.error()));
        };
    }

    private static NoSuchElementException emptyMono() {
        return new NoSuchElementException("Mono completed empty");
    }
}
