package dmx.fun.tutorial.chapter05;

import dmx.fun.Guard;
import dmx.fun.NonEmptyList;
import dmx.fun.Validated;

/**
 * Chapter 5 — all the errors at once. Two ways to validate the same form.
 *
 * <p>{@link #byHand} builds one {@code Validated} per field and merges them, so a form with three
 * problems reports three messages. {@link #withGuards} says the same thing with rules as values:
 * each rule is a {@code Guard}, written once, composed with {@code allOf}, and reused anywhere.
 */
public final class FormValidation {

    private FormValidation() {}

    // ── First half: by hand ───────────────────────────────────────────────────

    public static Validated<NonEmptyList<String>, EnrollmentForm> byHand(EnrollmentForm form) {
        Validated<NonEmptyList<String>, String> studentId = form.studentId().isBlank()
            ? Validated.invalidNel("studentId: must not be blank")
            : Validated.valid(form.studentId());
        Validated<NonEmptyList<String>, String> email = form.email().contains("@")
            ? Validated.valid(form.email())
            : Validated.invalidNel("email: must contain @");
        Validated<NonEmptyList<String>, String> age = isAtLeast16(form.age())
            ? Validated.valid(form.age())
            : Validated.invalidNel("age: must be a number, 16 or more");

        return studentId
            .combine(email, NonEmptyList::concat, (_, _) -> form)
            .combine(age, NonEmptyList::concat, (_, _) -> form);
    }

    private static boolean isAtLeast16(String age) {
        try {
            return Integer.parseInt(age) >= 16;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ── Second half: rules as values ──────────────────────────────────────────

    // #region guards
    static final Guard<String> NOT_BLANK = Guard.of(s -> !s.isBlank(), "must not be blank");
    static final Guard<String> EMAIL = Guard.of(s -> s.contains("@"), "must contain @");
    static final Guard<String> AGE_AT_LEAST_16 =
        Guard.ofCatching(s -> Integer.parseInt(s) >= 16, "must be a number, 16 or more");

    static final Guard<EnrollmentForm> FORM = Guard.allOf(
        NOT_BLANK.contramap(EnrollmentForm::studentId, "studentId"),
        NOT_BLANK.contramap(EnrollmentForm::courseId, "courseId"),
        EMAIL.contramap(EnrollmentForm::email, "email"),
        AGE_AT_LEAST_16.contramap(EnrollmentForm::age, "age")
    );

    public static Validated<NonEmptyList<String>, EnrollmentForm> withGuards(EnrollmentForm form) {
        return FORM.check(form);
    }
    // #endregion guards
}
