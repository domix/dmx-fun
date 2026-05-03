package dmx.fun.tracing;

import io.micrometer.tracing.test.simple.SimpleTracer;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmxTracedTest {

    private final SimpleTracer tracer = new SimpleTracer();

    // ── null contracts ─────────────────────────────────────────────────────────

    @Test
    void of_nullName_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxTraced.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    void tracer_nullTracer_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxTraced.of("op").tracer(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("tracer");
    }

    // ── traceTry ───────────────────────────────────────────────────────────────

    @Test
    void traceTry_success_returnsSuccessValue() {
        var result = DmxTraced.of("op")
            .tracer(tracer)
            .traceTry(() -> "ok");

        assertThat(result)
            .isSuccess()
            .containsValue("ok");
    }

    @Test
    void traceTry_failure_returnsFailure() {
        var result = DmxTraced.of("op")
            .tracer(tracer)
            .traceTry(() -> { throw new IOException("boom"); });

        assertThat(result)
            .isFailure();
    }

    @Test
    void traceTry_recordsSpan() {
        DmxTraced.of("payment.charge")
            .tracer(tracer)
            .traceTry(() -> "ok");

        assertThat(tracer.getSpans().peekFirst().getName())
            .isEqualTo("payment.charge");
        assertThat(tracer.getSpans().peekFirst().getTags())
            .containsEntry("outcome", "success");
    }

    // ── traceResult ────────────────────────────────────────────────────────────

    @Test
    void traceResult_success_returnsOk() {
        var result = DmxTraced.of("op")
            .tracer(tracer)
            .traceResult(() -> "ok");

        assertThat(result)
            .isOk()
            .containsValue("ok");
    }

    @Test
    void traceResult_failure_returnsErr() {
        var result = DmxTraced.of("op")
            .tracer(tracer)
            .traceResult(() -> { throw new IOException("boom"); });

        assertThat(result)
            .isErr();
    }

    // ── missing tracer guard ───────────────────────────────────────────────────

    @Test
    void traceTry_withoutTracer_throwsIllegalState() {
        assertThatThrownBy(() ->
            DmxTraced.of("op").traceTry(() -> "ok")
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void traceResult_withoutTracer_throwsIllegalState() {
        assertThatThrownBy(() ->
            DmxTraced.of("op").traceResult(() -> "ok")
        ).isInstanceOf(IllegalStateException.class);
    }

    // ── custom exception classifier ────────────────────────────────────────────

    @Test
    void exceptionClassifier_customClassifier_usedForExceptionTag() {
        DmxTraced.of("op")
            .tracer(tracer)
            .exceptionClassifier(cause -> cause instanceof IOException ? "io" : "other")
            .traceTry(() -> { throw new IOException("boom"); });

        assertThat(tracer.getSpans().peekFirst().getTags())
            .containsEntry("exception", "io");
    }

    @Test
    void exceptionClassifier_nullClassifier_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxTraced.of("op").exceptionClassifier(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("exceptionClassifier");
    }
}
