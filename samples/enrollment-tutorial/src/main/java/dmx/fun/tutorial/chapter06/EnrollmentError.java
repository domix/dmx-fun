package dmx.fun.tutorial.chapter06;

import dmx.fun.NonEmptyList;

/**
 * Everything that can go wrong when enrolling, as data.
 *
 * <p>Chapter 6 removes {@code CourseFull}: a full course is no longer a failure, it is the
 * waiting list, and it lives in the success value as {@code Either<Waitlisted, Enrolled>}.
 */
public sealed interface EnrollmentError {

    record StudentNotFound(String studentId) implements EnrollmentError {}

    record CourseNotFound(String courseId) implements EnrollmentError {}

    record AlreadyEnrolled(String studentId, String courseId) implements EnrollmentError {}

    record PaymentFailed(String reason) implements EnrollmentError {}

    record InvalidForm(NonEmptyList<String> errors) implements EnrollmentError {}
}
