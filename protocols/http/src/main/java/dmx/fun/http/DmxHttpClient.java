package dmx.fun.http;

import dmx.fun.CheckedFunction;
import dmx.fun.Result;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.jspecify.annotations.NullMarked;

/**
 * A thin wrapper around {@link HttpClient} that returns {@link Result}{@code <T, HttpError>}
 * instead of throwing exceptions.
 *
 * <p>Both network failures ({@link IOException}, {@link InterruptedException}) and
 * HTTP error status codes (4xx, 5xx) are surfaced as typed {@link HttpError} variants,
 * collapsing two separate failure modes into one:
 *
 * <pre>{@code
 * DmxHttpClient client = DmxHttpClient.of(HttpClient.newHttpClient());
 *
 * client.send(request, BodyHandlers.ofString())
 *     .peek(body -> process(body))
 *     .peekError(err -> log.warn("HTTP call failed: {}", err));
 * }</pre>
 *
 * <h2>Limitation: null-body handlers are not supported</h2>
 * <p>Body handlers that produce a {@code null} body — specifically
 * {@link HttpResponse.BodyHandlers#discarding()} — are not supported.
 * {@code Result.ok()} does not accept {@code null} values, so passing a null-body handler
 * will throw {@link NullPointerException} on a successful response.
 *
 * <p>If you need to send a request and ignore the response body, use the plain
 * {@link HttpClient} directly, or wait for a future {@code sendDiscarding()} overload.
 */
@NullMarked
public final class DmxHttpClient {

    private final HttpClient client;

    private DmxHttpClient(HttpClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    /**
     * Creates a new {@code DmxHttpClient} wrapping the given {@link HttpClient}.
     *
     * @param client the underlying JDK HTTP client; must not be {@code null}
     * @return a new {@code DmxHttpClient}
     */
    public static DmxHttpClient of(HttpClient client) {
        return new DmxHttpClient(client);
    }

    /**
     * Sends an HTTP request synchronously and returns the result.
     *
     * <p>Status mapping:
     * <ul>
     *   <li>2xx / 3xx → {@code Result.ok(body)}</li>
     *   <li>4xx        → {@code Result.err(ClientError)}</li>
     *   <li>5xx        → {@code Result.err(ServerError)}</li>
     *   <li>{@link HttpTimeoutException} → {@code Result.err(Timeout)}</li>
     *   <li>{@link IOException} / {@link InterruptedException} → {@code Result.err(NetworkFailure)}</li>
     * </ul>
     *
     * @param <T>         the body type
     * @param request     the request to send; must not be {@code null}
     * @param bodyHandler the response body handler; must not be {@code null}
     * @return a {@code Result} wrapping the body or an {@link HttpError}
     */
    public <T> Result<T, HttpError> send(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        try {
            return mapResponse(client.send(request, bodyHandler));
        } catch (HttpTimeoutException e) {
            return Result.err(new HttpError.Timeout(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.err(new HttpError.NetworkFailure(e));
        } catch (IOException e) {
            return Result.err(new HttpError.NetworkFailure(e));
        }
    }

    /**
     * Sends an HTTP request synchronously, then applies {@code deserializer} to the body.
     *
     * <p>If the HTTP call succeeds but {@code deserializer} throws, the exception is wrapped
     * in a {@link HttpError.NetworkFailure}.
     *
     * @param <T>          the raw body type produced by {@code bodyHandler}
     * @param <R>          the deserialized value type
     * @param request      the request to send; must not be {@code null}
     * @param bodyHandler  the response body handler; must not be {@code null}
     * @param deserializer function applied to the raw body on success; must not be {@code null}
     * @return a {@code Result} wrapping the deserialized value or an {@link HttpError}
     */
    public <T, R> Result<R, HttpError> send(
            HttpRequest request,
            HttpResponse.BodyHandler<T> bodyHandler,
            CheckedFunction<T, R> deserializer) {
        Objects.requireNonNull(deserializer, "deserializer");
        return send(request, bodyHandler).flatMap(body -> {
            try {
                return Result.ok(deserializer.apply(body));
            } catch (Exception e) {
                return Result.err(new HttpError.NetworkFailure(e));
            }
        });
    }

    /**
     * Sends an HTTP request asynchronously and returns a {@link CompletableFuture} that completes
     * with a {@link Result}.
     *
     * <p>The returned future never completes exceptionally — transport failures are wrapped in
     * {@link HttpError.NetworkFailure} or {@link HttpError.Timeout}.
     *
     * @param <T>         the body type
     * @param request     the request to send; must not be {@code null}
     * @param bodyHandler the response body handler; must not be {@code null}
     * @return a future that completes with a {@code Result} wrapping the body or an {@link HttpError}
     */
    public <T> CompletableFuture<Result<T, HttpError>> sendAsync(
            HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        return client.sendAsync(request, bodyHandler)
            .handle((response, throwable) -> {
                if (throwable != null) {
                    var cause = throwable instanceof CompletionException ce ? ce.getCause() : throwable;
                    if (cause instanceof HttpTimeoutException hte) {
                        return Result.err(new HttpError.Timeout(hte));
                    }
                    return Result.err(new HttpError.NetworkFailure(cause));
                }
                return mapResponse(response);
            });
    }

    private <T> Result<T, HttpError> mapResponse(HttpResponse<T> response) {
        int status = response.statusCode();
        if (status >= 500) {
            return Result.err(new HttpError.ServerError(status, response));
        }
        if (status >= 400) {
            return Result.err(new HttpError.ClientError(status, response));
        }
        return Result.ok(response.body());
    }
}
