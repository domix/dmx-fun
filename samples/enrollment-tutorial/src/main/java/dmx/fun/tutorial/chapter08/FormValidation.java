package dmx.fun.tutorial.chapter08;

import dmx.fun.Guard;
import dmx.fun.NonEmptyList;
import dmx.fun.Validated;

/** Form rules as Guard values (unchanged since chapter 5; the by-hand version is dropped). */
public final class FormValidation {

    private FormValidation() {}

    // ── Second half: rules as values ──────────────────────────────────────────

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
}
