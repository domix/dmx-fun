package dmx.fun.resilience4j;

import dmx.fun.Result;
import dmx.fun.Try;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmxCircuitBreakerTest {

    private static DmxCircuitBreaker openCircuit() {
        CircuitBreaker cb = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
        cb.transitionToOpenState();
        return DmxCircuitBreaker.of(cb);
    }

    private static DmxCircuitBreaker closedCircuit() {
        return DmxCircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
    }

    @Test
    void executeTry_success() {
        Try<String> result = closedCircuit().executeTry(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeTry_callFailure_returnsFailure() {
        IOException boom = new IOException("boom");
        Try<String> result = closedCircuit().executeTry(() -> { throw boom; });

        assertThat(result).failsWith(IOException.class);
        assertThat(result.getCause()).isSameAs(boom);
    }

    @Test
    void executeTry_circuitOpen_returnsCallNotPermittedException() {
        Try<String> result = openCircuit().executeTry(() -> "ok");

        assertThat(result).failsWith(CallNotPermittedException.class);
    }

    @Test
    void executeResult_success() {
        Result<String, Throwable> result = closedCircuit().executeResult(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_success() {
        Result<String, CallNotPermittedException> result = closedCircuit().executeResultTyped(() -> "ok");

        assertThat(result).containsValue("ok");
    }

    @Test
    void executeResultTyped_circuitOpen_returnsErr() {
        Result<String, CallNotPermittedException> result = openCircuit().executeResultTyped(() -> "ok");

        assertThat(result).isErr();
        assertThat(result.getError()).isInstanceOf(CallNotPermittedException.class);
    }

    @Test
    void executeResultTyped_callFailure_propagatesAsUnchecked() {
        assertThatThrownBy(() -> closedCircuit().executeResultTyped(() -> { throw new IOException("boom"); }))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void of_wrapsExistingCircuitBreaker() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("existing");
        DmxCircuitBreaker dmxCb = DmxCircuitBreaker.of(cb);

        assertThat(dmxCb.executeTry(() -> 42)).containsValue(42);
    }
}
