package dmx.fun.observation;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmxObservedTest {

    private final TestObservationRegistry registry = TestObservationRegistry.create();

    // ── null contracts ─────────────────────────────────────────────────────────

    @Test
    void of_nullName_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxObserved.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    void registry_nullRegistry_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxObserved.of("op").registry(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("registry");
    }

    // ── observeTry ─────────────────────────────────────────────────────────────

    @Test
    void observeTry_success_returnsSuccessValue() {
        var result = DmxObserved.of("op")
            .registry(registry)
            .observeTry(() -> "ok");

        assertThat(result)
            .isSuccess()
            .containsValue("ok");
    }

    @Test
    void observeTry_failure_returnsFailure() {
        var result = DmxObserved.of("op")
            .registry(registry)
            .observeTry(() -> { throw new IOException("boom"); });

        assertThat(result)
            .isFailure();
    }

    @Test
    void observeTry_recordsObservation() {
        DmxObserved.of("payment.charge")
            .registry(registry)
            .observeTry(() -> "ok");

        TestObservationRegistryAssert
            .assertThat(registry)
            .hasObservationWithNameEqualTo("payment.charge")
            .that()
            .hasLowCardinalityKeyValue("outcome", "success");
    }

    // ── observeResult ──────────────────────────────────────────────────────────

    @Test
    void observeResult_success_returnsOk() {
        var result = DmxObserved.of("op")
            .registry(registry)
            .observeResult(() -> "ok");

        assertThat(result)
            .isOk()
            .containsValue("ok");
    }

    @Test
    void observeResult_failure_returnsErr() {
        var result = DmxObserved.of("op")
            .registry(registry)
            .observeResult(() -> { throw new IOException("boom"); });

        assertThat(result)
            .isErr();
    }

    // ── missing registry guard ─────────────────────────────────────────────────

    @Test
    void observeTry_withoutRegistry_throwsIllegalState() {
        assertThatThrownBy(() ->
            DmxObserved.of("op").observeTry(() -> "ok")
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void observeResult_withoutRegistry_throwsIllegalState() {
        assertThatThrownBy(() ->
            DmxObserved.of("op").observeResult(() -> "ok")
        ).isInstanceOf(IllegalStateException.class);
    }

    // ── custom exception classifier ────────────────────────────────────────────

    @Test
    void exceptionClassifier_customClassifier_usedForExceptionKey() {
        DmxObserved.of("op")
            .registry(registry)
            .exceptionClassifier(cause -> cause instanceof IOException ? "io" : "other")
            .observeTry(() -> { throw new IOException("boom"); });

        TestObservationRegistryAssert
            .assertThat(registry)
            .hasObservationWithNameEqualTo("op")
            .that()
            .hasLowCardinalityKeyValue("exception", "io");
    }

    @Test
    void exceptionClassifier_nullClassifier_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxObserved.of("op").exceptionClassifier(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("exceptionClassifier");
    }
}
