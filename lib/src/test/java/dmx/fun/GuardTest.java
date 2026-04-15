package dmx.fun;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardTest {

    // -------------------------------------------------------------------------
    // of — static message
    // -------------------------------------------------------------------------

    @Test
    void of_staticMessage_returnsValid_whenPredicatePasses() {
        Guard<String> g = Guard.of(s -> !s.isBlank(), "must not be blank");
        Validated<NonEmptyList<String>, String> result = g.check("hello");
        assertThat(result.isValid()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
    }

    @Test
    void of_staticMessage_returnsInvalid_whenPredicateFails() {
        Guard<String> g = Guard.of(s -> !s.isBlank(), "must not be blank");
        Validated<NonEmptyList<String>, String> result = g.check("   ");
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
        Guard<Integer> g = Guard.of(n -> n > 0, n -> "must be positive, got " + n);
        assertThat(g.check(5).isValid()).isTrue();
    }

    @Test
    void of_dynamicMessage_includesValueInError_whenPredicateFails() {
        Guard<Integer> g = Guard.of(n -> n > 0, n -> "must be positive, got " + n);
        Validated<NonEmptyList<String>, Integer> result = g.check(-3);
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
        assertThatThrownBy(() -> Guard.of(n -> true, (java.util.function.Function<Object, String>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("errorMessageFn");
    }

    // -------------------------------------------------------------------------
    // and
    // -------------------------------------------------------------------------

    @Test
    void and_returnsValid_whenBothPass() {
        Guard<Integer> positive    = Guard.of(n -> n > 0,   "must be positive");
        Guard<Integer> lessThan100 = Guard.of(n -> n < 100, "must be less than 100");
        assertThat(positive.and(lessThan100).check(50).isValid()).isTrue();
    }

    @Test
    void and_returnsLeftError_whenOnlyLeftFails() {
        Guard<Integer> positive    = Guard.of(n -> n > 0,   "must be positive");
        Guard<Integer> lessThan100 = Guard.of(n -> n < 100, "must be less than 100");
        Validated<NonEmptyList<String>, Integer> result = positive.and(lessThan100).check(-1);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("must be positive");
    }

    @Test
    void and_returnsRightError_whenOnlyRightFails() {
        Guard<Integer> positive    = Guard.of(n -> n > 0,   "must be positive");
        Guard<Integer> lessThan100 = Guard.of(n -> n < 100, "must be less than 100");
        Validated<NonEmptyList<String>, Integer> result = positive.and(lessThan100).check(150);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("must be less than 100");
    }

    @Test
    void and_accumulatesAllErrors_whenBothFail() {
        Guard<Integer> positive = Guard.of(n -> n > 0,      "must be positive");
        Guard<Integer> even     = Guard.of(n -> n % 2 == 0, "must be even");
        // -1: fails positive (not > 0) and fails even (-1 % 2 != 0)
        Validated<NonEmptyList<String>, Integer> result = positive.and(even).check(-1);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList())
            .containsExactly("must be positive", "must be even");
    }

    @Test
    void and_evaluatesBothGuards_evenWhenFirstFails() {
        AtomicInteger rightCallCount = new AtomicInteger(0);
        Guard<Integer> alwaysFail = Guard.of(n -> false, "first");
        Guard<Integer> counter    = Guard.of(n -> { rightCallCount.incrementAndGet(); return true; }, "second");
        alwaysFail.and(counter).check(1);
        assertThat(rightCallCount.get()).isEqualTo(1); // right was evaluated
    }

    @Test
    void and_shouldThrowNPE_whenOtherIsNull() {
        Guard<String> g = Guard.of(s -> true, "msg");
        assertThatThrownBy(() -> g.and(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("other");
    }

    // -------------------------------------------------------------------------
    // or
    // -------------------------------------------------------------------------

    @Test
    void or_returnsValid_whenLeftPasses() {
        Guard<String> email = Guard.of(s -> s.contains("@"), "must contain @");
        Guard<String> phone = Guard.of(s -> s.matches("\\d+"), "must be digits");
        assertThat(email.or(phone).check("alice@example.com").isValid()).isTrue();
    }

    @Test
    void or_returnsValid_whenLeftFailsAndRightPasses() {
        Guard<String> email = Guard.of(s -> s.contains("@"), "must contain @");
        Guard<String> phone = Guard.of(s -> s.matches("\\d+"), "must be digits");
        assertThat(email.or(phone).check("12345").isValid()).isTrue();
    }

    @Test
    void or_accumulatesAllErrors_whenBothFail() {
        Guard<String> email = Guard.of(s -> s.contains("@"), "must contain @");
        Guard<String> phone = Guard.of(s -> s.matches("\\d+"), "must be digits");
        Validated<NonEmptyList<String>, String> result = email.or(phone).check("hello");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList())
            .containsExactly("must contain @", "must be digits");
    }

    @Test
    void or_shortCircuits_whenLeftPasses() {
        AtomicInteger rightCallCount = new AtomicInteger(0);
        Guard<String> alwaysPass = Guard.of(s -> true, "first");
        Guard<String> counter    = Guard.of(s -> { rightCallCount.incrementAndGet(); return true; }, "second");
        alwaysPass.or(counter).check("x");
        assertThat(rightCallCount.get()).isEqualTo(0); // right was NOT evaluated
    }

    @Test
    void or_shouldThrowNPE_whenOtherIsNull() {
        Guard<String> g = Guard.of(s -> true, "msg");
        assertThatThrownBy(() -> g.or(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("other");
    }

    // -------------------------------------------------------------------------
    // negate
    // -------------------------------------------------------------------------

    @Test
    void negate_returnsInvalid_whenOriginalPasses() {
        Guard<String> notAdmin = Guard.<String>of(s -> s.equals("admin"), "is admin").negate();
        assertThat(notAdmin.check("admin").isValid()).isFalse();
    }

    @Test
    void negate_returnsValid_whenOriginalFails() {
        Guard<String> notAdmin = Guard.<String>of(s -> s.equals("admin"), "is admin").negate();
        assertThat(notAdmin.check("alice").isValid()).isTrue();
    }

    @Test
    void negate_usesGenericMessage_whenNoMessageSupplied() {
        Guard<String> g = Guard.<String>of(s -> true, "always passes").negate();
        Validated<NonEmptyList<String>, String> result = g.check("x");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().head()).isEqualTo("must not satisfy the condition");
    }

    @Test
    void negate_withMessage_usesProvidedErrorMessage() {
        Guard<String> noAdmin = Guard.<String>of(s -> s.equals("admin"), "is admin")
            .negate("username must not be 'admin'");
        Validated<NonEmptyList<String>, String> result = noAdmin.check("admin");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("username must not be 'admin'");
    }

    @Test
    void negate_withMessage_shouldThrowNPE_whenMessageIsNull() {
        Guard<String> g = Guard.of(s -> true, "msg");
        assertThatThrownBy(() -> g.negate(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("errorMessage");
    }

    // -------------------------------------------------------------------------
    // Integration — chained composition
    // -------------------------------------------------------------------------

    @Test
    void check_composedChain_collectsAllViolations() {
        Guard<String> notBlank    = Guard.of(s -> !s.isBlank(),              "must not be blank");
        Guard<String> minLength   = Guard.of(s -> s.length() >= 3,           "min 3 chars");
        Guard<String> alphanumeric = Guard.of(s -> s.matches("[a-zA-Z0-9]+"), "must be alphanumeric");

        Guard<String> username = notBlank.and(minLength).and(alphanumeric);

        // Passes all
        assertThat(username.check("alice").isValid()).isTrue();

        // Fails minLength and alphanumeric ("a!") — notBlank passes
        Validated<NonEmptyList<String>, String> twoErrors = username.check("a!");
        assertThat(twoErrors.isValid()).isFalse();
        assertThat(twoErrors.getError().toList()).containsExactly("min 3 chars", "must be alphanumeric");

        // Blank string ("  ") fails all three
        Validated<NonEmptyList<String>, String> threeErrors = username.check("  ");
        assertThat(threeErrors.isValid()).isFalse();
        assertThat(threeErrors.getError().size()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // asPredicate
    // -------------------------------------------------------------------------

    @Test
    void asPredicate_returnsTrue_whenGuardPasses() {
        Guard<String> g = Guard.of(s -> !s.isBlank(), "must not be blank");
        assertThat(g.asPredicate().test("hello")).isTrue();
    }

    @Test
    void asPredicate_returnsFalse_whenGuardFails() {
        Guard<String> g = Guard.of(s -> !s.isBlank(), "must not be blank");
        assertThat(g.asPredicate().test("   ")).isFalse();
    }

    @Test
    void asPredicate_usefulForStreamFilter() {
        Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
        List<String> valid = List.of("alice", "  ", "bob", "").stream()
            .filter(notBlank.asPredicate())
            .toList();
        assertThat(valid).containsExactly("alice", "bob");
    }

    // -------------------------------------------------------------------------
    // contramap
    // -------------------------------------------------------------------------

    record User(String name) {}

    @Test
    void contramap_returnsValid_whenProjectedValuePasses() {
        Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
        Guard<User> userGuard = notBlank.contramap(User::name);
        Validated<NonEmptyList<String>, User> result = userGuard.check(new User("alice"));
        assertThat(result.isValid()).isTrue();
        assertThat(result.get().name()).isEqualTo("alice");
    }

    @Test
    void contramap_returnsInvalid_withOriginalErrors_whenProjectedValueFails() {
        Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "username must not be blank");
        Guard<User> userGuard = notBlank.contramap(User::name);
        Validated<NonEmptyList<String>, User> result = userGuard.check(new User("  "));
        assertThat(result.isValid()).isFalse();
        assertThat(result.getError().toList()).containsExactly("username must not be blank");
    }

    @Test
    void contramap_preservesOriginalInputOnSuccess() {
        Guard<Integer> positive = Guard.of(n -> n > 0, "must be positive");
        Guard<User> userGuard = positive.contramap(u -> u.name().length());
        User user = new User("hi");
        Validated<NonEmptyList<String>, User> result = userGuard.check(user);
        assertThat(result.isValid()).isTrue();
        assertThat(result.get()).isSameAs(user); // original U value preserved
    }

    @Test
    void contramap_shouldThrowNPE_whenMapperIsNull() {
        Guard<String> g = Guard.of(s -> true, "msg");
        assertThatThrownBy(() -> g.contramap(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    // -------------------------------------------------------------------------
    // checkToResult
    // -------------------------------------------------------------------------

    @Test
    void checkToResult_noMapper_returnsOk_whenGuardPasses() {
        Guard<String> g = Guard.of(s -> !s.isBlank(), "must not be blank");
        Result<String, NonEmptyList<String>> result = g.checkToResult("hello");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
    }

    @Test
    void checkToResult_noMapper_returnsErr_whenGuardFails() {
        Guard<String> g = Guard.of(s -> !s.isBlank(), "must not be blank");
        Result<String, NonEmptyList<String>> result = g.checkToResult("   ");
        assertThat(result.isError()).isTrue();
        assertThat(result.getError().toList()).containsExactly("must not be blank");
    }

    @Test
    void checkToResult_withMapper_returnsOk_whenGuardPasses() {
        Guard<String> g = Guard.of(s -> !s.isBlank(), "must not be blank");
        Result<String, String> result = g.checkToResult("hello", errors -> String.join(", ", errors.toList()));
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void checkToResult_withMapper_mapsErrors_whenGuardFails() {
        Guard<String> g = Guard.of(s -> !s.isBlank(), "must not be blank");
        Result<String, String> result = g.checkToResult("   ", errors -> String.join(", ", errors.toList()));
        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).isEqualTo("must not be blank");
    }

    @Test
    void checkToResult_withMapper_shouldThrowNPE_whenMapperIsNull() {
        Guard<String> g = Guard.of(s -> true, "msg");
        assertThatThrownBy(() -> g.checkToResult("x", (java.util.function.Function<NonEmptyList<String>, String>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("toError");
    }

    // -------------------------------------------------------------------------
    // checkToOption
    // -------------------------------------------------------------------------

    @Test
    void checkToOption_returnsSome_whenGuardPasses() {
        Guard<String> g = Guard.of(s -> !s.isBlank(), "must not be blank");
        Option<String> result = g.checkToOption("hello");
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get()).isEqualTo("hello");
    }

    @Test
    void checkToOption_returnsNone_whenGuardFails() {
        Guard<String> g = Guard.of(s -> !s.isBlank(), "must not be blank");
        Option<String> result = g.checkToOption("   ");
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void checkToOption_usefulForStreamFlatMap() {
        Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
        List<String> valid = List.of("alice", "  ", "bob").stream()
            .flatMap(s -> notBlank.checkToOption(s).stream())
            .toList();
        assertThat(valid).containsExactly("alice", "bob");
    }

    @Test
    void check_integratesWithValidatedCombine_toAccumulateAcrossFields() {
        Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "username must not be blank");
        Guard<String> email    = Guard.of(s -> s.contains("@"), "email must contain @");

        Validated<NonEmptyList<String>, String> username = notBlank.check("  ");
        Validated<NonEmptyList<String>, String> emailVal = email.check("not-an-email");

        Validated<NonEmptyList<String>, String> combined =
            username.combine(emailVal, NonEmptyList::concat, (u, e) -> u + "/" + e);

        assertThat(combined.isValid()).isFalse();
        assertThat(combined.getError().toList())
            .containsExactly("username must not be blank", "email must contain @");
    }
}
