package dmx.fun.spring.webflux;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Validated;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end body checks through a real functional route with {@link WebTestClient}. */
class WebfluxFunRouteTest {

    private final WebTestClient client = WebTestClient.bindToRouterFunction(routes()).build();

    private static RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
            .GET("/result", request -> WebfluxFun.fromResult(
                Mono.just(Result.<String, String>ok("hi")),
                error -> ServerResponse.badRequest().bodyValue(error)))
            .GET("/missing", request -> WebfluxFun.fromOption(Mono.just(Option.<String>none())))
            .GET("/invalid", request -> WebfluxFun.fromValidated(
                Mono.just(Validated.invalid(NonEmptyList.of("must not be blank", List.of("too short"))))))
            .GET("/accumulate", request -> WebfluxFun.fromResultStreamAccumulating(
                Flux.just(Result.<Integer, String>ok(1), Result.err("e1"), Result.err("e2"))))
            .GET("/accumulate-ok", request -> WebfluxFun.fromResultStreamAccumulating(
                Flux.just(Result.<Integer, String>ok(1), Result.ok(2))))
            .GET("/stream", request -> WebfluxFun.stream(
                Flux.just("a", "b", "c"), MediaType.APPLICATION_NDJSON, String.class))
            .GET("/stream-error", request -> WebfluxFun.stream(
                Flux.just("a").concatWith(Flux.error(new IllegalStateException("boom"))),
                MediaType.APPLICATION_NDJSON, String.class, throwable -> "FALLBACK"))
            .build();
    }

    @Test
    void ok_returns200WithBody() {
        client.get().uri("/result").exchange()
            .expectStatus().isOk()
            .expectBody(String.class).isEqualTo("hi");
    }

    @Test
    void none_returns404() {
        client.get().uri("/missing").exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void invalid_returns400WithAllErrors() {
        String body = client.get().uri("/invalid").exchange()
            .expectStatus().isBadRequest()
            .expectBody(String.class).returnResult().getResponseBody();
        assertThat(body).contains("must not be blank", "too short");
    }

    @Test
    void accumulate_returns400WithEveryError() {
        String body = client.get().uri("/accumulate").exchange()
            .expectStatus().isBadRequest()
            .expectBody(String.class).returnResult().getResponseBody();
        assertThat(body).contains("e1", "e2");
    }

    @Test
    void accumulate_allOk_returns200WithList() {
        String body = client.get().uri("/accumulate-ok").exchange()
            .expectStatus().isOk()
            .expectBody(String.class).returnResult().getResponseBody();
        assertThat(body).contains("1", "2");
    }

    @Test
    void stream_returns200WithNdjsonBody() {
        String body = client.get().uri("/stream").exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .expectBody(String.class).returnResult().getResponseBody();
        assertThat(body).contains("a", "b", "c");
    }

    @Test
    void stream_onError_appendsFallbackElement() {
        String body = client.get().uri("/stream-error").exchange()
            .expectStatus().isOk()
            .expectBody(String.class).returnResult().getResponseBody();
        assertThat(body).contains("a", "FALLBACK");
    }
}
