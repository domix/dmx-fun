package dmx.fun.samples;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Tuple2;
import dmx.fun.Tuple3;
import dmx.fun.Validated;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;

/**
 * Demonstrates fun-assertj: fluent custom assertions for dmx-fun types via
 * DmxFunAssertions.assertThat. The entry point coexists with
 * org.assertj.core.api.Assertions.assertThat without naming conflicts.
 */
class AssertJSampleTest {

    // ── Option<T> ────────────────────────────────────────────────────────────

    @Test
    void option_someContainsExpectedValue() {
        Option<String> name = Option.some("Alice");

        assertThat(name)
            .isSome()
            .containsValue("Alice");
    }

    @Test
    void option_noneIsEmpty() {
        Option<String> empty = Option.none();

        assertThat(empty).isNone();
    }

    @Test
    void option_hasValueSatisfyingPredicate() {
        Option<Integer> age = Option.some(30);

        assertThat(age).hasValueSatisfying(a -> {
            org.assertj.core.api.Assertions.assertThat(a).isGreaterThan(18);
        });
    }

    // ── Result<V, E> ─────────────────────────────────────────────────────────

    @Test
    void result_okContainsValue() {
        Result<Integer, String> result = Result.ok(42);

        assertThat(result)
            .isOk()
            .containsValue(42);
    }

    @Test
    void result_errContainsError() {
        Result<Integer, String> result = Result.err("not found");

        assertThat(result)
            .isErr()
            .containsError("not found");
    }

    // ── Try<V> ───────────────────────────────────────────────────────────────

    @Test
    void try_successContainsValue() {
        Try<String> t = Try.of(() -> "hello".toUpperCase());

        assertThat(t)
            .isSuccess()
            .containsValue("HELLO");
    }

    @Test
    void try_failureWithSpecificExceptionType() {
        Try<Integer> t = Try.of(() -> Integer.parseInt("bad"));

        assertThat(t)
            .isFailure()
            .failsWith(NumberFormatException.class);
    }

    // ── Validated<E, A> ──────────────────────────────────────────────────────

    @Test
    void validated_validContainsValue() {
        Validated<String, Integer> v = Validated.valid(100);

        assertThat(v)
            .isValid()
            .containsValue(100);
    }

    @Test
    void validated_invalidHasError() {
        Validated<String, Integer> v = Validated.invalid("must be positive");

        assertThat(v)
            .isInvalid()
            .hasError("must be positive");
    }

    @Test
    void validated_invalidNelHasErrorList() {
        Validated<NonEmptyList<String>, Integer> v =
            Validated.invalidNel("field is required");

        assertThat(v)
            .isInvalid()
            .hasError(NonEmptyList.singleton("field is required"));
    }

    // ── Tuple2/3 ─────────────────────────────────────────────────────────────

    @Test
    void tuple2_hasFirstAndSecond() {
        Tuple2<String, Integer> t = new Tuple2<>("Alice", 30);

        assertThat(t)
            .hasFirst("Alice")
            .hasSecond(30);
    }

    @Test
    void tuple3_hasAllSlots() {
        Tuple3<String, Integer, Boolean> t = Tuple3.of("Alice", 30, true);

        assertThat(t)
            .hasFirst("Alice")
            .hasSecond(30)
            .hasThird(true);
    }
}
