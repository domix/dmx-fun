package dmx.fun.spring.webflux;

import dmx.fun.NonEmptyList;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** End-to-end checks that WebfluxProblem renders RFC 7807 problem responses via {@link WebTestClient}. */
class WebfluxProblemTest {

    private final WebTestClient client = WebTestClient.bindToRouterFunction(routes()).build();

    private static RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
            .GET("/result-err", request -> WebfluxFun.fromResult(
                Mono.just(Result.<String, String>err("not there")),
                WebfluxProblem.problemDetail(HttpStatus.NOT_FOUND, error -> error)))
            .GET("/try-fail", request -> WebfluxFun.fromTry(
                Mono.just(Try.<String>failure(new IllegalStateException("boom"))),
                WebfluxProblem.problemDetail(HttpStatus.INTERNAL_SERVER_ERROR)))
            .GET("/try-fail-nomsg", request -> WebfluxFun.fromTry(
                Mono.just(Try.<String>failure(new IllegalStateException())),
                WebfluxProblem.problemDetail(HttpStatus.INTERNAL_SERVER_ERROR)))
            .GET("/validated-invalid", request -> WebfluxProblem.fromValidated(
                Mono.just(Validated.invalid(NonEmptyList.of("must not be blank", List.of("too short"))))))
            .GET("/validated-valid", request -> WebfluxProblem.fromValidated(
                Mono.just(Validated.<NonEmptyList<String>, String>valid("ok"))))
            .build();
    }

    @Test
    void resultErr_returnsProblemDetailWithStatusAndDetail() {
        String body = client.get().uri("/result-err").exchange()
            .expectStatus().isNotFound()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class).returnResult().getResponseBody();
        assertThat(body).contains("\"status\":404", "not there");
    }

    @Test
    void tryFailure_returnsProblemDetailFromThrowable() {
        String body = client.get().uri("/try-fail").exchange()
            .expectStatus().is5xxServerError()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class).returnResult().getResponseBody();
        assertThat(body).contains("\"status\":500", "boom");
    }

    @Test
    void validatedInvalid_returns400ProblemWithEveryError() {
        String body = client.get().uri("/validated-invalid").exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class).returnResult().getResponseBody();
        assertThat(body).contains("\"status\":400", "errors", "must not be blank", "too short");
    }

    @Test
    void validatedValid_returns200WithValue() {
        client.get().uri("/validated-valid").exchange()
            .expectStatus().isOk()
            .expectBody(String.class).isEqualTo("ok");
    }

    @Test
    void tryFailure_nullMessage_doesNotRenderLiteralNull() {
        String body = client.get().uri("/try-fail-nomsg").exchange()
            .expectStatus().is5xxServerError()
            .expectBody(String.class).returnResult().getResponseBody();
        assertThat(body).doesNotContain("\"detail\":\"null\"");
    }

    // ── null guards ────────────────────────────────────────────────────────────

    @Test
    void problemDetail_nullStatus_throws() {
        assertThatThrownBy(() -> WebfluxProblem.problemDetail(null, (String e) -> e))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("status");
    }

    @Test
    void problemDetail_nullDetail_throws() {
        assertThatThrownBy(() -> WebfluxProblem.<String>problemDetail(HttpStatus.BAD_REQUEST, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("detail");
    }

    @Test
    void problemDetailFromThrowable_nullStatus_throws() {
        assertThatThrownBy(() -> WebfluxProblem.problemDetail((HttpStatus) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("status");
    }

    @Test
    void fromValidated_nullSource_throws() {
        assertThatThrownBy(() -> WebfluxProblem.fromValidated(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("source");
    }
}
