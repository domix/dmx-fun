package dmx.fun;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardTest {
    Guard<Integer> positive = Guard.of(n -> n > 0, "must be positive, got %d"::formatted);
    Guard<String> notBeBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
    Guard<Integer> lessThan100 = lessThan(100);
    Guard<String> email = Guard.of(s -> s.contains("@"), "must contain @");
    Guard<String> phone = Guard.of(s -> s.matches("\\d+"), "must be digits");

    static Guard<Integer> lessThan(int limit) {
        return Guard.of(
            n -> n < limit,
            n -> "must be less than %d, got %d".formatted(limit, n)
        );
    }

    // -------------------------------------------------------------------------
    // of — static message
    // -------------------------------------------------------------------------

    @Test
    void of_staticMessage_returnsValid_whenPredicatePasses() {
        var result = notBeBlank.check("hello");

        assertThat(result.isValid()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
    }

    @Test
    void of_staticMessage_returnsInvalid_whenPredicateFails() {
        var result = notBeBlank.check("   ");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("must not be blank");
    }

    @Test
    void of_staticMessage_shouldThrowNPE_whenPredicateIsNull() {
        assertThatThrownBy(() -> Guard.of(null, "msg"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("predicate");
    }

    @Test
    void of_staticMessage_shouldThrowNPE_whenMessageIsNull() {
        assertThatThrownBy(() -> Guard.of(s -> true, (String) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("errorMessage");
    }

    // -------------------------------------------------------------------------
    // of — dynamic message
    // -------------------------------------------------------------------------

    @Test
    void of_dynamicMessage_returnsValid_whenPredicatePasses() {
        assertThat(positive.check(5).isValid()).isTrue();
    }

    @Test
    void of_dynamicMessage_includesValueInError_whenPredicateFails() {
        var result = positive.check(-3);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("must be positive, got -3");
    }

    @Test
    void of_dynamicMessage_shouldThrowNPE_whenPredicateIsNull() {
        assertThatThrownBy(() -> Guard.of(null, n -> "msg"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("predicate");
    }

    @Test
    void of_dynamicMessage_shouldThrowNPE_whenMessageFnIsNull() {
        assertThatThrownBy(() -> Guard.of(_ -> true, (java.util.function.Function<Object, String>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("errorMessageFn");
    }

    // -------------------------------------------------------------------------
    // and
    // -------------------------------------------------------------------------

    @Test
    void and_returnsValid_whenBothPass() {
        var result = positive
            .and(lessThan100)
            .check(50);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void and_returnsLeftError_whenOnlyLeftFails() {
        var result = positive
            .and(lessThan100)
            .check(-1);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("must be positive, got -1");
    }

    @Test
    void and_returnsRightError_whenOnlyRightFails() {
        var result = positive
            .and(lessThan100)
            .check(150);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("must be less than 100, got 150");
    }

    @Test
    void and_accumulatesAllErrors_whenBothFail() {
        var even = Guard.<Integer>of(n -> n % 2 == 0, "must be even");
        // -1: fails positive (not > 0) and fails even (-1 % 2 != 0)
        var result = positive
            .and(even)
            .check(-1);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList())
            .containsExactly("must be positive, got -1", "must be even");
    }

    @Test
    void and_evaluatesBothGuards_evenWhenFirstFails() {
        var rightCallCount = new AtomicInteger(0);
        var alwaysFail = Guard.<Integer>of(_ -> false, "first");
        var counter = Guard.<Integer>of(_ -> {
            rightCallCount.incrementAndGet();
            return true;
        }, "second");

        alwaysFail
            .and(counter)
            .check(1);

        assertThat(rightCallCount.get()).isEqualTo(1); // right was evaluated
    }

    @Test
    void and_shouldThrowNPE_whenOtherIsNull() {
        var g = Guard.<String>of(_ -> true, "msg");

        assertThatThrownBy(() -> g.and(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("other");
    }

    // -------------------------------------------------------------------------
    // or
    // -------------------------------------------------------------------------

    @Test
    void or_returnsValid_whenLeftPasses() {
        var result = email
            .or(phone)
            .check("alice@example.com");

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void or_returnsValid_whenLeftFailsAndRightPasses() {
        var result = email
            .or(phone)
            .check("12345");

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void or_accumulatesAllErrors_whenBothFail() {
        var result = email
            .or(phone)
            .check("hello");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList())
            .containsExactly("must contain @", "must be digits");
    }

    @Test
    void or_shortCircuits_whenLeftPasses() {
        var rightCallCount = new AtomicInteger(0);
        var alwaysPass = Guard.<String>of(_ -> true, "first");
        var counter = Guard.<String>of(_ -> {
            rightCallCount.incrementAndGet();
            return true;
        }, "second");

        alwaysPass
            .or(counter)
            .check("x");

        assertThat(rightCallCount.get()).isEqualTo(0); // right was NOT evaluated
    }

    @Test
    void or_shouldThrowNPE_whenOtherIsNull() {
        var g = Guard.<String>of(_ -> true, "msg");

        assertThatThrownBy(() -> g.or(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("other");
    }

    // -------------------------------------------------------------------------
    // negate
    // -------------------------------------------------------------------------

    @Test
    void negate_returnsInvalid_whenOriginalPasses() {
        var notAdmin = Guard.<String>of(s -> s.equals("admin"), "is admin").negate();
        assertThat(notAdmin.check("admin").isValid()).isFalse();
    }

    @Test
    void negate_returnsValid_whenOriginalFails() {
        var notAdmin = Guard.<String>of(s -> s.equals("admin"), "is admin").negate();
        assertThat(notAdmin.check("alice").isValid()).isTrue();
    }

    @Test
    void negate_usesGenericMessage_whenNoMessageSupplied() {
        var g = Guard.<String>of(_ -> true, "always passes").negate();
        var result = g.check("x");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().head()).isEqualTo("must not satisfy the condition");
    }

    @Test
    void negate_withMessage_usesProvidedErrorMessage() {
        var noAdmin = Guard.<String>of(s -> s.equals("admin"), "is admin")
            .negate("username must not be 'admin'");

        var result = noAdmin.check("admin");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("username must not be 'admin'");
    }

    @Test
    void negate_withMessage_shouldThrowNPE_whenMessageIsNull() {
        var g = Guard.<String>of(_ -> true, "msg");
        assertThatThrownBy(() -> g.negate(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("errorMessage");
    }

    // -------------------------------------------------------------------------
    // Integration — chained composition
    // -------------------------------------------------------------------------

    @Test
    void check_composedChain_collectsAllViolations() {
        var minLength = Guard.<String>of(s -> s.length() >= 3, "min 3 chars");
        var alphanumeric = Guard.<String>of(s -> s.matches("[a-zA-Z0-9]+"), "must be alphanumeric");

        var username = notBeBlank.and(minLength).and(alphanumeric);

        // Passes all
        assertThat(username.check("alice").isValid()).isTrue();

        // Fails minLength and alphanumeric ("a!") — notBlank passes
        var twoErrors = username.check("a!");
        assertThat(twoErrors.isValid()).isFalse();
        assertThat(twoErrors.getError().toList()).containsExactly("min 3 chars", "must be alphanumeric");

        // Blank string ("  ") fails all three
        var threeErrors = username.check("  ");
        assertThat(threeErrors.isValid()).isFalse();
        assertThat(threeErrors.getError().size()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // asPredicate
    // -------------------------------------------------------------------------

    @Test
    void asPredicate_returnsTrue_whenGuardPasses() {
        assertThat(notBeBlank.asPredicate().test("hello")).isTrue();
    }

    @Test
    void asPredicate_returnsFalse_whenGuardFails() {
        assertThat(notBeBlank.asPredicate().test("   ")).isFalse();
    }

    @Test
    void asPredicate_usefulForStreamFilter() {
        var valid = Stream.of("alice", "  ", "bob", "")
            .filter(notBeBlank.asPredicate())
            .toList();
        assertThat(valid).containsExactly("alice", "bob");
    }

    // -------------------------------------------------------------------------
    // contramap
    // -------------------------------------------------------------------------

    record User(String name) {
    }

    @Test
    void contramap_returnsValid_whenProjectedValuePasses() {
        var userGuard = notBeBlank.contramap(User::name);
        var result = userGuard.check(new User("alice"));
        assertThat(result.isValid()).isTrue();
        assertThat(result.get().name()).isEqualTo("alice");
    }

    @Test
    void contramap_returnsInvalid_withOriginalErrors_whenProjectedValueFails() {
        var userGuard = notBeBlank.contramap(User::name);
        var result = userGuard.check(new User("  "));
        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("must not be blank");
    }

    @Test
    void contramap_preservesOriginalInputOnSuccess() {
        var userGuard = positive.<User>contramap(u -> u.name().length());
        var user = new User("hi");

        var result = userGuard.check(user);

        assertThat(result.isValid()).isTrue();
        assertThat(result.get()).isSameAs(user); // original U value preserved
    }

    @Test
    void contramap_shouldThrowNPE_whenMapperIsNull() {
        var g = Guard.<String>of(_ -> true, "msg");
        assertThatThrownBy(() -> g.contramap(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    // -------------------------------------------------------------------------
    // checkToResult
    // -------------------------------------------------------------------------

    @Test
    void checkToResult_noMapper_returnsOk_whenGuardPasses() {
        var result = notBeBlank.checkToResult("hello");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
    }

    @Test
    void checkToResult_noMapper_returnsErr_whenGuardFails() {
        var result = notBeBlank.checkToResult("   ");
        assertThat(result.isError()).isTrue();
        assertThat(result.getError().toList()).containsExactly("must not be blank");
    }

    @Test
    void checkToResult_withMapper_returnsOk_whenGuardPasses() {
        var result = notBeBlank.checkToResult("hello", errors -> String.join(", ", errors.toList()));
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void checkToResult_withMapper_mapsErrors_whenGuardFails() {
        var result = notBeBlank.checkToResult("   ", errors -> String.join(", ", errors.toList()));
        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).isEqualTo("must not be blank");
    }

    @Test
    void checkToResult_withMapper_shouldThrowNPE_whenMapperIsNull() {
        var g = Guard.<String>of(_ -> true, "msg");
        assertThatThrownBy(() -> g.checkToResult("x", (java.util.function.Function<NonEmptyList<String>, String>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("toError");
    }

    // -------------------------------------------------------------------------
    // checkToOption
    // -------------------------------------------------------------------------

    @Test
    void checkToOption_returnsSome_whenGuardPasses() {
        var result = notBeBlank.checkToOption("hello");
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
    }

    @Test
    void checkToOption_returnsNone_whenGuardFails() {
        var result = notBeBlank.checkToOption("   ");
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void checkToOption_usefulForStreamFlatMap() {
        var valid = Stream.of("alice", "  ", "bob")
            .flatMap(s -> notBeBlank.checkToOption(s).stream())
            .toList();
        assertThat(valid).containsExactly("alice", "bob");
    }

    // -------------------------------------------------------------------------
    // nonNull — static factory
    // -------------------------------------------------------------------------

    @Test
    void nonNull_rejectsNull() {
        var g = Guard.<String>nonNull();
        var result = g.check(null);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("must not be null");
    }

    @Test
    void nonNull_acceptsNonNull() {
        var g = Guard.<String>nonNull();
        var result = g.check("hello");
        assertThat(result.isValid()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
    }

    @Test
    void nonNull_worksForAnyType() {
        var g = Guard.<Integer>nonNull();
        assertThat(g.check(42).isValid()).isTrue();
        assertThat(g.check(null).isValid()).isFalse();
    }

    // -------------------------------------------------------------------------
    // andThen — short-circuit composition
    // -------------------------------------------------------------------------

    @Test
    void andThen_returnsValid_whenBothPass() {
        var minLength = Guard.<String>of(s -> s.length() >= 3, "must be at least 3 chars");

        var result = notBeBlank
            .andThen(minLength)
            .check("alice");

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void andThen_returnsFirstInvalid_andSkipsNext_whenFirstFails() {
        var nonNull = Guard.<String>nonNull();
        var notBlank = Guard.<@Nullable String>of(
            s -> s != null && !s.isBlank(), "must not be blank");

        // nonNull fails → notBlank must NOT be called (would NPE on null)
        var result = nonNull
            .andThen(notBlank)
            .check(null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("must not be null");
    }

    @Test
    void andThen_returnsNextInvalid_whenFirstPassesAndNextFails() {
        var nonNull = Guard.<String>nonNull();

        var result = nonNull.andThen(notBeBlank).check("   ");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("must not be blank");
    }

    @Test
    void andThen_shouldThrowNPE_whenNextIsNull() {
        assertThatThrownBy(() -> notBeBlank.andThen(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void check_integratesWithValidatedCombine_toAccumulateAcrossFields() {
        var email = Guard.<String>of(s -> s.contains("@"), "email must contain @");

        var username = notBeBlank.check("  ");
        var emailVal = email.check("not-an-email");

        var combined = username
            .combine(
                emailVal,
                NonEmptyList::concat,
                "%s/%s"::formatted
            );

        assertThat(combined.isValid()).isFalse();
        assertThat(combined.getError().toList())
            .containsExactly("must not be blank", "email must contain @");
    }
}
