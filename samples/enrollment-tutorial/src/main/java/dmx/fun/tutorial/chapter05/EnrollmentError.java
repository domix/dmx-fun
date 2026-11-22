package dmx.fun.tutorial.chapter05;

import dmx.fun.NonEmptyList;

/** Everything that can go wrong when enrolling, as data. Chapter 5 adds {@link InvalidForm}. */
public sealed interface EnrollmentError {

    record StudentNotFound(String studentId) implements EnrollmentError {}

    record CourseNotFound(String courseId) implements EnrollmentError {}

    record CourseFull(String courseId, int capacity) implements EnrollmentError {}

    record AlreadyEnrolled(String studentId, String courseId) implements EnrollmentError {}

    record PaymentFailed(String reason) implements EnrollmentError {}

    /** The form had problems; all of them, not just the first. Messages are for the user. */
    record InvalidForm(NonEmptyList<String> errors) implements EnrollmentError {}
}
