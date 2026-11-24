package dmx.fun.resilience4j;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
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

    // ── fatal throwables ───────────────────────────────────────────────────────

    @Test
    void executeTry_interruptedException_isRethrownWithInterruptFlagSet() {
        var interruption = new InterruptedException("stop");
        try {
            assertThatThrownBy(() -> withMaxConcurrent(10).executeTry(() -> {
                throw interruption;
            }))
                .isInstanceOf(CompletionException.class)
                .hasCause(interruption);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted(); // do not leak the flag into other tests
        }
    }

    @Test
    void executeTry_fatalError_isRethrown() {
        var overflow = new StackOverflowError("deep");

        assertThatThrownBy(() -> withMaxConcurrent(10).executeTry(() -> {
            throw overflow;
        })).isSameAs(overflow);
    }

    @Test
    void executeResult_fatalError_isRethrown() {
        var overflow = new StackOverflowError("deep");

        assertThatThrownBy(() -> withMaxConcurrent(10).executeResult(() -> {
            throw overflow;
        })).isSameAs(overflow);
    }

    @Test
    void executeTry_nonFatalError_staysFailure() {
        var failed = new AssertionError("not fatal");
        var result = withMaxConcurrent(10).executeTry(() -> {
            throw failed;
        });

        assertThat(result).failsWith(AssertionError.class);
        assertThat(result.getCause()).isSameAs(failed);
    }

    @Test
    void executeResultTyped_interruptedException_wrapsAndRestoresInterruptFlag() {
        var interruption = new InterruptedException("stop");
        try {
            assertThatThrownBy(() -> withMaxConcurrent(10).executeResultTyped(() -> {
                throw interruption;
            }))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCause(interruption);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void executeResultTyped_interruptionAsCause_restoresInterruptFlag() {
        var wrapped = new ExecutionException(new InterruptedException("stop"));
        try {
            assertThatThrownBy(() -> withMaxConcurrent(10).executeResultTyped(() -> {
                throw wrapped;
            }))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCause(wrapped);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void executeResultTyped_checkedExceptionWithoutInterruption_leavesInterruptFlagClear() {
        assertThatThrownBy(() -> withMaxConcurrent(10).executeResultTyped(() -> {
            throw new IOException("boom");
        })).isInstanceOf(RuntimeException.class);

        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }
}
