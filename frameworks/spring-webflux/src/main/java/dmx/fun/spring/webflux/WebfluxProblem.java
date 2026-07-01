package dmx.fun.spring.webflux;

import dmx.fun.NonEmptyList;
import dmx.fun.Validated;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Renders dmx-fun failures as <a href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807</a>
 * {@link ProblemDetail} responses ({@code application/problem+json}), so error handling in a
 * {@code fun-spring-webflux} application produces a standard problem document instead of an
 * ad-hoc body.
 *
 * <p>The {@link #problemDetail} factories return an {@link ErrorHttpMapper} /
 * {@link ThrowableHttpMapper} you pass to {@link WebfluxFun#fromResult} / {@link WebfluxFun#fromTry};
 * {@link #fromValidated} renders accumulated validation failures as a {@code 400} problem whose
 * {@code errors} property lists every violation.
 */
@NullMarked
public final class WebfluxProblem {

    private WebfluxProblem() {
    }

    /**
     * Returns an {@link ErrorHttpMapper} that renders a {@code Result.Err} as a
     * {@link ProblemDetail} with the given {@code status} and a {@code detail} derived from the
     * error value. Use it with {@link WebfluxFun#fromResult(Mono, ErrorHttpMapper)}.
     *
     * @param status the HTTP status for the problem
     * @param detail derives the problem's human-readable detail from the error value
     * @param <E>    the domain error type
     * @return an {@link ErrorHttpMapper} producing a problem-detail response
     */
    public static <E> ErrorHttpMapper<E> problemDetail(HttpStatus status, Function<? super E, String> detail) {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(detail, "detail");
        return error -> problemResponse(ProblemDetail.forStatusAndDetail(status, detail.apply(error)));
    }

    /**
     * Returns a {@link ThrowableHttpMapper} that renders a {@code Try.Failure} as a
     * {@link ProblemDetail} with the given {@code status} and the exception message as the detail.
     * Use it with {@link WebfluxFun#fromTry(Mono, ThrowableHttpMapper)}.
     *
     * @param status the HTTP status for the problem
     * @return a {@link ThrowableHttpMapper} producing a problem-detail response
     */
    public static ThrowableHttpMapper problemDetail(HttpStatus status) {
        Objects.requireNonNull(status, "status");
        return cause -> problemResponse(ProblemDetail.forStatusAndDetail(status, String.valueOf(cause.getMessage())));
    }

    /**
     * Maps a {@code Mono<Validated<NonEmptyList<E>, V>>} to a response: {@code Valid} → {@code 200}
     * with the value, {@code Invalid} → {@code 400} {@link ProblemDetail} whose {@code errors}
     * property lists every accumulated error, empty → {@code 404}.
     *
     * @param source the upstream validated value
     * @param <V>    the value type
     * @param <E>    the element type of the accumulated errors
     * @return the HTTP response
     */
    public static <V, E> Mono<ServerResponse> fromValidated(Mono<Validated<NonEmptyList<E>, V>> source) {
        Objects.requireNonNull(source, "source");
        return source.<ServerResponse>flatMap(validated -> switch (validated) {
            case Validated.Valid<NonEmptyList<E>, V> valid -> ServerResponse.ok().bodyValue(valid.value());
            case Validated.Invalid<NonEmptyList<E>, V> invalid -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
                problem.setProperty("errors", invalid.error().toList());
                yield problemResponse(problem);
            }
        }).switchIfEmpty(ServerResponse.notFound().build());
    }

    private static Mono<ServerResponse> problemResponse(ProblemDetail problem) {
        return ServerResponse.status(problem.getStatus())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .bodyValue(problem);
    }
}
