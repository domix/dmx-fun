/**
 * {@code java.net.http.HttpClient} wrapper for dmx-fun.
 *
 * <p>Wraps the JDK's built-in HTTP client so that both network failures and
 * HTTP error status codes are surfaced as a single typed {@link dmx.fun.Result}
 * rather than a mix of exceptions and status-code checks:
 *
 * <pre>{@code
 * DmxHttpClient client = DmxHttpClient.of(HttpClient.newHttpClient());
 *
 * Result<String, HttpError> result =
 *     client.send(request, BodyHandlers.ofString());
 *
 * result
 *     .peek(body -> System.out.println("OK: " + body))
 *     .peekError(err -> System.err.println("Failed: " + err));
 * }</pre>
 *
 * <p>Error categories — {@link dmx.fun.http.HttpError}:
 * <ul>
 *   <li>{@link dmx.fun.http.HttpError.ClientError} — 4xx responses</li>
 *   <li>{@link dmx.fun.http.HttpError.ServerError} — 5xx responses</li>
 *   <li>{@link dmx.fun.http.HttpError.Timeout}     — {@link java.net.http.HttpTimeoutException}</li>
 *   <li>{@link dmx.fun.http.HttpError.NetworkFailure} — {@link java.io.IOException} / {@link InterruptedException}</li>
 * </ul>
 */
module dmx.fun.http {
    requires transitive dmx.fun;
    requires java.net.http;
    requires static org.jspecify;

    exports dmx.fun.http;
}
