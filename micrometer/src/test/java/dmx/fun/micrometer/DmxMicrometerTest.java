package dmx.fun.micrometer;

import dmx.fun.Result;
import dmx.fun.Try;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class DmxMicrometerTest {

    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final DmxMicrometer dmx = DmxMicrometer.of(registry);

    // ── recordTry — success ────────────────────────────────────────────────────

    @Test
    void recordTry_success_returnsSuccessValue() {
        Try<String> result = dmx.recordTry("op", Tags.empty(), () -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void recordTry_success_incrementsSuccessCounter() {
        dmx.recordTry("op", Tags.empty(), () -> "ok");

        Counter counter = registry.get("op.count").tag("outcome", "success").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordTry_success_recordsDurationTimer() {
        dmx.recordTry("op", Tags.empty(), () -> "ok");

        Timer timer = registry.get("op.duration").tag("outcome", "success").timer();
        assertThat(timer.count()).isEqualTo(1L);
    }

    @Test
    void recordTry_success_noFailureCounter() {
        dmx.recordTry("op", Tags.empty(), () -> "ok");

        assertThat(registry.find("op.failure").counters()).isEmpty();
    }

    // ── recordTry — failure ────────────────────────────────────────────────────

    @Test
    void recordTry_failure_returnsFailure() {
        IOException boom = new IOException("boom");
        Try<String> result = dmx.recordTry("op", Tags.empty(), () -> { throw boom; });

        assertThat(result).failsWith(IOException.class);
        assertThat(result.getCause()).isSameAs(boom);
    }

    @Test
    void recordTry_failure_incrementsFailureCounter() {
        dmx.recordTry("op", Tags.empty(), () -> { throw new IOException("boom"); });

        Counter counter = registry.get("op.count").tag("outcome", "failure").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordTry_failure_recordsDurationTimer() {
        dmx.recordTry("op", Tags.empty(), () -> { throw new RuntimeException("boom"); });

        Timer timer = registry.get("op.duration").tag("outcome", "failure").timer();
        assertThat(timer.count()).isEqualTo(1L);
    }

    @Test
    void recordTry_failure_incrementsExceptionCounter() {
        dmx.recordTry("op", Tags.empty(), () -> { throw new IOException("boom"); });

        Counter counter = registry.get("op.failure").tag("exception", "IOException").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordTry_failure_exceptionCounterTaggedWithSimpleClassName() {
        dmx.recordTry("op", Tags.empty(), () -> { throw new IllegalArgumentException("bad"); });

        Counter counter = registry.get("op.failure").tag("exception", "IllegalArgumentException").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    // ── custom tags ────────────────────────────────────────────────────────────

    @Test
    void recordTry_customTags_propagatedToAllMetrics() {
        Tags tags = Tags.of("service", "payments");
        dmx.recordTry("op", tags, () -> "ok");

        Counter counter = registry.get("op.count")
            .tags("service", "payments", "outcome", "success")
            .counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordTry_customTags_propagatedToFailureCounter() {
        Tags tags = Tags.of("service", "payments");
        dmx.recordTry("op", tags, () -> { throw new IOException("boom"); });

        Counter counter = registry.get("op.failure")
            .tags("service", "payments", "exception", "IOException")
            .counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    // ── multiple calls ─────────────────────────────────────────────────────────

    @Test
    void recordTry_multipleSuccesses_counterAccumulates() {
        dmx.recordTry("op", Tags.empty(), () -> "a");
        dmx.recordTry("op", Tags.empty(), () -> "b");
        dmx.recordTry("op", Tags.empty(), () -> "c");

        Counter counter = registry.get("op.count").tag("outcome", "success").counter();
        assertThat(counter.count()).isEqualTo(3.0);
    }

    // ── recordResult ───────────────────────────────────────────────────────────

    @Test
    void recordResult_success_returnsOk() {
        Result<String, Throwable> result = dmx.recordResult("op", Tags.empty(), () -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void recordResult_failure_returnsErr() {
        Result<String, Throwable> result = dmx.recordResult("op", Tags.empty(),
            () -> { throw new IOException("boom"); });

        assertThat(result).isErr();
        assertThat(result.getError()).isInstanceOf(IOException.class);
    }

    @Test
    void recordResult_recordsSameMetricsAsTry() {
        dmx.recordResult("op", Tags.empty(), () -> "ok");

        Counter counter = registry.get("op.count").tag("outcome", "success").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
