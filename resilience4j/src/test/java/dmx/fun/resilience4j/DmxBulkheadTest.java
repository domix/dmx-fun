package dmx.fun.resilience4j;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DmxBulkheadTest {

    private static DmxBulkhead withMaxConcurrent(int max) {
        var config = BulkheadConfig.custom()
            .maxConcurrentCalls(max)
            .maxWaitDuration(Duration.ZERO)
            .build();
        return DmxBulkhead.of("test", config);
    }

    @Test
    void executeTry_success() {
        var result = withMaxConcurrent(10).executeTry(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeTry_callFailure_returnsFailure() {
        var boom = new IOException("boom");
        var result = withMaxConcurrent(10).executeTry(() -> {
            throw boom;
        });

        assertThat(result).failsWith(IOException.class);
        assertThat(result.getCause()).isSameAs(boom);
    }

    @Test
    void executeTry_bulkheadFull_returnsBulkheadFullException() throws Exception {
        var bh = withMaxConcurrent(1);
        var hold = new CountDownLatch(1);
        var inside = new CountDownLatch(1);

        var occupier = Thread.ofVirtual().start(() ->
            bh.executeTry(() -> {
                inside.countDown();
                hold.await();
                return "held";
            })
        );
        inside.await();

        var result = bh.executeTry(() -> "second");

        hold.countDown();
        occupier.join();

        assertThat(result).failsWith(BulkheadFullException.class);
    }

    @Test
    void executeResult_success() {
        var result = withMaxConcurrent(10).executeResult(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_success() {
        var result = withMaxConcurrent(10).executeResultTyped(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_bulkheadFull_returnsErr() throws Exception {
        var bh = withMaxConcurrent(1);
        var hold = new CountDownLatch(1);
        var inside = new CountDownLatch(1);

        var occupier = Thread.ofVirtual().start(() ->
            bh.executeResultTyped(() -> {
                inside.countDown();
                hold.await();
                return "held";
            })
        );
        inside.await();

        var result = bh.executeResultTyped(() -> "second");

        hold.countDown();
        occupier.join();

        assertThat(result).isErr();
        assertThat(result.getError()).isInstanceOf(BulkheadFullException.class);
    }

    @Test
    void executeResultTyped_callFailure_propagatesAsUnchecked() {
        assertThatThrownBy(() -> withMaxConcurrent(10).executeResultTyped(() -> {
            throw new IOException("boom");
        }))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void of_wrapsExistingBulkhead() {
        var bh = Bulkhead.ofDefaults("existing");
        var dmxBh = DmxBulkhead.of(bh);

        assertThat(dmxBh.executeTry(() -> 42)).containsValue(42);
    }

    // ── null contracts ────────────────────────────────────────────────────────────

    @Test
    void of_nullBulkhead_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DmxBulkhead.of(null));
    }
}
