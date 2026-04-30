package dmx.fun.http;

import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import org.jspecify.annotations.NullMarked;

/**
 * Typed failure hierarchy for {@link DmxHttpClient}.
 *
 * <p>Use exhaustive {@code switch} (or {@code instanceof} checks) to handle all
 * failure categories without a fallthrough:
 *
 * <pre>{@code
 * switch (error) {
 *     case HttpError.ClientError  e -> log.warn("4xx {}", e.statusCode());
 *     case HttpError.ServerError  e -> log.error("5xx {}", e.statusCode());
 *     case HttpError.Timeout      e -> log.warn("timed out", e.cause());
 *     case HttpError.NetworkFailure e -> log.error("transport failure", e.cause());
 * }
 * }</pre>
 */
@NullMarked
public sealed interface HttpError
        permits HttpError.ClientError, HttpError.ServerError, HttpError.Timeout, HttpError.NetworkFailure {

    /**
     * 4xx — bad request, unauthorized, not found, etc.
     *
     * @param statusCode the HTTP status code (400–499)
     * @param response   the full HTTP response
     */
    record ClientError(int statusCode, HttpResponse<?> response) implements HttpError {}

    /**
     * 5xx — internal server error, bad gateway, service unavailable, etc.
     *
     * @param statusCode the HTTP status code (500–599)
     * @param response   the full HTTP response
     */
    record ServerError(int statusCode, HttpResponse<?> response) implements HttpError {}

    /**
     * {@link HttpTimeoutException} thrown by the underlying {@code HttpClient}.
     *
     * @param cause the timeout exception
     */
    record Timeout(HttpTimeoutException cause) implements HttpError {}

    /**
     * {@link java.io.IOException}, {@link InterruptedException}, or any other
     * transport-level failure that prevented a response from being received.
     *
     * @param cause the underlying exception
     */
    record NetworkFailure(Throwable cause) implements HttpError {}
}
