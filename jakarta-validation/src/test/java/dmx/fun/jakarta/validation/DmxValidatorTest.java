package dmx.fun.jakarta.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DmxValidatorTest {

    record Item(@NotBlank String name, @Min(1) int quantity) {}

    private static Validator validator;

    @BeforeAll
    static void setup() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // ── validate ─────────────────────────────────────────────────────────────────

    @Test
    void validate_allConstraintsPass_returnsValid() {
        var result = DmxValidator
            .validate(validator, new Item("widget", 5));

        assertThat(result).isValid()
            .containsValue(new Item("widget", 5));
    }

    @Test
    void validate_singleViolation_returnsInvalidWithOneMessage() {
        var result = DmxValidator
            .validate(validator, new Item("", 5));

        assertThat(result)
            .isInvalid();
        assertThat(result.getError())
            .hasSize(1);
    }

    @Test
    void validate_multipleViolations_accumulatesAllMessages() {
        var result = DmxValidator
            .validate(validator, new Item("", 0));

        assertThat(result)
            .isInvalid();
        assertThat(result.getError())
            .hasSize(2);
    }

    @Test
    void validate_messagesAreSortedDeterministically() {
        var result = DmxValidator
            .validate(validator, new Item("", 0));
        var messages = result.getError()
            .stream()
            .toList();

        // "name:..." sorts before "quantity:..."
        assertThat(messages.get(0))
            .startsWith("name:");
        assertThat(messages.get(1))
            .startsWith("quantity:");
    }

    // ── validateRaw ──────────────────────────────────────────────────────────────

    @Test
    void validateRaw_allConstraintsPass_returnsValid() {
        var result = DmxValidator
            .validateRaw(validator, new Item("widget", 5));

        assertThat(result)
            .isValid()
            .containsValue(new Item("widget", 5));
    }

    @Test
    void validateRaw_singleViolation_returnsConstraintViolation() {
        var result = DmxValidator
            .validateRaw(validator, new Item("", 5));

        assertThat(result).isInvalid();
        var violations = result.getError();

        assertThat(violations)
            .hasSize(1);
        assertThat(
            violations.stream()
                .findFirst()
                .orElseThrow()
                .getPropertyPath()
                .toString()
        ).isEqualTo("name");
    }

    @Test
    void validateRaw_multipleViolations_returnsAllSortedByPropertyPath() {
        var result = DmxValidator.validateRaw(validator, new Item("", 0));
        var violations = result.getError().stream().toList();

        assertThat(violations).hasSize(2);
        assertThat(violations.get(0).getPropertyPath().toString()).isEqualTo("name");
        assertThat(violations.get(1).getPropertyPath().toString()).isEqualTo("quantity");
    }

    @Test
    void validateRaw_violation_preservesConstraintMetadata() {
        var result = DmxValidator.validateRaw(validator, new Item("widget", 0));

        assertThat(result).isInvalid();
        var violation = result.getError().stream().findFirst().orElseThrow();
        assertThat(violation.getInvalidValue()).isEqualTo(0);
        assertThat(violation.getPropertyPath().toString()).isEqualTo("quantity");
    }

    // ── null contracts ────────────────────────────────────────────────────────────

    @Test
    void validate_nullValidator_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DmxValidator.validate(null, new Item("ok", 1)));
    }

    @Test
    void validate_nullObject_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DmxValidator.validate(validator, null));
    }

    @Test
    void validateRaw_nullValidator_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DmxValidator.validateRaw(null, new Item("ok", 1)));
    }

    @Test
    void validateRaw_nullObject_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DmxValidator.validateRaw(validator, null));
    }
}
