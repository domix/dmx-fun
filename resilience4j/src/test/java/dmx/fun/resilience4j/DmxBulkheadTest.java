package dmx.fun.resilience4j;

import dmx.fun.Result;
import dmx.fun.Try;
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

class DmxBulkheadTest {

    private static DmxBulkhead withMaxConcurrent(int max) {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(max)
            .maxWaitDuration(Duration.ZERO)
            .build();
        return DmxBulkhead.of("test", config);
    }

    @Test
    void executeTry_success() {
        Try<String> result = withMaxConcurrent(10).executeTry(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeTry_callFailure_returnsFailure() {
        IOException boom = new IOException("boom");
        Try<String> result = withMaxConcurrent(10).executeTry(() -> {
            throw boom;
        });

        assertThat(result).failsWith(IOException.class);
        assertThat(result.getCause()).isSameAs(boom);
    }

    @Test
    void executeTry_bulkheadFull_returnsBulkheadFullException() throws Exception {
        DmxBulkhead bh = withMaxConcurrent(1);
        CountDownLatch hold = new CountDownLatch(1);
        CountDownLatch inside = new CountDownLatch(1);

        Thread occupier = Thread.ofVirtual().start(() ->
            bh.executeTry(() -> {
                inside.countDown();
                hold.await();
                return "held";
            })
        );
        inside.await();

        Try<String> result = bh.executeTry(() -> "second");

        hold.countDown();
        occupier.join();

        assertThat(result).failsWith(BulkheadFullException.class);
    }

    @Test
    void executeResult_success() {
        Result<String, Throwable> result = withMaxConcurrent(10).executeResult(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_success() {
        Result<String, BulkheadFullException> result = withMaxConcurrent(10).executeResultTyped(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_bulkheadFull_returnsErr() throws Exception {
        DmxBulkhead bh = withMaxConcurrent(1);
        CountDownLatch hold = new CountDownLatch(1);
        CountDownLatch inside = new CountDownLatch(1);

        Thread occupier = Thread.ofVirtual().start(() ->
            bh.executeResultTyped(() -> {
                inside.countDown();
                hold.await();
                return "held";
            })
        );
        inside.await();

        Result<String, BulkheadFullException> result = bh.executeResultTyped(() -> "second");

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
        Bulkhead bh = Bulkhead.ofDefaults("existing");
        DmxBulkhead dmxBh = DmxBulkhead.of(bh);

        assertThat(dmxBh.executeTry(() -> 42)).containsValue(42);
    }
}
