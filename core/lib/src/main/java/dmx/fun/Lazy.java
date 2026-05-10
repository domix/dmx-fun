package dmx.fun;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A lazily evaluated value that is computed at most once.
 *
 * <p>The supplier passed to {@link #of(Supplier)} is not invoked until the first call to
 * {@link #get()}. Subsequent calls return the cached result without invoking the supplier again.
 * Memoization is thread-safe via a {@code volatile} {@link Try}{@code <T>} state field and
 * double-checked locking: the first read is lock-free; if the state is {@code null}, a
 * {@code synchronized} block is entered and the state is checked again before evaluating
 * the supplier. Both successful values and supplier exceptions are stored in the state as a
 * {@link Try}, so any exception thrown during evaluation is memoized and rethrown on every
 * subsequent {@link #get()} call without re-invoking the supplier. The thread-safety design
 * is documented in
 * <a href="https://domix.github.io/dmx-fun/adr/adr-012-lazy-double-checked-locking/">
 * ADR-012 — Lazy&lt;T&gt; with volatile Try&lt;T&gt; and double-checked locking</a>.
 *
 * <p>This type is {@code @NullMarked}: the supplier must return a non-null value.
 * Use {@code Lazy<Option<T>>} to model a lazily evaluated optional result.
 *
 * @param <T> the type of the lazily evaluated value
 */
@NullMarked
public final class Lazy<T> {

    private final Supplier<? extends T> supplier;
    /**
     * {@code null} means unevaluated; non-null holds the cached {@link Try} (success or failure).
     */
    private volatile @Nullable Try<T> state = null;

    private Lazy(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    /**
     * Creates a new {@code Lazy} that will evaluate {@code supplier} on the first call to
     * {@link #get()}.
     *
     * @param <T>      the type of the value
     * @param supplier the supplier to evaluate lazily; must not be {@code null}
     * @return a new, unevaluated {@code Lazy<T>}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public static <T> Lazy<T> of(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        return new Lazy<>(supplier);
    }

    /**
     * Returns the value, evaluating the supplier on the first call and caching the result.
     *
     * <p>The supplier is called at most once. All subsequent calls return the cached value.
     * This method is thread-safe.
     *
     * @return the (possibly cached) value; never {@code null}
     * @throws NullPointerException if the supplier returns {@code null}
     */
    public T get() {
        var s = evaluate();
        if (s.isSuccess()) {
            return s.get();
        }
        throw rethrowOrWrap(s.getCause());
    }

    /**
     * Returns {@code true} if the supplier has already been evaluated.
     *
     * @return {@code true} after the first call to {@link #get()}, {@code false} before
     */
    public boolean isEvaluated() {
        return state != null;
    }

    /**
     * Returns a new {@code Lazy} that applies {@code f} to this value when evaluated.
     *
     * <p>The mapping function is not invoked until {@link #get()} is called on the returned
     * {@code Lazy}. Evaluating the returned {@code Lazy} also evaluates this one.
     *
     * @param <R> the type of the mapped value
     * @param f   the mapping function; must not be {@code null} and must not return {@code null}
     * @return a new, unevaluated {@code Lazy<R>}
     * @throws NullPointerException if {@code f} is {@code null}
     */
    public <R> Lazy<R> map(Function<? super T, ? extends R> f) {
        Objects.requireNonNull(f, "mapper must not be null");
        return Lazy.of(() -> Objects.requireNonNull(f.apply(get()), "map function returned null"));
    }

    /**
     * Returns a new {@code Lazy} by applying {@code f} to this value and flattening the result.
     *
     * <p>Neither this value nor the function is evaluated until {@link #get()} is called on
     * the returned {@code Lazy}.
     *
     * @param <R> the type of the resulting value
     * @param f   a function that returns a {@code Lazy<R>}; must not be {@code null}
     * @return a new, unevaluated {@code Lazy<R>}
     * @throws NullPointerException if {@code f} is {@code null} or returns {@code null}
     */
    public <R> Lazy<R> flatMap(Function<? super T, Lazy<? extends R>> f) {
        Objects.requireNonNull(f, "mapper must not be null");
        return Lazy.of(() -> Objects.requireNonNull(f.apply(get()), "flatMap function returned null").get());
    }

    /**
     * Evaluates this {@code Lazy} and wraps the result in an {@link Option}.
     *
     * <p>Unlike {@link #toTry()}, this method does <em>not</em> capture exceptions —
     * any unchecked throwable ({@link RuntimeException} or {@link Error}) thrown by the
     * supplier propagates to the caller, with the same semantics as {@link #get()}.
     * Use {@code toTry().toOption()} if you need exception-safe conversion.
     *
     * @return {@code Option.some(value)}
     * @throws RuntimeException if the supplier throws a {@code RuntimeException}
     * @throws Error            if the supplier throws an {@code Error}
     */
    public Option<T> toOption() {
        return Option.some(get());
    }

    /**
     * Evaluates this {@code Lazy} inside a {@link Try}, capturing any exception thrown by the
     * supplier as a {@code Failure}.
     *
     * <p>The result is memoized: the supplier is called at most once regardless of how many
     * times this method is invoked.
     *
     * @return {@code Success(value)} if the supplier completes normally,
     * or {@code Failure(exception)} if it throws
     */
    public Try<T> toTry() {
        return evaluate();
    }

    /**
     * Creates a {@code Lazy} that defers the blocking wait for {@code future} until the first
     * call to {@link #get()}.
     *
     * <p>The future's result is obtained via {@link Try#fromFuture(CompletableFuture)}, which
     * unwraps {@link java.util.concurrent.CompletionException} transparently. If the future
     * completed exceptionally, {@link #get()} rethrows the original {@link RuntimeException} or
     * {@link Error} as-is, and wraps checked exceptions in a new {@link RuntimeException}.
     *
     * @param <T>    the type of the future's value
     * @param future the {@code CompletableFuture} to wrap; must not be {@code null}
     * @return a new, unevaluated {@code Lazy<T>}
     * @throws NullPointerException if {@code future} is {@code null}
     */
    public static <T> Lazy<T> fromFuture(CompletableFuture<? extends T> future) {
        Objects.requireNonNull(future, "future must not be null");
        return Lazy.of(
            () -> Try.<T>fromFuture(future).getOrThrow(Lazy::rethrowOrWrap)
        );
    }

    /**
     * Converts this {@code Lazy} into a {@link CompletableFuture}.
     *
     * <p>If the value has already been evaluated (i.e., {@link #isEvaluated()} is {@code true}),
     * returns an already-completed future with the cached value — no thread pool dispatch occurs.
     * Otherwise, evaluates the supplier asynchronously via {@link CompletableFuture#supplyAsync}.
     *
     * @return a {@code CompletableFuture<T>} that completes with this lazy's value
     */
    public CompletableFuture<T> toFuture() {
        var s = state;
        if (s != null) {
            if (s.isFailure()) {
                return CompletableFuture.failedFuture(s.getCause());
            }
            return CompletableFuture.completedFuture(s.get());
        }
        return CompletableFuture.supplyAsync(this::get);
    }

    /**
     * Evaluates this {@code Lazy} inside a {@link Result}, capturing any exception thrown by the
     * supplier as {@code Err(Throwable)}.
     *
     * @return {@code Ok(value)} if the supplier completes normally,
     * or {@code Err(exception)} if it throws
     */
    public Result<T, Throwable> toResult() {
        return toTry().toResult();
    }

    /**
     * Evaluates this {@code Lazy} inside a {@link Result}, converting any exception thrown by the
     * supplier into a typed error using {@code errorMapper}.
     *
     * @param <E>         the error type
     * @param errorMapper a function that converts the thrown exception to the error value;
     *                    must not be {@code null} and must not return {@code null}
     * @return {@code Ok(value)} if the supplier completes normally,
     * or {@code Err(errorMapper.apply(exception))} if it throws
     * @throws NullPointerException if {@code errorMapper} is {@code null} or returns {@code null}
     */
    public <E> Result<T, E> toResult(Function<? super Throwable, ? extends E> errorMapper) {
        return toTry()
            .toResult(errorMapper);
    }

    /**
     * Evaluates this {@code Lazy} and returns the value wrapped in a
     * {@link java.util.Optional Optional}.
     *
     * <p>Unlike {@link #toTry()}, this method does <em>not</em> capture exceptions —
     * any unchecked throwable ({@link RuntimeException} or {@link Error}) thrown by the
     * supplier propagates to the caller, with the same semantics as {@link #get()}.
     * Use {@code toTry().toOptional()} if you need exception-safe conversion.
     *
     * @return {@code Optional.of(value)}; never {@code Optional.empty()}
     * @throws RuntimeException if the supplier throws a {@code RuntimeException}
     * @throws Error            if the supplier throws an {@code Error}
     */
    public Optional<T> toOptional() {
        return Optional.of(get());
    }

    /**
     * Evaluates this {@code Lazy} inside an {@link Either}, capturing any exception thrown by the
     * supplier as {@code Left(Throwable)}.
     *
     * @return {@code Either.right(value)} if the supplier completes normally,
     *         or {@code Either.left(exception)} if it throws
     */
    public Either<Throwable, T> toEither() {
        return toTry().toEither();
    }

    /**
     * Evaluates this {@code Lazy} inside an {@link Either}, converting any exception thrown by the
     * supplier into a typed left value using {@code errorMapper}.
     *
     * @param <E>         the left (error) type
     * @param errorMapper a function that converts the thrown exception to the left value;
     *                    must not be {@code null} and must not return {@code null}
     * @return {@code Either.right(value)} if the supplier completes normally,
     *         or {@code Either.left(errorMapper.apply(exception))} if it throws
     * @throws NullPointerException if {@code errorMapper} is {@code null} or returns {@code null}
     */
    public <E> Either<E, T> toEither(Function<? super Throwable, ? extends E> errorMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper must not be null");
        var t = toTry();
        return t.isSuccess()
            ? Either.right(t.get())
            : Either.left(Objects.requireNonNull(errorMapper.apply(t.getCause()), "errorMapper returned null"));
    }

    /**
     * Returns a string representation of this {@code Lazy}.
     *
     * @return {@code "Lazy[?]"} if not yet evaluated, {@code "Lazy[value]"} if evaluated
     * successfully, or {@code "Lazy[!]"} if evaluation failed
     */
    @Override
    public String toString() {
        var s = state;
        if (s == null) {
            return "Lazy[?]";
        }
        if (s.isFailure()) {
            return "Lazy[!]";
        }
        return "Lazy[" + s.get() + "]";
    }

    // ── internals ─────────────────────────────────────────────────────────────

    /**
     * Returns the cause as a {@link RuntimeException} if it already is one, or wraps it.
     * Rethrows {@link Error} directly — {@code Error} subclasses signal unrecoverable JVM
     * conditions and must not be wrapped.
     */
    private static RuntimeException rethrowOrWrap(Throwable cause) {
        if (cause instanceof RuntimeException re) return re;
        if (cause instanceof Error e) throw e;
        return new RuntimeException(cause);
    }

    private Try<T> evaluate() {
        // Using a local variable throughout
        // would make the non-null guarantee
        // explicit and prevents warnings
        // from static analysis tools
        var s = state;
        if (s != null) {
            return s;
        }
        synchronized (this) {
            s = state;
            if (s == null) {
                s = Try.of(
                    () -> Objects.requireNonNull(
                        supplier.get(),
                        "supplier returned null"
                    )
                );
                state = s;
            }
        }
        return s;
    }
}
