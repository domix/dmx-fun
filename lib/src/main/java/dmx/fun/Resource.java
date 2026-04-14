package dmx.fun;

import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * A functional managed resource: a value that must be acquired before use and released
 * afterwards. {@code Resource<T>} is the composable alternative to {@code try-with-resources}.
 *
 * <p>Acquisition and release are declared together at construction time. The resource is only
 * live during the execution of {@link #use(CheckedFunction) use(fn)} — it is acquired just
 * before the body runs, and the release function is <em>always</em> called when the body
 * completes, whether it succeeds or throws.
 *
 * <h2>Behaviour contract</h2>
 * <ul>
 *   <li>If the body succeeds and release succeeds → {@code Try.success(result)}.</li>
 *   <li>If the body succeeds but release throws → {@code Try.failure(releaseException)}.</li>
 *   <li>If the body throws and release succeeds → {@code Try.failure(bodyException)}.</li>
 *   <li>If both the body and release throw → the release exception is <em>suppressed</em> onto
 *       the body exception (mirroring {@code try-with-resources} semantics) and
 *       {@code Try.failure(bodyException)} is returned.</li>
 * </ul>
 *
 * <h2>Composition</h2>
 * <ul>
 *   <li>{@link #map(Function) map} transforms the resource value without changing
 *       acquire/release.</li>
 *   <li>{@link #flatMap(Function) flatMap} sequences two resources; both are released in
 *       reverse acquisition order (inner first, then outer).</li>
 * </ul>
 *
 * @param <T> the type of the managed resource value
 */
@NullMarked
public final class Resource<T> {

    /**
     * Internal representation: a generic lifecycle function.
     * Implemented as an anonymous class (not a lambda) because the method carries its own
     * type parameter {@code <R>}, which Java lambdas cannot implement.
     */
    private interface Effect<T> {
        <R> Try<R> run(CheckedFunction<? super T, ? extends R> body);
    }

    private final Effect<T> effect;

    private Resource(Effect<T> effect) {
        this.effect = effect;
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code Resource<T>} from explicit acquire and release functions.
     *
     * <p>Example:
     * <pre>{@code
     * Resource<Connection> conn = Resource.of(
     *     () -> dataSource.getConnection(),
     *     Connection::close
     * );
     * }</pre>
     *
     * @param <T>     the resource type
     * @param acquire supplier that obtains the resource; may throw
     * @param release consumer that frees the resource; always called after the body
     * @return a new {@code Resource<T>}
     * @throws NullPointerException if {@code acquire} or {@code release} is {@code null}
     */
    public static <T> Resource<T> of(
            CheckedSupplier<? extends T> acquire,
            CheckedConsumer<? super T> release) {
        Objects.requireNonNull(acquire, "acquire");
        Objects.requireNonNull(release, "release");
        return new Resource<>(new Effect<>() {
            @Override
            public <R> Try<R> run(CheckedFunction<? super T, ? extends R> body) {
                T resource;
                try {
                    resource = acquire.get();
                } catch (Throwable t) {
                    return Try.failure(t);
                }
                return runBody(resource, body, release);
            }
        });
    }

    /**
     * Creates a {@code Resource<T>} from an {@link AutoCloseable} supplier.
     * The {@link AutoCloseable#close()} method is used as the release function.
     *
     * <p>Example:
     * <pre>{@code
     * Resource<BufferedReader> reader = Resource.fromAutoCloseable(
     *     () -> new BufferedReader(new FileReader(path))
     * );
     * Try<String> content = reader.use(r -> r.lines().collect(joining("\n")));
     * }</pre>
     *
     * @param <T>     the resource type, must extend {@link AutoCloseable}
     * @param acquire supplier that obtains the {@code AutoCloseable} resource; may throw
     * @return a new {@code Resource<T>}
     * @throws NullPointerException if {@code acquire} is {@code null}
     */
    public static <T extends AutoCloseable> Resource<T> fromAutoCloseable(
            CheckedSupplier<? extends T> acquire) {
        Objects.requireNonNull(acquire, "acquire");
        return of(acquire, AutoCloseable::close);
    }

    /**
     * Creates a {@code Resource<T>} from a pre-computed {@link Try Try&lt;T&gt;} and a release
     * function.
     *
     * <p>If {@code acquired} is already a failure, {@code use} returns that failure immediately
     * and the {@code release} function is <em>never called</em> — there is nothing to release.
     *
     * <p>Example:
     * <pre>{@code
     * Try<Connection> tryConn = Try.of(() -> dataSource.getConnection());
     * Resource<Connection> conn = Resource.eval(tryConn, Connection::close);
     * Try<List<User>> users = conn.use(c -> fetchUsers(c));
     * }</pre>
     *
     * @param <T>      the resource type
     * @param acquired the pre-computed result of an acquire attempt; if failure, release is skipped
     * @param release  consumer that frees the resource when acquired successfully
     * @return a new {@code Resource<T>}
     * @throws NullPointerException if {@code acquired} or {@code release} is {@code null}
     */
    public static <T> Resource<T> eval(
            Try<? extends T> acquired,
            CheckedConsumer<? super T> release) {
        Objects.requireNonNull(acquired, "acquired");
        Objects.requireNonNull(release, "release");
        return new Resource<>(new Effect<T>() {
            @Override
            public <R> Try<R> run(CheckedFunction<? super T, ? extends R> body) {
                if (acquired.isFailure()) {
                    return Try.failure(acquired.getCause());
                }
                return runBody(acquired.get(), body, release);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Core operation
    // -------------------------------------------------------------------------

    /**
     * Acquires the resource, applies {@code body} to it, releases the resource, and returns
     * the body's result wrapped in a {@link Try}.
     *
     * <p>The release function is <em>always</em> called, even when {@code body} throws.
     * See the class-level contract for the exact exception-merging rules.
     *
     * @param <R>  the result type
     * @param body function applied to the live resource; may throw
     * @return {@code Try.success(result)} on success, or {@code Try.failure(cause)} on any error
     * @throws NullPointerException if {@code body} is {@code null}
     */
    public <R> Try<R> use(CheckedFunction<? super T, ? extends R> body) {
        Objects.requireNonNull(body, "body");
        return effect.run(body);
    }

    /**
     * Acquires the resource, applies {@code body} to produce a {@link Result}, releases the
     * resource, and returns a {@code Result<R, E>}.
     *
     * <p>This is the {@code Result}-integrated variant of {@link #use(CheckedFunction) use()}.
     * It is useful when the domain layer models failures as typed {@code Result} values rather
     * than {@code Throwable}.
     *
     * <ul>
     *   <li>If acquire or release throws a {@code Throwable}, it is mapped to {@code E} via
     *       {@code onError} and returned as {@code Result.err(e)}.</li>
     *   <li>If the body returns {@code Result.err(e)}, that error is returned as-is.</li>
     *   <li>If both body and release fail, the release exception is suppressed onto the body
     *       exception and the combined throwable is passed to {@code onError}.</li>
     * </ul>
     *
     * <p>Example:
     * <pre>{@code
     * Result<List<User>, DbError> users = connResource.useAsResult(
     *     conn -> fetchUsers(conn),
     *     ex   -> new DbError.QueryFailed(ex.getMessage())
     * );
     * }</pre>
     *
     * @param <R>     the success type
     * @param <E>     the error type
     * @param body    function applied to the live resource; returns a {@code Result}
     * @param onError maps any {@code Throwable} from acquire/release/body to {@code E}
     * @return {@code Result.ok(value)} on success, or {@code Result.err(error)} on any failure
     * @throws NullPointerException if {@code body} or {@code onError} is {@code null}
     */
    public <R, E> Result<R, E> useAsResult(
            Function<? super T, ? extends Result<? extends R, ? extends E>> body,
            Function<? super Throwable, ? extends E> onError) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(onError, "onError");
        Try<Result<R, E>> tryResult = use(t -> {
            @SuppressWarnings("unchecked")
            Result<R, E> r = (Result<R, E>) Objects.requireNonNull(
                body.apply(t), "useAsResult body must not return null");
            return r;
        });
        if (tryResult.isSuccess()) {
            return tryResult.get();
        }
        return Result.err(onError.apply(tryResult.getCause()));
    }

    // -------------------------------------------------------------------------
    // Transformations
    // -------------------------------------------------------------------------

    /**
     * Returns a new {@code Resource<R>} whose body receives the result of applying {@code fn}
     * to the acquired value. The acquire/release lifecycle of the underlying resource is
     * unchanged.
     *
     * <p>If {@code fn} throws, the underlying resource is still released and the exception is
     * captured as a {@code Try.failure}.
     *
     * @param <R> the mapped resource type
     * @param fn  function transforming the acquired value; must not be {@code null}
     * @return a new {@code Resource<R>}
     * @throws NullPointerException if {@code fn} is {@code null}
     */
    public <R> Resource<R> map(Function<? super T, ? extends R> fn) {
        Objects.requireNonNull(fn, "fn");
        Effect<T> self = this.effect;
        return new Resource<>(new Effect<>() {
            @Override
            public <S> Try<S> run(CheckedFunction<? super R, ? extends S> body) {
                return self.run(
                    t -> body.apply(fn.apply(t))
                );
            }
        });
    }

    /**
     * Sequences this resource with an inner resource derived from its value.
     * Both resources are released in reverse acquisition order: the inner resource is
     * released first, then this (outer) resource.
     *
     * <p>Example — connection then prepared statement:
     * <pre>{@code
     * Resource<PreparedStatement> stmt = connResource.flatMap(c ->
     *     Resource.of(
     *         () -> c.prepareStatement("SELECT * FROM users"),
     *         PreparedStatement::close
     *     )
     * );
     * Try<List<User>> result = stmt.use(ps -> mapRows(ps.executeQuery()));
     * }</pre>
     *
     * @param <R> the inner resource type
     * @param fn  function that produces the inner resource from this resource's value;
     *            must not be {@code null} and must not return {@code null}
     * @return a composed {@code Resource<R>} whose lifecycle manages both resources
     * @throws NullPointerException if {@code fn} is {@code null} or returns {@code null}
     */
    public <R> Resource<R> flatMap(Function<? super T, ? extends Resource<R>> fn) {
        Objects.requireNonNull(fn, "fn");
        Effect<T> self = this.effect;
        return new Resource<>(new Effect<>() {
            @Override
            public <S> Try<S> run(CheckedFunction<? super R, ? extends S> body) {
                // Run the outer lifecycle with a body that:
                //   1. Creates the inner resource from T
                //   2. Runs the inner resource with the user's body
                //   3. If inner failed, rethrows so the outer Effect captures it as bodyEx
                //      (and still releases the outer resource)
                //   4. If inner succeeded, returns the result so the outer Effect returns success
                return self.run(t -> {
                    var inner = Objects.requireNonNull(
                        fn.apply(t), "flatMap fn must not return null");
                    var innerResult = inner.use(body);
                    if (innerResult.isFailure()) {
                        sneakyThrow(innerResult.getCause());
                    }
                    return innerResult.get();
                });
            }
        });
    }

    /**
     * Returns a new {@code Resource<R>} whose value is obtained by applying a
     * {@link Try}-returning function to the acquired value.
     *
     * <p>This is the {@link Try}-integrated counterpart of {@link #map(Function) map()}.
     * It is useful when the transformation itself is a fallible operation already wrapped in
     * a {@code Try} (e.g., parsing, validation, or a call to a {@code Try.of(...)}-wrapped API).
     * If {@code fn} returns a failure, the underlying resource is still released and the
     * failure is propagated.
     *
     * <p>Example:
     * <pre>{@code
     * Resource<Config> config = rawTextResource.mapTry(text ->
     *     Try.of(() -> Config.parse(text))
     * );
     * Try<Integer> port = config.use(c -> c.port());
     * }</pre>
     *
     * @param <R> the mapped resource type
     * @param fn  function returning a {@code Try<R>}; must not be {@code null} or return
     *            {@code null}
     * @return a new {@code Resource<R>}
     * @throws NullPointerException if {@code fn} is {@code null}
     */
    public <R> Resource<R> mapTry(Function<? super T, ? extends Try<? extends R>> fn) {
        Objects.requireNonNull(fn, "fn");
        Effect<T> self = this.effect;
        return new Resource<>(new Effect<>() {
            @Override
            public <S> Try<S> run(CheckedFunction<? super R, ? extends S> body) {
                return self.run(t -> {
                    Try<? extends R> inner = Objects.requireNonNull(
                        fn.apply(t), "mapTry fn must not return null");
                    if (inner.isFailure()) {
                        sneakyThrow(inner.getCause());
                    }
                    return body.apply(inner.get());
                });
            }
        });
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Runs {@code body} with {@code resource}, always calls {@code release}, and merges
     * exceptions according to the class-level contract.
     */
    private static <T, R> Try<R> runBody(
            T resource,
            CheckedFunction<? super T, ? extends R> body,
            CheckedConsumer<? super T> release
    ) {
        Throwable bodyEx = null;
        R result = null;
        try {
            result = body.apply(resource);
        } catch (Throwable t) {
            bodyEx = t;
        }
        try {
            release.accept(resource);
        } catch (Throwable releaseEx) {
            if (bodyEx != null) {
                bodyEx.addSuppressed(releaseEx);
            } else {
                bodyEx = releaseEx;
            }
        }
        return bodyEx != null ? Try.failure(bodyEx) : Try.success(result);
    }

    /** Bypasses the checked-exception compiler check to rethrow any {@link Throwable}. */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }
}
