package dmx.fun;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Tracks open/close events for a resource in tests. */
    static class Tracked {
        final String name;
        boolean open = false;
        boolean closed = false;

        Tracked(String name) { this.name = name; }

        void open() { open = true; }
        void close() { closed = true; }
    }

    static Resource<Tracked> tracked(String name) {
        Tracked t = new Tracked(name);
        return Resource.of(
            () -> { t.open(); return t; },
            r -> r.close()
        );
    }

    // -------------------------------------------------------------------------
    // of / use — success path
    // -------------------------------------------------------------------------

    @Test
    void use_shouldReturnSuccess_whenBodyAndReleaseSucceed() {
        Resource<String> r = Resource.of(() -> "hello", s -> {});
        Try<Integer> result = r.use(String::length);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(5);
    }

    @Test
    void use_shouldAlwaysCallRelease_whenBodySucceeds() {
        AtomicBoolean released = new AtomicBoolean(false);
        Resource<String> r = Resource.of(() -> "hello", s -> released.set(true));
        r.use(String::length);
        assertThat(released.get()).isTrue();
    }

    @Test
    void use_shouldAlwaysCallRelease_whenBodyThrows() {
        AtomicBoolean released = new AtomicBoolean(false);
        RuntimeException bodyEx = new RuntimeException("body");
        Resource<String> r = Resource.of(() -> "hello", s -> released.set(true));
        Try<Integer> result = r.use(s -> { throw bodyEx; });
        assertThat(released.get()).isTrue();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(bodyEx);
    }

    // -------------------------------------------------------------------------
    // Exception-merging contract
    // -------------------------------------------------------------------------

    @Test
    void use_shouldReturnReleaseException_whenBodySucceedsButReleaseThrows() {
        RuntimeException releaseEx = new RuntimeException("release");
        Resource<String> r = Resource.of(() -> "hello", s -> { throw releaseEx; });
        Try<Integer> result = r.use(String::length);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(releaseEx);
    }

    @Test
    void use_shouldSuppressReleaseException_whenBothBodyAndReleaseThrow() {
        RuntimeException bodyEx    = new RuntimeException("body");
        RuntimeException releaseEx = new RuntimeException("release");
        Resource<String> r = Resource.of(() -> "hello", s -> { throw releaseEx; });
        Try<Integer> result = r.use(s -> { throw bodyEx; });
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(bodyEx);
        assertThat(result.getCause().getSuppressed()).containsExactly(releaseEx);
    }

    @Test
    void use_shouldReturnFailure_whenAcquireThrows() {
        RuntimeException acquireEx = new RuntimeException("acquire");
        Resource<String> r = Resource.of(() -> { throw acquireEx; }, s -> {});
        Try<Integer> result = r.use(String::length);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(acquireEx);
    }

    // -------------------------------------------------------------------------
    // fromAutoCloseable
    // -------------------------------------------------------------------------

    @Test
    void fromAutoCloseable_shouldCloseResource_afterUse() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable ac = () -> closed.set(true);
        Resource<AutoCloseable> r = Resource.fromAutoCloseable(() -> ac);
        r.use(a -> "ok");
        assertThat(closed.get()).isTrue();
    }

    @Test
    void fromAutoCloseable_shouldCloseResource_whenBodyThrows() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable ac = () -> closed.set(true);
        Resource<AutoCloseable> r = Resource.fromAutoCloseable(() -> ac);
        r.use(a -> { throw new RuntimeException("oops"); });
        assertThat(closed.get()).isTrue();
    }

    // -------------------------------------------------------------------------
    // map
    // -------------------------------------------------------------------------

    @Test
    void map_shouldTransformValue_andReleaseOriginalResource() {
        Tracked t = new Tracked("db");
        Resource<Tracked> base = Resource.of(() -> { t.open(); return t; }, Tracked::close);
        Resource<String> mapped = base.map(tr -> tr.name.toUpperCase());

        Try<Integer> result = mapped.use(String::length);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(2); // "DB".length()
        assertThat(t.open).isTrue();
        assertThat(t.closed).isTrue();
    }

    @Test
    void map_shouldReleaseResource_whenMapperThrows() {
        AtomicBoolean released = new AtomicBoolean(false);
        RuntimeException mapEx = new RuntimeException("map");
        Resource<String> r = Resource.of(() -> "hello", s -> released.set(true));
        Resource<String> mapped = r.map(s -> { throw mapEx; });
        Try<Integer> result = mapped.use(String::length);
        assertThat(released.get()).isTrue();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(mapEx);
    }

    @Test
    void map_shouldThrowNPE_whenMapperIsNull() {
        Resource<String> r = Resource.of(() -> "hello", s -> {});
        assertThatThrownBy(() -> r.map(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // flatMap
    // -------------------------------------------------------------------------

    @Test
    void flatMap_shouldComposeResources_andReleaseInReverseOrder() {
        List<String> events = new ArrayList<>();

        Resource<String> outer = Resource.of(
            () -> { events.add("outer-acquire"); return "conn"; },
            s  -> events.add("outer-release")
        );
        Resource<String> composed = outer.flatMap(conn ->
            Resource.of(
                () -> { events.add("inner-acquire"); return conn + "+stmt"; },
                s  -> events.add("inner-release")
            )
        );

        Try<Integer> result = composed.use(s -> { events.add("body"); return s.length(); });

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("conn+stmt".length());
        assertThat(events).containsExactly(
            "outer-acquire", "inner-acquire", "body", "inner-release", "outer-release");
    }

    @Test
    void flatMap_shouldReleaseOuterResource_whenInnerBodyThrows() {
        List<String> events = new ArrayList<>();
        RuntimeException bodyEx = new RuntimeException("body");

        Resource<String> outer = Resource.of(
            () -> { events.add("outer-acquire"); return "conn"; },
            s  -> events.add("outer-release")
        );
        Resource<String> composed = outer.flatMap(conn ->
            Resource.of(
                () -> { events.add("inner-acquire"); return conn + "+stmt"; },
                s  -> events.add("inner-release")
            )
        );

        Try<Integer> result = composed.use(s -> { throw bodyEx; });

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(bodyEx);
        assertThat(events).containsExactly(
            "outer-acquire", "inner-acquire", "inner-release", "outer-release");
    }

    @Test
    void flatMap_shouldSuppressOuterReleaseException_ontoBodyException() {
        RuntimeException bodyEx       = new RuntimeException("body");
        RuntimeException outerRelease = new RuntimeException("outer-release");

        Resource<String> outer = Resource.of(() -> "outer", s -> { throw outerRelease; });
        Resource<String> composed = outer.flatMap(s ->
            Resource.of(() -> "inner", i -> {}));

        Try<Integer> result = composed.use(s -> { throw bodyEx; });

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(bodyEx);
        assertThat(result.getCause().getSuppressed()).containsExactly(outerRelease);
    }

    @Test
    void flatMap_shouldThrowNPE_whenFnIsNull() {
        Resource<String> r = Resource.of(() -> "hello", s -> {});
        assertThatThrownBy(() -> r.flatMap(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void flatMap_shouldThrowNPE_whenFnReturnsNull() {
        Resource<String> r = Resource.of(() -> "hello", s -> {});
        Try<Integer> result = r.<String>flatMap(s -> null).use(String::length);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // NPE guards on use / of / fromAutoCloseable
    // -------------------------------------------------------------------------

    @Test
    void use_shouldThrowNPE_whenBodyIsNull() {
        Resource<String> r = Resource.of(() -> "hello", s -> {});
        assertThatThrownBy(() -> r.use(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("body");
    }

    @Test
    void of_shouldThrowNPE_whenAcquireIsNull() {
        assertThatThrownBy(() -> Resource.of(null, s -> {}))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("acquire");
    }

    @Test
    void of_shouldThrowNPE_whenReleaseIsNull() {
        assertThatThrownBy(() -> Resource.of(() -> "hello", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("release");
    }

    @Test
    void fromAutoCloseable_shouldThrowNPE_whenAcquireIsNull() {
        assertThatThrownBy(() -> Resource.fromAutoCloseable(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("acquire");
    }

    // -------------------------------------------------------------------------
    // Reusability — each use() call is independent
    // -------------------------------------------------------------------------

    @Test
    void resource_shouldBeReusable_eachUseAcquiresAndReleases() {
        AtomicInteger acquireCount = new AtomicInteger(0);
        AtomicInteger releaseCount = new AtomicInteger(0);
        Resource<String> r = Resource.of(
            () -> { acquireCount.incrementAndGet(); return "hello"; },
            s  -> releaseCount.incrementAndGet()
        );

        r.use(String::length);
        r.use(String::length);

        assertThat(acquireCount.get()).isEqualTo(2);
        assertThat(releaseCount.get()).isEqualTo(2);
    }
}
