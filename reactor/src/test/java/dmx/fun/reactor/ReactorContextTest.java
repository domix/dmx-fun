package dmx.fun.reactor;

import org.junit.jupiter.api.Test;
import reactor.util.context.Context;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReactorContextTest {

    // ContextView was introduced in Reactor 3.4; the compatibility matrix also runs
    // against 3.3.x, where these helpers cannot exist. Skip rather than fail there.
    private static final boolean CONTEXT_VIEW_AVAILABLE = contextViewAvailable();

    private static boolean contextViewAvailable() {
        try {
            Class.forName("reactor.util.context.ContextView");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Test
    void getOrNone_presentKey_isSome() {
        assumeTrue(CONTEXT_VIEW_AVAILABLE, "ContextView requires Reactor 3.4+");
        Context ctx = Context.of("user", "alice");
        assertThat(ReactorContext.<String>getOrNone(ctx, "user")).isSome().containsValue("alice");
    }

    @Test
    void getOrNone_absentKey_isNone() {
        assumeTrue(CONTEXT_VIEW_AVAILABLE, "ContextView requires Reactor 3.4+");
        Context ctx = Context.of("user", "alice");
        assertThat(ReactorContext.<String>getOrNone(ctx, "missing")).isNone();
    }

    @Test
    void getOrNone_nullContext_throws() {
        assumeTrue(CONTEXT_VIEW_AVAILABLE, "ContextView requires Reactor 3.4+");
        assertThatThrownBy(() -> ReactorContext.getOrNone(null, "user"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("context");
    }
}
