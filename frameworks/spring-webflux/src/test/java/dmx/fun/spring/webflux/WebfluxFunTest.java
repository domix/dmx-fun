package dmx.fun.spring.webflux;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebfluxFunTest {

    private static final ErrorHttpMapper<String> ERROR_400 =
        e -> ServerResponse.badRequest().bodyValue(e);
    private static final ThrowableHttpMapper FAILURE_500 =
        t -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(String.valueOf(t.getMessage()));

    private static HttpStatus status(Mono<ServerResponse> response) {
        return HttpStatus.valueOf(response.block().statusCode().value());
    }

    // ── fromResult ─────────────────────────────────────────────────────────────

    @Test
    void fromResult_ok_is200() {
        assertThat(status(WebfluxFun.fromResult(Mono.just(Result.<String, String>ok("x")), ERROR_400)))
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    void fromResult_err_usesErrorMapper() {
        assertThat(status(WebfluxFun.fromResult(Mono.just(Result.<String, String>err("bad")), ERROR_400)))
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void fromResult_emptyMono_is404() {
        assertThat(status(WebfluxFun.fromResult(Mono.empty(), ERROR_400)))
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── fromOption ─────────────────────────────────────────────────────────────

    @Test
    void fromOption_some_is200() {
        assertThat(status(WebfluxFun.fromOption(Mono.just(Option.some("x")))))
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    void fromOption_none_is404() {
        assertThat(status(WebfluxFun.fromOption(Mono.just(Option.<String>none()))))
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void fromOption_emptyMono_is404() {
        assertThat(status(WebfluxFun.fromOption(Mono.empty())))
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── fromTry ────────────────────────────────────────────────────────────────

    @Test
    void fromTry_success_is200() {
        assertThat(status(WebfluxFun.fromTry(Mono.just(Try.success("x")), FAILURE_500)))
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    void fromTry_failure_usesFailureMapper() {
        Mono<Try<String>> failed = Mono.just(Try.failure(new IOException("disk")));
        assertThat(status(WebfluxFun.fromTry(failed, FAILURE_500)))
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ── fromValidated ──────────────────────────────────────────────────────────

    @Test
    void fromValidated_valid_is200() {
        Mono<Validated<NonEmptyList<String>, String>> valid = Mono.just(Validated.valid("alice"));
        assertThat(status(WebfluxFun.fromValidated(valid))).isEqualTo(HttpStatus.OK);
    }

    @Test
    void fromValidated_invalid_is400() {
        Mono<Validated<NonEmptyList<String>, String>> invalid =
            Mono.just(Validated.invalid(NonEmptyList.of("must not be blank", List.of("too short"))));
        assertThat(status(WebfluxFun.fromValidated(invalid))).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── fromResultStream ───────────────────────────────────────────────────────

    @Test
    void fromResultStream_allOk_is200() {
        Flux<Result<Integer, String>> stream = Flux.just(Result.ok(1), Result.ok(2));
        assertThat(status(WebfluxFun.fromResultStream(stream, ERROR_400))).isEqualTo(HttpStatus.OK);
    }

    @Test
    void fromResultStream_firstErr_usesErrorMapper() {
        Flux<Result<Integer, String>> stream = Flux.just(Result.ok(1), Result.err("bad"));
        assertThat(status(WebfluxFun.fromResultStream(stream, ERROR_400))).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── fromResultStreamAccumulating ───────────────────────────────────────────

    @Test
    void fromResultStreamAccumulating_allOk_is200() {
        Flux<Result<Integer, String>> stream = Flux.just(Result.ok(1), Result.ok(2));
        assertThat(status(WebfluxFun.fromResultStreamAccumulating(stream))).isEqualTo(HttpStatus.OK);
    }

    @Test
    void fromResultStreamAccumulating_withErrors_is400() {
        Flux<Result<Integer, String>> stream = Flux.just(Result.ok(1), Result.err("e1"), Result.err("e2"));
        assertThat(status(WebfluxFun.fromResultStreamAccumulating(stream))).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── stream ─────────────────────────────────────────────────────────────────

    @Test
    void stream_setsOkStatusAndContentType() {
        ServerResponse response =
            WebfluxFun.stream(Flux.just("a", "b"), MediaType.APPLICATION_NDJSON, String.class).block();
        assertThat(HttpStatus.valueOf(response.statusCode().value())).isEqualTo(HttpStatus.OK);
        assertThat(response.headers().getContentType()).isEqualTo(MediaType.APPLICATION_NDJSON);
    }

    // ── null guards ────────────────────────────────────────────────────────────

    @Test
    void fromResult_nullSource_throws() {
        assertThatThrownBy(() -> WebfluxFun.fromResult(null, ERROR_400))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("source");
    }

    @Test
    void fromResult_nullErrorMapper_throws() {
        assertThatThrownBy(() -> WebfluxFun.fromResult(Mono.just(Result.<String, String>ok("x")), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("errorMapper");
    }
}
