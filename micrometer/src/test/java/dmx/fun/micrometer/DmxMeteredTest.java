package dmx.fun.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmxMeteredTest {

    private final MeterRegistry registry = new SimpleMeterRegistry();

    // ── null contracts ─────────────────────────────────────────────────────────

    @Test
    void of_nullName_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxMetered.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    void tags_nullTags_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxMetered.of("op").tags(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("tags");
    }

    @Test
    void registry_nullRegistry_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxMetered.of("op").registry(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("registry");
    }

    // ── recordTry ──────────────────────────────────────────────────────────────

    @Test
    void recordTry_success_returnsSuccessValue() {
        var result = DmxMetered.of("op")
            .registry(registry)
            .recordTry(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void recordTry_failure_returnsFailure() {
        var result = DmxMetered.of("op")
            .registry(registry)
            .recordTry(() -> { throw new IOException("boom"); });

        assertThat(result).failsWith(IOException.class);
    }

    @Test
    void recordTry_recordsMetrics() {
        DmxMetered.of("op")
            .tags(Tags.of("service", "payments"))
            .registry(registry)
            .recordTry(() -> "ok");

        var counter = registry.get("op.count")
            .tags("service", "payments", "outcome", "success")
            .counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordResult_success_returnsOk() {
        var result = DmxMetered.of("op")
            .registry(registry)
            .recordResult(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void recordResult_failure_returnsErr() {
        var result = DmxMetered.of("op")
            .registry(registry)
            .recordResult(() -> { throw new IOException("boom"); });

        assertThat(result).isErr();
    }

    @Test
    void recordTry_withoutRegistry_throwsIllegalState() {
        assertThatThrownBy(() ->
            DmxMetered.of("op").recordTry(() -> "ok")
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordResult_withoutRegistry_throwsIllegalState() {
        assertThatThrownBy(() ->
            DmxMetered.of("op").recordResult(() -> "ok")
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void tags_defaultsToEmpty() {
        DmxMetered.of("op").registry(registry).recordTry(() -> "ok");

        var counter = registry.get("op.count").tag("outcome", "success").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
