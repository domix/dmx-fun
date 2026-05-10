package dmx.fun;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Tracks open/close events for a resource in tests.
     */
    static class Tracked {
        final String name;
        boolean open = false;
        boolean closed = false;

        Tracked(String name) {
            this.name = name;
        }

        void open() {
            open = true;
        }

        void close() {
            closed = true;
        }
    }

    static Resource<Tracked> tracked(String name) {
        return Resource.of(
            () -> {
                Tracked t = new Tracked(name);
                t.open();
                return t;
            },
            r -> r.close()
        );
    }

    // -------------------------------------------------------------------------
    // of / use — success path
    // -------------------------------------------------------------------------

    @Test
    void use_shouldReturnSuccess_whenBodyAndReleaseSucceed() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        var result = r.use(String::length);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(5);
    }

    @Test
    void use_nullBodyResult_shouldReturnFailureWithNPE() {
        var released = new AtomicBoolean(false);
        var r = Resource.of(() -> "hello", _ -> released.set(true));

        var result = r.use(_ -> null);

        assertThat(released.get()).isTrue();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("body returned null");
    }

    @Test
    void use_shouldAlwaysCallRelease_whenBodySucceeds() {
        var released = new AtomicBoolean(false);
        var r = Resource.of(() -> "hello", _ -> released.set(true));

        r.use(String::length);
        assertThat(released.get()).isTrue();
    }

    @Test
    void use_shouldAlwaysCallRelease_whenBodyThrows() {
        var released = new AtomicBoolean(false);
        var bodyEx = new RuntimeException("body");
        var r = Resource.of(() -> "hello", _ -> released.set(true));
        var result = r.use(_ -> {
            throw bodyEx;
        });

        assertThat(released.get()).isTrue();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(bodyEx);
    }

    // -------------------------------------------------------------------------
    // Exception-merging contract
    // -------------------------------------------------------------------------

    @Test
    void use_shouldReturnReleaseException_whenBodySucceedsButReleaseThrows() {
        var releaseEx = new RuntimeException("release");
        var r = Resource.of(() -> "hello", _ -> {
            throw releaseEx;
        });
        var result = r.use(String::length);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(releaseEx);
    }

    @Test
    void use_shouldSuppressReleaseException_whenBothBodyAndReleaseThrow() {
        var bodyEx = new RuntimeException("body");
        var releaseEx = new RuntimeException("release");
        var r = Resource.of(() -> "hello", _ -> {
            throw releaseEx;
        });
        var result = r.use(_ -> {
            throw bodyEx;
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(bodyEx);
        assertThat(result.getCause().getSuppressed()).containsExactly(releaseEx);
    }

    @Test
    void use_shouldReturnFailure_whenAcquireThrows() {
        var acquireEx = new RuntimeException("acquire");
        var r = Resource.<String>of(() -> {
            throw acquireEx;
        }, _ -> {
        });
        var result = r.use(String::length);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(acquireEx);
    }

    // -------------------------------------------------------------------------
    // fromAutoCloseable
    // -------------------------------------------------------------------------

    @Test
    void fromAutoCloseable_shouldCloseResource_afterUse() {
        var closed = new AtomicBoolean(false);
        AutoCloseable ac = () -> closed.set(true);
        var r = Resource.fromAutoCloseable(() -> ac);

        r.use(a -> "ok");
        assertThat(closed.get()).isTrue();
    }

    @Test
    void fromAutoCloseable_shouldCloseResource_whenBodyThrows() {
        var closed = new AtomicBoolean(false);
        AutoCloseable ac = () -> closed.set(true);
        var r = Resource.fromAutoCloseable(() -> ac);

        r.use(_ -> {
            throw new RuntimeException("oops");
        });
        assertThat(closed.get()).isTrue();
    }

    // -------------------------------------------------------------------------
    // map
    // -------------------------------------------------------------------------

    @Test
    void map_shouldTransformValue_andReleaseOriginalResource() {
        var t = new Tracked("db");
        var base = Resource.of(
            () -> {
                t.open();
                return t;
            },
            Tracked::close
        );
        var mapped = base.map(tr -> tr.name.toUpperCase());
        var result = mapped.use(String::length);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(2); // "DB".length()
        assertThat(t.open).isTrue();
        assertThat(t.closed).isTrue();
    }

    @Test
    void map_shouldReleaseResource_whenMapperThrows() {
        var released = new AtomicBoolean(false);
        var mapEx = new RuntimeException("map");
        var r = Resource.of(
            () -> "hello",
            _ -> released.set(true)
        );
        var mapped = r.<String>map(_ -> {
            throw mapEx;
        });
        var result = mapped.use(String::length);

        assertThat(released.get()).isTrue();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(mapEx);
    }

    @Test
    void map_shouldThrowNPE_whenMapperIsNull() {
        var r = Resource.of(() -> "hello", _ -> {
        });

        assertThatThrownBy(() -> r.map(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    // -------------------------------------------------------------------------
    // flatMap
    // -------------------------------------------------------------------------

    @Test
    void flatMap_shouldComposeResources_andReleaseInReverseOrder() {
        var events = new ArrayList<String>();

        var outer = Resource.of(
            () -> {
                events.add("outer-acquire");
                return "conn";
            },
            _ -> events.add("outer-release")
        );
        var composed = outer.flatMap(conn ->
            Resource.of(
                () -> {
                    events.add("inner-acquire");
                    return conn + "+stmt";
                },
                _ -> events.add("inner-release")
            )
        );

        var result = composed.use(s -> {
            events.add("body");
            return s.length();
        });

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("conn+stmt".length());
        assertThat(events).containsExactly(
            "outer-acquire", "inner-acquire", "body", "inner-release", "outer-release");
    }

    @Test
    void flatMap_shouldReleaseOuterResource_whenInnerBodyThrows() {
        var events = new ArrayList<String>();
        var bodyEx = new RuntimeException("body");

        var outer = Resource.of(
            () -> {
                events.add("outer-acquire");
                return "conn";
            },
            _ -> events.add("outer-release")
        );
        var composed = outer.flatMap(conn ->
            Resource.of(
                () -> {
                    events.add("inner-acquire");
                    return conn + "+stmt";
                },
                _ -> events.add("inner-release")
            )
        );
        var result = composed.use(_ -> {
            throw bodyEx;
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(bodyEx);
        assertThat(events).containsExactly(
            "outer-acquire", "inner-acquire", "inner-release", "outer-release");
    }

    @Test
    void flatMap_shouldSuppressOuterReleaseException_ontoBodyException() {
        var bodyEx = new RuntimeException("body");
        var outerRelease = new RuntimeException("outer-release");

        var outer = Resource.of(() -> "outer", _ -> {
            throw outerRelease;
        });
        var composed = outer.flatMap(
            _ -> Resource.of(() -> "inner", _ -> {
            })
        );
        var result = composed.use(_ -> {
            throw bodyEx;
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(bodyEx);
        assertThat(result.getCause().getSuppressed()).containsExactly(outerRelease);
    }

    @Test
    void flatMap_shouldThrowNPE_whenFnIsNull() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        assertThatThrownBy(() -> r.flatMap(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("resourceMapper");
    }

    @Test
    void flatMap_shouldThrowNPE_whenFnReturnsNull() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        var result = r.<String>flatMap(_ -> null).use(String::length);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // NPE guards on use / of / fromAutoCloseable
    // -------------------------------------------------------------------------

    @Test
    void use_shouldThrowNPE_whenBodyIsNull() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        assertThatThrownBy(() -> r.use(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("body");
    }

    @Test
    void of_shouldThrowNPE_whenAcquireIsNull() {
        assertThatThrownBy(() -> Resource.of(null, _ -> {
        }))
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
        var acquireCount = new AtomicInteger(0);
        var releaseCount = new AtomicInteger(0);
        var r = Resource.of(
            () -> {
                acquireCount.incrementAndGet();
                return "hello";
            },
            _ -> releaseCount.incrementAndGet()
        );

        r.use(String::length);
        r.use(String::length);

        assertThat(acquireCount.get()).isEqualTo(2);
        assertThat(releaseCount.get()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // eval — factory from pre-computed Try
    // -------------------------------------------------------------------------

    @Test
    void eval_shouldRunBodyAndRelease_whenTryIsSuccess() {
        var released = new AtomicBoolean(false);
        var r = Resource.eval(Try.success("hello"), _ -> released.set(true));
        var result = r.use(String::length);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(5);
        assertThat(released.get()).isTrue();
    }

    @Test
    void eval_shouldReturnFailure_andSkipRelease_whenTryIsFailure() {
        var released = new AtomicBoolean(false);
        var acquireEx = new RuntimeException("acquire failed");
        var r = Resource.<String>eval(
            Try.failure(acquireEx),
            _ -> released.set(true)
        );
        var result = r.use(String::length);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(acquireEx);
        assertThat(released.get()).isFalse();
    }

    @Test
    void eval_shouldAlwaysCallRelease_whenBodyThrows() {
        var released = new AtomicBoolean(false);
        var bodyEx = new RuntimeException("body");
        var r = Resource.eval(
            Try.success("hello"),
            _ -> released.set(true)
        );
        var result = r.use(_ -> {
            throw bodyEx;
        });

        assertThat(released.get()).isTrue();
        assertThat(result.getCause()).isSameAs(bodyEx);
    }

    @Test
    void eval_shouldThrowNPE_whenAcquiredIsNull() {
        assertThatThrownBy(() -> Resource.eval(null, _ -> {
        }))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("acquired");
    }

    @Test
    void eval_shouldThrowNPE_whenReleaseIsNull() {
        assertThatThrownBy(() -> Resource.eval(Try.success("x"), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("release");
    }

    // -------------------------------------------------------------------------
    // useAsResult — Result-integrated use
    // -------------------------------------------------------------------------

    @Test
    void useAsResult_shouldReturnOk_whenBodyReturnsOk() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        var result = r.useAsResult(
            s -> Result.ok(s.length()),
            Throwable::getMessage
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(5);
    }

    @Test
    void useAsResult_shouldReturnErr_whenBodyReturnsErr() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        var result = r.useAsResult(
            _ -> Result.err("domain error"),
            Throwable::getMessage
        );

        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).isEqualTo("domain error");
    }

    @Test
    void useAsResult_shouldMapToErr_whenAcquireThrows() {
        var acquireEx = new RuntimeException("acquire");
        var r = Resource.<String>of(() -> {
            throw acquireEx;
        }, _ -> {
        });
        var result = r.useAsResult(
            s -> Result.ok(s.length()),
            Throwable::getMessage
        );

        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).isEqualTo("acquire");
    }

    @Test
    void useAsResult_shouldMapToErr_whenBodyThrowsUnexpectedly() {
        var bodyEx = new RuntimeException("unexpected");
        var r = Resource.of(() -> "hello", _ -> {
        });
        var result = r.useAsResult(
            _ -> {
                throw bodyEx;
            },
            Throwable::getMessage
        );

        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).isEqualTo("unexpected");
    }

    @Test
    void useAsResult_shouldAlwaysRelease_whenBodyThrowsUnexpectedly() {
        var released = new AtomicBoolean(false);
        var r = Resource.of(() -> "hello", _ -> released.set(true));
        r.useAsResult(
            _ -> {
                throw new RuntimeException("oops");
            },
            Throwable::getMessage
        );
        assertThat(released.get()).isTrue();
    }

    @Test
    void useAsResult_shouldThrowNPE_whenBodyIsNull() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        assertThatThrownBy(() -> r.useAsResult(null, Throwable::getMessage))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("body");
    }

    @Test
    void useAsResult_shouldThrowNPE_whenOnErrorIsNull() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        assertThatThrownBy(() -> r.useAsResult(s -> Result.ok(s.length()), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("onError");
    }

    @Test
    void useAsResult_nullOnErrorReturn_shouldThrowNPEWithDescriptiveMessage() {
        var r = Resource.<String>of(() -> { throw new RuntimeException("fail"); }, _ -> {});
        assertThatThrownBy(() -> r.useAsResult(_ -> Result.ok("x"), _ -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("onError returned null");
    }

    // -------------------------------------------------------------------------
    // useAsEither — Either-integrated use
    // -------------------------------------------------------------------------

    @Test
    void useAsEither_shouldReturnRight_whenBodyReturnsRight() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        var result = r.useAsEither(
            s -> Either.right(s.length()),
            Throwable::getMessage
        );

        assertThat(result.isRight()).isTrue();
        assertThat(result.getRight()).isEqualTo(5);
    }

    @Test
    void useAsEither_shouldReturnLeft_whenBodyReturnsLeft() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        var result = r.useAsEither(
            _ -> Either.left("domain error"),
            Throwable::getMessage
        );

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEqualTo("domain error");
    }

    @Test
    void useAsEither_shouldMapToLeft_whenAcquireThrows() {
        var acquireEx = new RuntimeException("acquire");
        var r = Resource.<String>of(() -> {
            throw acquireEx;
        }, _ -> {
        });
        var result = r.useAsEither(
            s -> Either.right(s.length()),
            Throwable::getMessage
        );

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEqualTo("acquire");
    }

    @Test
    void useAsEither_shouldMapToLeft_whenBodyThrowsUnexpectedly() {
        var bodyEx = new RuntimeException("unexpected");
        var r = Resource.of(() -> "hello", _ -> {
        });
        var result = r.useAsEither(
            _ -> {
                throw bodyEx;
            },
            Throwable::getMessage
        );

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEqualTo("unexpected");
    }

    @Test
    void useAsEither_shouldAlwaysRelease_whenBodyThrowsUnexpectedly() {
        var released = new AtomicBoolean(false);
        var r = Resource.of(() -> "hello", _ -> released.set(true));
        r.useAsEither(
            _ -> {
                throw new RuntimeException("oops");
            },
            Throwable::getMessage
        );

        assertThat(released.get()).isTrue();
    }

    @Test
    void useAsEither_shouldSuppressReleaseException_whenBothBodyAndReleaseThrow() {
        var bodyEx    = new RuntimeException("body");
        var releaseEx = new RuntimeException("release");
        var r = Resource.of(() -> "hello", _ -> { throw releaseEx; });
        var captured = new java.util.concurrent.atomic.AtomicReference<Throwable>();

        var result = r.useAsEither(
            _ -> { throw bodyEx; },
            t -> { captured.set(t); return t.getMessage(); }
        );

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEqualTo("body");
        assertThat(captured.get()).isSameAs(bodyEx);
        assertThat(captured.get().getSuppressed()).containsExactly(releaseEx);
    }

    @Test
    void useAsEither_shouldReturnLeft_whenBodyReturnsNull() {
        var r = Resource.of(() -> "hello", _ -> {});

        var result = r.useAsEither(_ -> null, Throwable::getMessage);

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).contains("body");
    }

    @Test
    void useAsEither_shouldThrowNPE_whenBodyIsNull() {
        var r = Resource.of(() -> "hello", _ -> {
        });

        assertThatThrownBy(() -> r.useAsEither(null, Throwable::getMessage))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("body");
    }

    @Test
    void useAsEither_shouldThrowNPE_whenOnErrorIsNull() {
        var r = Resource.of(() -> "hello", _ -> {
        });

        assertThatThrownBy(() -> r.useAsEither(s -> Either.right(s.length()), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("onError");
    }

    @Test
    void useAsEither_nullOnErrorReturn_shouldThrowNPEWithDescriptiveMessage() {
        var r = Resource.<String>of(() -> { throw new RuntimeException("fail"); }, _ -> {});
        assertThatThrownBy(() -> r.useAsEither(_ -> Either.right("x"), _ -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("onError returned null");
    }

    // -------------------------------------------------------------------------
    // mapTry — transform resource value with Try-returning function
    // -------------------------------------------------------------------------

    @Test
    void mapTry_shouldApplyBody_whenFnReturnsSuccess() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        var mapped = r.mapTry(s -> Try.success(s.length()));
        var result = mapped.use(n -> "len=" + n);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("len=5");
    }

    @Test
    void mapTry_shouldReturnFailure_whenFnReturnsFailure() {
        var parseEx = new RuntimeException("parse failed");
        var r = Resource.of(() -> "bad", _ -> {
        });
        var mapped = r.mapTry(_ -> Try.failure(parseEx));
        var result = mapped.use(n -> "len=" + n);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isSameAs(parseEx);
    }

    @Test
    void mapTry_shouldReleaseResource_whenFnReturnsFailure() {
        var released = new AtomicBoolean(false);
        var r = Resource.of(() -> "hello", _ -> released.set(true));
        r.mapTry(
            _ -> Try.failure(new RuntimeException("fail"))
        ).use(n -> n);

        assertThat(released.get()).isTrue();
    }

    @Test
    void mapTry_shouldThrowNPE_whenFnIsNull() {
        var r = Resource.of(() -> "hello", _ -> {
        });

        assertThatThrownBy(() -> r.mapTry(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    @Test
    void mapTry_shouldThrowNPE_whenFnReturnsNull() {
        var r = Resource.of(() -> "hello", _ -> {
        });
        var result = r.<Integer>mapTry(_ -> null)
            .use(n -> n);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(NullPointerException.class);
    }
}
