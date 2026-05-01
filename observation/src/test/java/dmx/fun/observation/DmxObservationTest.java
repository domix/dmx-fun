package dmx.fun.observation;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmxObservationTest {

    private final TestObservationRegistry registry = TestObservationRegistry.create();
    private final DmxObservation dmx = DmxObservation.of(registry);

    // ── null contracts ─────────────────────────────────────────────────────────

    @Test
    void of_nullRegistry_throwsNullPointerException() {
        assertThatThrownBy(() -> DmxObservation.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("registry");
    }

    @Test
    void observeTry_nullName_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.observeTry(null, () -> "ok"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    void observeTry_nullSupplier_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.observeTry("op", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("supplier");
    }

    @Test
    void observeResult_nullName_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.observeResult(null, () -> "ok"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    void observeResult_nullSupplier_throwsNullPointerException() {
        assertThatThrownBy(() -> dmx.observeResult("op", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("supplier");
    }

    // ── observeTry — success ───────────────────────────────────────────────────

    @Test
    void observeTry_success_returnsSuccessValue() {
        var result = dmx.observeTry("op", () -> "ok");

        assertThat(result)
            .isSuccess()
            .containsValue("ok");
    }

    @Test
    void observeTry_success_createsObservationWithName() {
        dmx.observeTry("payment.charge", () -> "ok");

        TestObservationRegistryAssert
            .assertThat(registry)
            .hasObservationWithNameEqualTo("payment.charge");
    }

    @Test
    void observeTry_success_tagsOutcomeSuccess() {
        dmx.observeTry("op", () -> "ok");

        TestObservationRegistryAssert
            .assertThat(registry)
            .hasObservationWithNameEqualTo("op")
            .that()
            .hasLowCardinalityKeyValue("outcome", "success");
    }

    @Test
    void observeTry_success_noExceptionKey() {
        dmx.observeTry("op", () -> "ok");

        TestObservationRegistryAssert
            .assertThat(registry)
            .hasObservationWithNameEqualTo("op")
            .that()
            .doesNotHaveLowCardinalityKeyValueWithKey("exception");
    }

    @Test
    void observeTry_success_stopsObservation() {
        dmx.observeTry("op", () -> "ok");

        TestObservationRegistryAssert
            .assertThat(registry)
            .hasObservationWithNameEqualTo("op")
            .that()
            .hasBeenStopped();
    }

    // ── observeTry — failure ───────────────────────────────────────────────────

    @Test
    void observeTry_failure_returnsFailure() {
        var boom = new IOException("boom");
        var result = dmx.observeTry("op", () -> { throw boom; });

        assertThat(result)
            .isFailure();
        assertThat(result.getCause())
            .isSameAs(boom);
    }

    @Test
    void observeTry_failure_tagsOutcomeFailure() {
        dmx.observeTry("op", () -> { throw new IOException("boom"); });

        TestObservationRegistryAssert
            .assertThat(registry)
            .hasObservationWithNameEqualTo("op")
            .that()
            .hasLowCardinalityKeyValue("outcome", "failure");
    }

    static Stream<Exception> exceptionTypes() {
        return Stream.of(
            new IOException("boom"),
            new IllegalArgumentException("bad")
        );
    }

    @ParameterizedTest(name = "exception tag uses simple class name: {0}")
    @MethodSource("exceptionTypes")
    void observeTry_failure_exceptionTagUsesSimpleClassName(Exception cause) {
        dmx.observeTry("op", () -> { throw cause; });

        TestObservationRegistryAssert
            .assertThat(registry)
            .hasObservationWithNameEqualTo("op")
            .that()
            .hasLowCardinalityKeyValue("exception", cause.getClass().getSimpleName());
    }

    @Test
    void observeTry_failure_marksObservationAsError() {
        dmx.observeTry("op", () -> { throw new IOException("boom"); });

        TestObservationRegistryAssert
            .assertThat(registry)
            .hasObservationWithNameEqualTo("op")
            .that()
            .hasError();
    }

    @Test
    void observeTry_failure_stopsObservation() {
        dmx.observeTry("op", () -> { throw new IOException("boom"); });

        TestObservationRegistryAssert.assertThat(registry)
            .hasObservationWithNameEqualTo("op")
            .that()
            .hasBeenStopped();
    }

    // ── observeResult ──────────────────────────────────────────────────────────

    @Test
    void observeResult_success_returnsOk() {
        var result = dmx.observeResult("op", () -> "ok");

        assertThat(result)
            .isOk()
            .containsValue("ok");
    }

    @Test
    void observeResult_failure_returnsErr() {
        var result = dmx.observeResult("op", () -> { throw new IOException("boom"); });

        assertThat(result)
            .isErr();
        assertThat(result.getError())
            .isInstanceOf(IOException.class);
    }

    @Test
    void observeResult_recordsSameSignalsAsObserveTry() {
        dmx.observeResult("op", () -> "ok");

        TestObservationRegistryAssert
            .assertThat(registry)
            .hasObservationWithNameEqualTo("op")
            .that()
            .hasLowCardinalityKeyValue("outcome", "success")
            .hasBeenStopped();
    }

    // ── multiple calls ─────────────────────────────────────────────────────────

    @Test
    void observeTry_multipleCalls_eachCreatesOwnObservation() {
        dmx.observeTry("op.a", () -> "a");
        dmx.observeTry("op.b", () -> { throw new IOException(); });

        TestObservationRegistryAssert
            .assertThat(registry)
            .hasNumberOfObservationsEqualTo(2);
    }
}
