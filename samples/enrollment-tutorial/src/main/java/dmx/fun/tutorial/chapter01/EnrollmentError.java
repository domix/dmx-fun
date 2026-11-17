package dmx.fun.tutorial.chapter01;

/**
 * Everything that can go wrong when enrolling, as data.
 *
 * <p>A sealed interface with one record per case. The compiler knows the full list, so a
 * {@code switch} over it must handle every case or it does not compile. Compare with chapter 0,
 * where the failure modes were three exception classes, a {@code null} and a wrapped cause
 * that nothing listed in one place.
 */
public sealed interface EnrollmentError {

    record StudentNotFound(String studentId) implements EnrollmentError {}

    record CourseNotFound(String courseId) implements EnrollmentError {}

    record CourseFull(String courseId, int capacity) implements EnrollmentError {}

    record AlreadyEnrolled(String studentId, String courseId) implements EnrollmentError {}

    record PaymentFailed(String reason) implements EnrollmentError {}
}
