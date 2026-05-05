package dmx.fun.samples;

import dmx.fun.Accumulator;
import dmx.fun.Either;
import dmx.fun.Guard;
import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Resource;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Tuple2;
import dmx.fun.Tuple3;
import dmx.fun.Validated;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;

/**
 * Demonstrates fun-assertj: fluent custom assertions for dmx-fun types via
 * DmxFunAssertions.assertThat. The entry point coexists with
 * org.assertj.core.api.Assertions.assertThat without naming conflicts.
 */
class AssertJSampleTest {

    // ── Either<L, R> ─────────────────────────────────────────────────────────

    @Test
    void either_rightContainsValue() {
        // Models a computation that produces the success track (Right).
        var parsed = Either.right(42);

        assertThat(parsed)
            .isRight()
            .containsRight(42);
    }

    @Test
    void either_leftContainsError() {
        // Models a computation that ended on the error track (Left).
        var failed = Either.left("invalid input");

        assertThat(failed)
            .isLeft()
            .containsLeft("invalid input");
    }

    @Test
    void either_isRight_afterSwap() {
        // swap() exchanges left and right — a Left becomes a Right.
        var original = Either.<String, Integer>left("msg");
        var swapped  = original.swap();

        assertThat(swapped)
            .isRight()
            .containsRight("msg");
    }

    // ── Option<T> ────────────────────────────────────────────────────────────

    @Test
    void option_someContainsExpectedValue() {
        var name = Option.some("Alice");

        assertThat(name)
            .isSome()
            .containsValue("Alice");
    }

    @Test
    void option_noneIsEmpty() {
        var empty = Option.none();

        assertThat(empty).isNone();
    }

    @Test
    void option_hasValueSatisfyingPredicate() {
        var age = Option.some(30);

        assertThat(age).hasValueSatisfying(a -> {
            org.assertj.core.api.Assertions.assertThat(a).isGreaterThan(18);
        });
    }

    // ── Result<V, E> ─────────────────────────────────────────────────────────

    @Test
    void result_okContainsValue() {
        var result = Result.ok(42);

        assertThat(result)
            .isOk()
            .containsValue(42);
    }

    @Test
    void result_errContainsError() {
        var result = Result.err("not found");

        assertThat(result)
            .isErr()
            .containsError("not found");
    }

    // ── Try<V> ───────────────────────────────────────────────────────────────

    @Test
    void try_successContainsValue() {
        var t = Try.of("hello"::toUpperCase);

        assertThat(t)
            .isSuccess()
            .containsValue("HELLO");
    }

    @Test
    void try_failureWithSpecificExceptionType() {
        var t = Try.of(() -> Integer.parseInt("bad"));

        assertThat(t)
            .isFailure()
            .failsWith(NumberFormatException.class);
    }

    // ── Validated<E, A> ──────────────────────────────────────────────────────

    @Test
    void validated_validContainsValue() {
        var v = Validated.valid(100);

        assertThat(v)
            .isValid()
            .containsValue(100);
    }

    @Test
    void validated_invalidHasError() {
        var v = Validated.invalid("must be positive");

        assertThat(v)
            .isInvalid()
            .hasError("must be positive");
    }

    @Test
    void validated_invalidNelHasErrorList() {
        var v = Validated.invalidNel("field is required");

        assertThat(v)
            .isInvalid()
            .hasError(NonEmptyList.singleton("field is required"));
    }

    // ── Tuple2/3 ─────────────────────────────────────────────────────────────

    @Test
    void tuple2_hasFirstAndSecond() {
        var t = new Tuple2<>("Alice", 30);

        assertThat(t)
            .hasFirst("Alice")
            .hasSecond(30);
    }

    @Test
    void tuple3_hasAllSlots() {
        var t = Tuple3.of("Alice", 30, true);

        assertThat(t)
            .hasFirst("Alice")
            .hasSecond(30)
            .hasThird(true);
    }

    // ── Resource<T> ──────────────────────────────────────────────────────────

    @Test
    void resource_succeedsWith_whenAcquireAndReleaseComplete() {
        // Simulates acquiring a database connection string and releasing it safely.
        // succeedsWith checks that use(v -> v) returns the acquired value unchanged.
        var connectionString = Resource.of(
            () -> "jdbc:postgresql://localhost/mydb",
            _ -> { /* release: return to pool */ }
        );

        assertThat(connectionString).succeedsWith("jdbc:postgresql://localhost/mydb");
    }

    @Test
    void resource_failsWith_whenAcquireThrows() {
        // A resource whose acquisition always fails — e.g. cannot open the file.
        var unavailable = Resource.of(
            () -> { throw new java.io.IOException("file not found"); },
            _  -> { /* release never called */ }
        );

        assertThat(unavailable).failsWith(java.io.IOException.class);
    }

    @Test
    void resource_failsWithMessage_whenAcquireThrowsWithKnownMessage() {
        // The acquire step fails with a predictable message — useful for asserting
        // that the correct error propagates through the resource lifecycle.
        var unavailable = Resource.of(
            () -> { throw new IllegalStateException("config file missing: app.yml"); },
            _  -> {}
        );

        assertThat(unavailable).failsWithMessage("config file missing: app.yml");
    }

    // ── Guard<T> ─────────────────────────────────────────────────────────────

    @Test
    void guard_accepts_whenPredicatePasses() {
        // A username must be 3–20 characters and contain only alphanumerics.
        var usernameGuard = Guard.<String>of(
            s -> s.length() >= 3 && s.length() <= 20 && s.matches("[A-Za-z0-9]+"),
            "username must be 3–20 alphanumeric characters"
        );

        assertThat(usernameGuard).accepts("alice99");
    }

    @Test
    void guard_rejects_whenPredicateFails() {
        var usernameGuard = Guard.<String>of(
            s -> s.length() >= 3 && s.length() <= 20 && s.matches("[A-Za-z0-9]+"),
            "username must be 3–20 alphanumeric characters"
        );

        assertThat(usernameGuard).rejects("a!");
    }

    @Test
    void guard_rejectsWithMessage_includesContextualError() {
        var positiveAmount = Guard.<Integer>of(
            n -> n > 0,
            n -> "amount must be positive, got: " + n
        );

        assertThat(positiveAmount).rejectsWithMessage(-5, "amount must be positive, got: -5");
    }

    @Test
    void guard_rejectsWithMessages_whenMultipleRulesApplied() {
        // Compose two guards: minimum length and no whitespace.
        var minLength  = Guard.<String>of(s -> s.length() >= 8, "must be at least 8 characters");
        var noSpaces   = Guard.<String>of(s -> !s.contains(" "), "must not contain spaces");
        var passwordGuard = minLength.and(noSpaces);

        // "hi pw" has 5 chars (fails minLength) and a space (fails noSpaces) — both errors accumulated
        assertThat(passwordGuard)
            .rejectsWithMessages("hi pw", "must be at least 8 characters", "must not contain spaces");
    }

    // ── Accumulator<E, A> ────────────────────────────────────────────────────

    @Test
    void accumulator_hasValue_whenPrimaryValuePresent() {
        // Models a pipeline step that produces a result and logs a message.
        var step =
            Accumulator.of(42, List.of("computed answer"));

        assertThat(step).hasValue(42);
    }

    @Test
    void accumulator_hasAccumulation_matchesLogEntries() {
        var step =
            Accumulator.of("Alice", List.of("fetched user", "applied discount"));

        assertThat(step).hasAccumulation(List.of("fetched user", "applied discount"));
    }

    @Test
    void accumulator_accumulationContains_singleAuditEntry() {
        var result =
            Accumulator.of(100, List.of("order created", "stock reserved"));

        assertThat(result)
            .accumulationContains("order created")
            .accumulationContains("stock reserved");
    }

    @Test
    void accumulator_accumulationHasSize_tracksStepCount() {
        // A multi-step pipeline that accumulates one log entry per step.
        var pipeline =
            Accumulator.of("done", List.of("step1", "step2", "step3"));

        assertThat(pipeline).accumulationHasSize(3);
    }

    @Test
    void accumulator_tell_sideEffectOnly() {
        // tell() creates an accumulator with no primary value — useful for pure logging steps.
        var log =
            Accumulator.tell(List.of("audit: payment initiated"));

        assertThat(log)
            .hasValue(null)
            .accumulationContains("audit: payment initiated");
    }
}
