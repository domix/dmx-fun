package dmx.fun.tutorial.chapter05;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 5: three problems in the form, three messages in the answer, in both styles. */
class FormValidationTest {

    private static final EnrollmentForm BAD = new EnrollmentForm("", "c1", "ada-at-example.com", "fifteen", Option.none());
    private static final EnrollmentForm GOOD = new EnrollmentForm("s1", "c1", "ada@example.com", "21", Option.none());

    @Test
    void byHand_reportsEveryError() {
        var result = FormValidation.byHand(BAD);

        assertThat(result).isInvalid();
        assertThat(result.getError().toList()).containsExactly(
            "studentId: must not be blank",
            "email: must contain @",
            "age: must be a number, 16 or more");
    }

    @Test
    void withGuards_reportsEveryError_sameMessages() {
        var result = FormValidation.withGuards(BAD);

        assertThat(result).isInvalid();
        assertThat(result.getError().toList()).containsExactly(
            "studentId: must not be blank",
            "email: must contain @",
            "age: must be a number, 16 or more");
    }

    @Test
    void validForm_passesThroughUnchanged() {
        assertThat(FormValidation.byHand(GOOD)).isValid().containsValue(GOOD);
        assertThat(FormValidation.withGuards(GOOD)).isValid().containsValue(GOOD);
    }

    @Test
    void guardsAreValues_reusableOnTheirOwn() {
        assertThat(FormValidation.EMAIL.check("no-at-sign")).isInvalid()
            .hasError(NonEmptyList.singleton("must contain @"));
        assertThat(FormValidation.AGE_AT_LEAST_16.check("abc")).isInvalid(); // parseInt threw; folded into Invalid
        assertThat(FormValidation.AGE_AT_LEAST_16.check("16")).isValid();
    }
}
