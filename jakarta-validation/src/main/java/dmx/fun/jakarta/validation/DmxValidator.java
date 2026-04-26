package dmx.fun.jakarta.validation;

import dmx.fun.NonEmptyList;
import dmx.fun.Validated;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * dmx-fun adapter for Jakarta Bean Validation.
 *
 * <p>Validates objects using a {@link Validator} and returns a dmx-fun
 * {@link Validated} type instead of throwing {@code ConstraintViolationException}.
 * Violations accumulate into a {@link NonEmptyList}, preserving all constraint
 * failures for the caller to inspect.
 *
 * <pre>{@code
 * Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
 *
 * // Messages only — "propertyPath: message" strings, sorted for determinism
 * Validated<NonEmptyList<String>, CreateItemRequest> r1 =
 *     DmxValidator.validate(validator, request);
 *
 * // Full ConstraintViolation detail
 * Validated<NonEmptyList<ConstraintViolation<CreateItemRequest>>, CreateItemRequest> r2 =
 *     DmxValidator.validateRaw(validator, request);
 * }</pre>
 */
@NullMarked
public final class DmxValidator {

    private DmxValidator() {}

    /**
     * Validates {@code object} and returns violation messages as a
     * {@link NonEmptyList NonEmptyList&lt;String&gt;}.
     *
     * <p>Each message has the form {@code "propertyPath: constraintMessage"}.
     * Messages are sorted alphabetically for deterministic ordering across JVM runs.
     *
     * @param <T>       the type of the object being validated
     * @param validator the Jakarta Validation {@link Validator} to use
     * @param object    the object to validate
     * @param groups    the validation groups to apply; empty means the default group
     * @return {@code Valid(object)} when all constraints pass,
     *         {@code Invalid(NonEmptyList.of(messages))} when at least one is violated
     * @throws NullPointerException if {@code validator} or {@code object} is {@code null}
     */
    public static <T> Validated<NonEmptyList<String>, T> validate(
            Validator validator,
            T object,
            Class<?>... groups
    ) {
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(object, "object");

        var violations = validator.validate(object, groups);

        if (violations.isEmpty()) {
            return Validated.valid(object);
        }

        var messages = violations.stream()
            .map(v -> "%s: %s".formatted(v.getPropertyPath(), v.getMessage()))
            .sorted()
            .toList();

        return Validated.invalid(
            NonEmptyList.of(
                messages.getFirst(),
                messages.subList(1, messages.size())
            )
        );
    }

    /**
     * Validates {@code object} and returns the raw {@link ConstraintViolation} set
     * as a {@link NonEmptyList}.
     *
     * <p>Violations are sorted by property path for deterministic ordering.
     * Use this method when the caller needs to inspect constraint metadata
     * (annotation type, interpolated message template, leaf bean, etc.).
     *
     * @param <T>       the type of the object being validated
     * @param validator the Jakarta Validation {@link Validator} to use
     * @param object    the object to validate
     * @param groups    the validation groups to apply; empty means the default group
     * @return {@code Valid(object)} when all constraints pass,
     *         {@code Invalid(NonEmptyList.of(violations))} when at least one is violated
     * @throws NullPointerException if {@code validator} or {@code object} is {@code null}
     */
    public static <T> Validated<NonEmptyList<ConstraintViolation<T>>, T> validateRaw(
            Validator validator,
            T object,
            Class<?>... groups
    ) {
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(object, "object");

        var violations = validator.validate(object, groups);

        if (violations.isEmpty()) {
            return Validated.valid(object);
        }

        var sorted = violations.stream()
            .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
            .toList();

        return Validated.invalid(
            NonEmptyList.of(
                sorted.getFirst(),
                sorted.subList(1, sorted.size())
            )
        );
    }
}
