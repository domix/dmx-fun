package dmx.fun.http;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DmxHttpClientTest {

    private HttpServer server;
    private DmxHttpClient client;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
        client = DmxHttpClient.of(HttpClient.newHttpClient());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private void handle(String path, int status, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes();
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    private void handleSlow(String path, long delayMs) {
        server.createContext(path, exchange -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = "late".getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    private HttpRequest requestWithTimeout(String path, Duration timeout) {
        return HttpRequest.newBuilder(uri(path)).GET().timeout(timeout).build();
    }

    // ── null contracts ────────────────────────────────────────────────────────

    @Nested
    class NullContracts {

        @Test
        void of_nullClient_throwsNullPointerException() {
            assertThatThrownBy(() -> DmxHttpClient.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("client");
        }

        @Test
        void send_nullDeserializer_throwsNullPointerException() {
            assertThatThrownBy(() ->
                client.send(request("/ok"), BodyHandlers.ofString(), null)
            ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("deserializer");
        }
    }

    // ── send(request, bodyHandler) ────────────────────────────────────────────

    @Nested
    class Send {

        @Test
        void status200ReturnsOk() {
            handle("/ok", 200, "hello");
            var result = client.send(request("/ok"), BodyHandlers.ofString());
            assertThat(result).isOk();
            assertThat(result).containsValue("hello");
        }

        @Test
        void status201ReturnsOk() {
            handle("/created", 201, "created");
            var result = client.send(request("/created"), BodyHandlers.ofString());
            assertThat(result).isOk();
        }

        @Test
        void status400ReturnsClientError() {
            handle("/bad-request", 400, "bad request");
            var result = client.send(request("/bad-request"), BodyHandlers.ofString());
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.ClientError.class);
            assertThat(((HttpError.ClientError) result.getError()).statusCode()).isEqualTo(400);
        }

        @Test
        void status404ReturnsClientError() {
            handle("/not-found", 404, "not found");
            var result = client.send(request("/not-found"), BodyHandlers.ofString());
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.ClientError.class);
            assertThat(((HttpError.ClientError) result.getError()).statusCode()).isEqualTo(404);
        }

        @Test
        void status500ReturnsServerError() {
            handle("/error", 500, "internal error");
            var result = client.send(request("/error"), BodyHandlers.ofString());
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.ServerError.class);
            assertThat(((HttpError.ServerError) result.getError()).statusCode()).isEqualTo(500);
        }

        @Test
        void status503ReturnsServerError() {
            handle("/unavailable", 503, "unavailable");
            var result = client.send(request("/unavailable"), BodyHandlers.ofString());
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.ServerError.class);
        }

        @Test
        void networkFailureReturnsNetworkFailureError() {
            // Stop server to force a connection-refused IOException
            server.stop(0);
            var result = client.send(request("/gone"), BodyHandlers.ofString());
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.NetworkFailure.class);
        }

        @Test
        void clientErrorResponseIncludesHttpResponse() {
            handle("/forbidden", 403, "forbidden");
            var result = client.send(request("/forbidden"), BodyHandlers.ofString());
            assertThat(result).isErr();
            var error = (HttpError.ClientError) result.getError();
            assertThat(error.response()).isNotNull();
            assertThat(error.response().statusCode()).isEqualTo(403);
        }

        @Test
        void requestTimeoutReturnsTimeout() {
            handleSlow("/slow", 500);
            var result = client.send(
                requestWithTimeout("/slow", Duration.ofMillis(50)),
                BodyHandlers.ofString()
            );
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.Timeout.class);
        }

        @Test
        void timeoutErrorExposesCause() {
            handleSlow("/slow-cause", 500);
            var result = client.send(
                requestWithTimeout("/slow-cause", Duration.ofMillis(50)),
                BodyHandlers.ofString()
            );
            assertThat(result).isErr();
            var timeout = (HttpError.Timeout) result.getError();
            assertThat(timeout.cause()).isNotNull();
        }

        @Test
        void serverErrorExposesStatusCodeAndResponse() {
            handle("/err", 503, "unavailable");
            var result = client.send(request("/err"), BodyHandlers.ofString());
            assertThat(result).isErr();
            var error = (HttpError.ServerError) result.getError();
            assertThat(error.statusCode()).isEqualTo(503);
            assertThat(error.response()).isNotNull();
            assertThat(error.response().statusCode()).isEqualTo(503);
        }

        @Test
        void interruptedThreadMapsToNetworkFailureAndRestoresInterruptFlag() {
            handle("/ok", 200, "hello");
            Thread.currentThread().interrupt();
            var result = client.send(request("/ok"), BodyHandlers.ofString());
            // Our code calls Thread.currentThread().interrupt() to restore the flag;
            // Thread.interrupted() reads and clears it so subsequent tests are not affected.
            assertThat(Thread.interrupted()).isTrue();
            assertThat(result).isErr();
            var failure = (HttpError.NetworkFailure) result.getError();
            assertThat(failure.cause()).isInstanceOf(InterruptedException.class);
        }
    }

    // ── send(request, bodyHandler, deserializer) ──────────────────────────────

    @Nested
    class SendWithDeserializer {

        @Test
        void deserializerAppliedOnSuccess() {
            handle("/number", 200, "42");
            var result = client.send(request("/number"), BodyHandlers.ofString(), Integer::parseInt);
            assertThat(result).isOk();
            assertThat(result).containsValue(42);
        }

        @Test
        void deserializerExceptionBecomesNetworkFailure() {
            handle("/not-a-number", 200, "NaN");
            var result = client.send(request("/not-a-number"), BodyHandlers.ofString(), Integer::parseInt);
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.NetworkFailure.class);
        }

        @Test
        void httpErrorSkipsDeserializer() {
            handle("/fail", 500, "boom");
            var result = client.send(request("/fail"), BodyHandlers.ofString(), body -> {
                throw new AssertionError("deserializer must not be called on HTTP error");
            });
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.ServerError.class);
        }
    }

    // ── sendAsync(request, bodyHandler) ──────────────────────────────────────

    @Nested
    class SendAsync {

        @Test
        void status200ReturnsOk() throws Exception {
            handle("/async-ok", 200, "async-hello");
            var result = client.sendAsync(request("/async-ok"), BodyHandlers.ofString()).get();
            assertThat(result).isOk();
            assertThat(result).containsValue("async-hello");
        }

        @Test
        void status404ReturnsClientError() throws Exception {
            handle("/async-404", 404, "not found");
            var result = client.sendAsync(request("/async-404"), BodyHandlers.ofString()).get();
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.ClientError.class);
        }

        @Test
        void status500ReturnsServerError() throws Exception {
            handle("/async-500", 500, "server error");
            var result = client.sendAsync(request("/async-500"), BodyHandlers.ofString()).get();
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.ServerError.class);
        }

        @Test
        void networkFailureReturnsNetworkFailureError() throws Exception {
            server.stop(0);
            var result = client.sendAsync(request("/gone"), BodyHandlers.ofString()).get();
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.NetworkFailure.class);
        }

        @Test
        void networkFailureExposesCause() throws Exception {
            server.stop(0);
            var result = client.sendAsync(request("/gone"), BodyHandlers.ofString()).get();
            assertThat(result).isErr();
            var failure = (HttpError.NetworkFailure) result.getError();
            assertThat(failure.cause()).isNotNull();
        }

        @Test
        void futureNeverCompletesExceptionally() throws Exception {
            server.stop(0);
            var future = client.sendAsync(request("/gone"), BodyHandlers.ofString());
            future.get(5, TimeUnit.SECONDS);
            assertThat(future.isDone()).isTrue();
            assertThat(future.isCompletedExceptionally()).isFalse();
        }

        @Test
        void requestTimeoutReturnsTimeout() throws Exception {
            handleSlow("/async-slow", 500);
            var result = client.sendAsync(
                requestWithTimeout("/async-slow", Duration.ofMillis(50)),
                BodyHandlers.ofString()
            ).get();
            assertThat(result).isErr();
            assertThat(result.getError()).isInstanceOf(HttpError.Timeout.class);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpRequest request(String path) {
        return HttpRequest.newBuilder(uri(path)).GET().build();
    }
}
