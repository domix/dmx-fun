package codes.domix.fun;

import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckedInterfacesTest {

    // ---- CheckedSupplier ----

    @Test
    void checkedSupplier_successPath_viaTryOf() {
        CheckedSupplier<String> supplier = () -> "hello";
        Try<String> result = Try.of(supplier);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
    }

    @Test
    void checkedSupplier_failurePath_viaTryOf() {
        CheckedSupplier<String> supplier = () -> { throw new IOException("boom"); };
        Try<String> result = Try.of(supplier);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(IOException.class).hasMessage("boom");
    }

    @Test
    void checkedSupplier_lambdaSyntax_noExplicitCast() {
        Try<Integer> result = Try.of(() -> 42);
        assertThat(result.get()).isEqualTo(42);
    }

    // ---- CheckedRunnable ----

    @Test
    void checkedRunnable_successPath_viaTryRun() {
        CheckedRunnable runnable = () -> { /* no-op */ };
        Try<Void> result = Try.run(runnable);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void checkedRunnable_failurePath_viaTryRun() {
        CheckedRunnable runnable = () -> { throw new IllegalStateException("bad state"); };
        Try<Void> result = Try.run(runnable);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(IllegalStateException.class).hasMessage("bad state");
    }

    @Test
    void checkedRunnable_lambdaSyntax_noExplicitCast() {
        Try<Void> result = Try.run(() -> { /* side-effect */ });
        assertThat(result.isSuccess()).isTrue();
    }

    // ---- CheckedFunction ----

    @Test
    void checkedFunction_directApply_succeeds() throws Exception {
        CheckedFunction<String, Integer> fn = String::length;
        assertThat(fn.apply("hello")).isEqualTo(5);
    }

    @Test
    void checkedFunction_exceptionCapturedByTryOf() {
        CheckedFunction<String, Integer> fn = s -> { throw new RuntimeException("fn failed"); };
        Try<Integer> result = Try.of(() -> fn.apply("anything"));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).hasMessage("fn failed");
    }

    @Test
    void checkedFunction_lambdaSyntax_noExplicitCast() throws Exception {
        CheckedFunction<Integer, String> fn = i -> "val=" + i;
        assertThat(fn.apply(7)).isEqualTo("val=7");
    }

    // ---- CheckedConsumer ----

    @Test
    void checkedConsumer_directAccept_succeeds() throws Exception {
        StringBuilder sb = new StringBuilder();
        CheckedConsumer<String> consumer = sb::append;
        consumer.accept("world");
        assertThat(sb.toString()).isEqualTo("world");
    }

    @Test
    void checkedConsumer_exceptionPropagatesCorrectly() {
        CheckedConsumer<String> consumer = s -> { throw new IOException("consumer failed"); };
        assertThatThrownBy(() -> consumer.accept("x"))
            .isInstanceOf(IOException.class)
            .hasMessage("consumer failed");
    }

    @Test
    void checkedConsumer_lambdaSyntax_noExplicitCast() throws Exception {
        CheckedConsumer<Integer> consumer = i -> { /* use i */ };
        consumer.accept(99); // must compile without cast
    }
}
