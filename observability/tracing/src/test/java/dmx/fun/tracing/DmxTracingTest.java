package dmx.fun.tracing;

import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmxTracingTest {

    private final SimpleTracer tracer = new SimpleTracer();
    private final DmxTracing dmx = DmxTracing.of(tracer);

    // ── null contracts ─────────────────────────────────────────────────────────

    @Test
    void of_nullTracer_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxTracing.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("tracer");
    }

    @Test
    void traceTry_nullName_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.traceTry(null, () -> "ok"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    void traceTry_nullSupplier_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.traceTry("op", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("supplier");
    }

    @Test
    void traceResult_nullName_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.traceResult(null, () -> "ok"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    void traceResult_nullSupplier_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.traceResult("op", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("supplier");
    }

    // ── traceTry — success ─────────────────────────────────────────────────────

    @Test
    void traceTry_success_returnsSuccessValue() {
        var result = dmx.traceTry("op", () -> "ok");

        assertThat(result)
            .isSuccess()
            .containsValue("ok");
    }

    @Test
    void traceTry_success_createsSpanWithName() {
        dmx.traceTry("payment.charge", () -> "ok");

        assertThat(finishedSpan().getName())
            .isEqualTo("payment.charge");
    }

    @Test
    void traceTry_success_tagsOutcomeSuccess() {
        dmx.traceTry("op", () -> "ok");

        assertThat(finishedSpan().getTags())
            .containsEntry("outcome", "success");
    }

    @Test
    void traceTry_success_noExceptionTag() {
        dmx.traceTry("op", () -> "ok");

        assertThat(finishedSpan().getTags())
            .doesNotContainKey("exception");
    }

    @Test
    void traceTry_success_noError() {
        dmx.traceTry("op", () -> "ok");

        assertThat(finishedSpan().getError())
            .isNull();
    }

    @Test
    void traceTry_success_endsSpan() {
        dmx.traceTry("op", () -> "ok");

        assertThat(tracer.getSpans())
            .hasSize(1);
    }

    // ── traceTry — failure ─────────────────────────────────────────────────────

    @Test
    void traceTry_failure_returnsFailure() {
        var boom = new IOException("boom");
        var result = dmx.traceTry("op", () -> { throw boom; });

        assertThat(result)
            .isFailure();
        assertThat(result.getCause())
            .isSameAs(boom);
    }

    @Test
    void traceTry_failure_tagsOutcomeFailure() {
        dmx.traceTry("op", () -> { throw new IOException("boom"); });

        assertThat(finishedSpan().getTags())
            .containsEntry("outcome", "failure");
    }

    @Test
    void traceTry_failure_tagsExceptionSimpleClassName() {
        dmx.traceTry("op", () -> { throw new IOException("boom"); });

        assertThat(finishedSpan().getTags())
            .containsEntry("exception", "IOException");
    }

    @Test
    void traceTry_failure_marksSpanAsError() {
        var boom = new IOException("boom");
        dmx.traceTry("op", () -> { throw boom; });

        assertThat(finishedSpan().getError())
            .isSameAs(boom);
    }

    @Test
    void traceTry_failure_endsSpan() {
        dmx.traceTry("op", () -> { throw new IOException("boom"); });

        assertThat(tracer.getSpans())
            .hasSize(1);
    }

    @Test
    void traceTry_failure_exceptionTagUsesSimpleClassName() {
        dmx.traceTry("op", () -> { throw new IllegalArgumentException("bad"); });

        assertThat(finishedSpan().getTags())
            .containsEntry("exception", "IllegalArgumentException");
    }

    @Test
    void of_withCustomClassifier_usesClassifierForExceptionTag() {
        var customDmx = DmxTracing.of(tracer, cause ->
            cause instanceof IOException ? "io" : "other"
        );
        customDmx.traceTry("op", () -> { throw new IOException("boom"); });

        assertThat(tracer.getSpans().peekFirst().getTags())
            .containsEntry("exception", "io");
    }

    @Test
    void of_withCustomClassifier_nullClassifier_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxTracing.of(tracer, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("exceptionClassifier");
    }

    // ── traceResult ────────────────────────────────────────────────────────────

    @Test
    void traceResult_success_returnsOk() {
        var result = dmx.traceResult("op", () -> "ok");

        assertThat(result)
            .isOk()
            .containsValue("ok");
    }

    @Test
    void traceResult_failure_returnsErr() {
        var result = dmx.traceResult("op", () -> { throw new IOException("boom"); });

        assertThat(result)
            .isErr();
        assertThat(result.getError())
            .isInstanceOf(IOException.class);
    }

    @Test
    void traceResult_recordsSameSpanAsTraceTry() {
        dmx.traceResult("op", () -> "ok");

        assertThat(finishedSpan().getTags())
            .containsEntry("outcome", "success");
    }

    // ── multiple calls ─────────────────────────────────────────────────────────

    @Test
    void traceTry_multipleCalls_eachCreatesOwnSpan() {
        dmx.traceTry("op.a", () -> "a");
        dmx.traceTry("op.b", () -> { throw new IOException(); });

        assertThat(tracer.getSpans())
            .hasSize(2);
    }

    // ── helper ─────────────────────────────────────────────────────────────────

    private SimpleSpan finishedSpan() {
        return tracer.getSpans().peekFirst();
    }
}
