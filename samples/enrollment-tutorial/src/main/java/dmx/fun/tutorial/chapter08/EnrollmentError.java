package dmx.fun.tutorial.chapter08;

import dmx.fun.NonEmptyList;

/**
 * Everything that can go wrong when enrolling, as data.
 *
 * <p>Unchanged since chapter 6.
 */
public sealed interface EnrollmentError {

    record StudentNotFound(String studentId) implements EnrollmentError {}

    record CourseNotFound(String courseId) implements EnrollmentError {}

    record AlreadyEnrolled(String studentId, String courseId) implements EnrollmentError {}

    record PaymentFailed(String reason) implements EnrollmentError {}

    record InvalidForm(NonEmptyList<String> errors) implements EnrollmentError {}
}
