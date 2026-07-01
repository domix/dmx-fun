package dmx.fun.spring.webflux;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class WebfluxEntityTest {

    private static final java.util.function.Function<String, ResponseEntity<?>> ERROR_400 =
        e -> ResponseEntity.badRequest().body(e);
    private static final java.util.function.Function<Throwable, ResponseEntity<?>> FAILURE_500 =
        t -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(t.getMessage());

    // ── fromOption ─────────────────────────────────────────────────────────────

    @Test
    void fromOption_some_is200WithBody() {
        ResponseEntity<String> response = WebfluxEntity.fromOption(Mono.just(Option.some("x"))).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("x");
    }

    @Test
    void fromOption_none_is404() {
        ResponseEntity<String> response = WebfluxEntity.fromOption(Mono.just(Option.<String>none())).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void fromOption_empty_is404() {
        ResponseEntity<String> response = WebfluxEntity.fromOption(Mono.<Option<String>>empty()).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── fromResult ─────────────────────────────────────────────────────────────

    @Test
    void fromResult_ok_is200WithBody() {
        ResponseEntity<?> response =
            WebfluxEntity.fromResult(Mono.just(Result.<String, String>ok("x")), ERROR_400).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("x");
    }

    @Test
    void fromResult_err_usesErrorMapper() {
        ResponseEntity<?> response =
            WebfluxEntity.fromResult(Mono.just(Result.<String, String>err("bad")), ERROR_400).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("bad");
    }

    @Test
    void fromResult_empty_is404() {
        ResponseEntity<?> response =
            WebfluxEntity.fromResult(Mono.<Result<String, String>>empty(), ERROR_400).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── fromTry ────────────────────────────────────────────────────────────────

    @Test
    void fromTry_success_is200WithBody() {
        ResponseEntity<?> response =
            WebfluxEntity.fromTry(Mono.just(Try.success("x")), FAILURE_500).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("x");
    }

    @Test
    void fromTry_failure_usesFailureMapper() {
        ResponseEntity<?> response =
            WebfluxEntity.fromTry(Mono.just(Try.<String>failure(new RuntimeException("boom"))), FAILURE_500).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("boom");
    }

    @Test
    void fromTry_empty_is404() {
        ResponseEntity<?> response =
            WebfluxEntity.fromTry(Mono.<Try<String>>empty(), FAILURE_500).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── fromValidated ──────────────────────────────────────────────────────────

    @Test
    void fromValidated_valid_is200WithBody() {
        ResponseEntity<?> response = WebfluxEntity.fromValidated(
            Mono.just(Validated.<NonEmptyList<String>, String>valid("x"))).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("x");
    }

    @Test
    void fromValidated_invalid_is400WithAllErrors() {
        NonEmptyList<String> errors = NonEmptyList.of("a", List.of("b"));
        ResponseEntity<?> response = WebfluxEntity.fromValidated(
            Mono.just(Validated.<NonEmptyList<String>, String>invalid(errors))).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(List.of("a", "b"));
    }

    @Test
    void fromValidated_empty_is404() {
        ResponseEntity<?> response = WebfluxEntity.fromValidated(
            Mono.<Validated<NonEmptyList<String>, String>>empty()).block();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── null-guards ────────────────────────────────────────────────────────────

    @Test
    void nullArguments_areRejected() {
        assertThatNullPointerException().isThrownBy(() -> WebfluxEntity.fromOption(null));
        assertThatNullPointerException().isThrownBy(
            () -> WebfluxEntity.fromResult(Mono.empty(), null));
        assertThatNullPointerException().isThrownBy(
            () -> WebfluxEntity.fromTry(Mono.empty(), null));
        assertThatNullPointerException().isThrownBy(() -> WebfluxEntity.fromValidated(null));
    }
}
