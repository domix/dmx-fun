package dmx.fun.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmxMicrometerTest {

    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final DmxMicrometer dmx = DmxMicrometer.of(registry);

    @Test
    void of_nullRegistry_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxMicrometer.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("registry");
    }

    @Test
    void recordTry_nullName_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.recordTry(null, Tags.empty(), () -> "ok"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    void recordTry_nullTags_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.recordTry("op", null, () -> "ok"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("tags");
    }

    @Test
    void recordTry_nullSupplier_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.recordTry("op", Tags.empty(), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("supplier");
    }

    @Test
    void recordResult_nullName_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.recordResult(null, Tags.empty(), () -> "ok"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    void recordResult_nullTags_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.recordResult("op", null, () -> "ok"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("tags");
    }

    @Test
    void recordResult_nullSupplier_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.recordResult("op", Tags.empty(), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("supplier");
    }

    // ── recordTry — success ────────────────────────────────────────────────────

    @Test
    void recordTry_success_returnsSuccessValue() {
        var result = dmx.recordTry("op", Tags.empty(), () -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void recordTry_success_incrementsSuccessCounter() {
        dmx.recordTry("op", Tags.empty(), () -> "ok");

        var counter = registry.get("op.count").tag("outcome", "success").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordTry_success_recordsDurationTimer() {
        dmx.recordTry("op", Tags.empty(), () -> "ok");

        var timer = registry.get("op.duration").tag("outcome", "success").timer();
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
        var boom = new IOException("boom");
        var result = dmx.recordTry("op", Tags.empty(), () -> { throw boom; });

        assertThat(result).failsWith(IOException.class);
        assertThat(result.getCause()).isSameAs(boom);
    }

    @Test
    void recordTry_failure_incrementsFailureCounter() {
        dmx.recordTry("op", Tags.empty(), () -> { throw new IOException("boom"); });

        var counter = registry.get("op.count").tag("outcome", "failure").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordTry_failure_recordsDurationTimer() {
        dmx.recordTry("op", Tags.empty(), () -> { throw new RuntimeException("boom"); });

        var timer = registry.get("op.duration").tag("outcome", "failure").timer();
        assertThat(timer.count()).isEqualTo(1L);
    }

    @Test
    void recordTry_failure_incrementsExceptionCounter() {
        dmx.recordTry("op", Tags.empty(), () -> { throw new IOException("boom"); });

        var counter = registry.get("op.failure").tag("exception", "IOException").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordTry_failure_exceptionCounterTaggedWithSimpleClassName() {
        dmx.recordTry("op", Tags.empty(), () -> { throw new IllegalArgumentException("bad"); });

        var counter = registry.get("op.failure").tag("exception", "IllegalArgumentException").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    // ── custom tags ────────────────────────────────────────────────────────────

    @Test
    void recordTry_customTags_propagatedToAllMetrics() {
        var tags = Tags.of("service", "payments");
        dmx.recordTry("op", tags, () -> "ok");

        var counter = registry.get("op.count")
            .tags("service", "payments", "outcome", "success")
            .counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordTry_customTags_propagatedToFailureCounter() {
        var tags = Tags.of("service", "payments");
        dmx.recordTry("op", tags, () -> { throw new IOException("boom"); });

        var counter = registry.get("op.failure")
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

        var counter = registry.get("op.count").tag("outcome", "success").counter();
        assertThat(counter.count()).isEqualTo(3.0);
    }

    // ── recordResult ───────────────────────────────────────────────────────────

    @Test
    void recordResult_success_returnsOk() {
        var result = dmx.recordResult("op", Tags.empty(), () -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void recordResult_failure_returnsErr() {
        var result = dmx.recordResult("op", Tags.empty(),
            () -> { throw new IOException("boom"); });

        assertThat(result).isErr();
        assertThat(result.getError()).isInstanceOf(IOException.class);
    }

    @Test
    void recordResult_recordsSameMetricsAsTry() {
        dmx.recordResult("op", Tags.empty(), () -> "ok");

        var counter = registry.get("op.count").tag("outcome", "success").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordTry_concurrentCalls_accumulateSuccessFailureAndDurationMetrics() {
        int totalCalls = 500;

        IntStream.range(0, totalCalls).parallel().forEach(i ->
            dmx.recordTry("op.concurrent", Tags.of("component", "test"), () -> {
                if (i % 3 == 0) throw new IOException("boom");
                return "ok";
            })
        );

        long expectedFailures = IntStream.range(0, totalCalls).filter(i -> i % 3 == 0).count();
        long expectedSuccesses = totalCalls - expectedFailures;

        var success = registry.get("op.concurrent.count")
            .tags("component", "test", "outcome", "success")
            .counter();
        var failure = registry.get("op.concurrent.count")
            .tags("component", "test", "outcome", "failure")
            .counter();
        var exception = registry.get("op.concurrent.failure")
            .tags("component", "test", "exception", "IOException")
            .counter();
        var successTimer = registry.get("op.concurrent.duration")
            .tags("component", "test", "outcome", "success")
            .timer();
        var failureTimer = registry.get("op.concurrent.duration")
            .tags("component", "test", "outcome", "failure")
            .timer();

        assertThat(success.count()).isEqualTo((double) expectedSuccesses);
        assertThat(failure.count()).isEqualTo((double) expectedFailures);
        assertThat(exception.count()).isEqualTo((double) expectedFailures);
        assertThat(successTimer.count()).isEqualTo(expectedSuccesses);
        assertThat(failureTimer.count()).isEqualTo(expectedFailures);
    }
}
