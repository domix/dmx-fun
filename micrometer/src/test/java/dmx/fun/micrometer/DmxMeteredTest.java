package dmx.fun.micrometer;

import dmx.fun.Result;
import dmx.fun.Try;
import io.micrometer.core.instrument.Counter;
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

    @Test
    void recordTry_success_returnsSuccessValue() {
        Try<String> result = DmxMetered.of("op")
            .registry(registry)
            .recordTry(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void recordTry_failure_returnsFailure() {
        Try<String> result = DmxMetered.of("op")
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

        Counter counter = registry.get("op.count")
            .tags("service", "payments", "outcome", "success")
            .counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordResult_success_returnsOk() {
        Result<String, Throwable> result = DmxMetered.of("op")
            .registry(registry)
            .recordResult(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void recordResult_failure_returnsErr() {
        Result<String, Throwable> result = DmxMetered.of("op")
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

        Counter counter = registry.get("op.count").tag("outcome", "success").counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
