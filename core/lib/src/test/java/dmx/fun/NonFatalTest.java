package dmx.fun;

import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NonFatalTest {

    @Test
    void check_shouldClassifyOrdinaryExceptionsAsNonFatal() {
        assertThat(NonFatal.check(new RuntimeException())).isTrue();
        assertThat(NonFatal.check(new IOException())).isTrue();
        assertThat(NonFatal.check(new AssertionError())).isTrue();
    }

    @Test
    void check_shouldClassifyFatalThrowables() {
        assertThat(NonFatal.check(new OutOfMemoryError())).isFalse();
        assertThat(NonFatal.check(new StackOverflowError())).isFalse();
        assertThat(NonFatal.check(new NoClassDefFoundError())).isFalse();
        assertThat(NonFatal.check(new InterruptedException())).isFalse();
    }

    @Test
    void check_shouldNotInspectCauseChain() {
        assertThat(NonFatal.check(new RuntimeException(new OutOfMemoryError()))).isTrue();
    }

    @Test
    void check_shouldThrowNPEOnNull() {
        assertThatThrownBy(() -> NonFatal.check(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("throwable");
    }
}
