package dmx.fun.tutorial.chapter04;

/** Everything that can go wrong when enrolling, as data (unchanged since chapter 1). */
public sealed interface EnrollmentError {

    record StudentNotFound(String studentId) implements EnrollmentError {}

    record CourseNotFound(String courseId) implements EnrollmentError {}

    record CourseFull(String courseId, int capacity) implements EnrollmentError {}

    record AlreadyEnrolled(String studentId, String courseId) implements EnrollmentError {}

    record PaymentFailed(String reason) implements EnrollmentError {}
}
