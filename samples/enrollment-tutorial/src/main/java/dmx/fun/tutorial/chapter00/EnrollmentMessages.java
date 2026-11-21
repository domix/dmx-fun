package dmx.fun.tutorial.chapter00;

import dmx.fun.tutorial.domain.Enrollment;

/**
 * Chapter 0 from the caller's seat: a message for every way {@code enroll} can end.
 *
 * <p>Nothing in the signature says which exceptions to catch, that {@code null} means "already
 * enrolled", or that a payment problem arrives as a bare {@code RuntimeException} with the real
 * cause inside. The caller has to know all of it, and nothing checks that it does.
 */
public final class EnrollmentMessages {

    private EnrollmentMessages() {
    }

    // #region describe
    public static String describe(EnrollmentService service, String studentId, String courseId) {
        try {
            Enrollment e = service.enroll(studentId, courseId);
            if (e == null) {                           // "already enrolled", if you knew to check
                return studentId + " is already enrolled in " + courseId;
            }
            return "Enrolled " + e.studentId() + " in " + e.courseId();
        } catch (StudentNotFoundException | CourseNotFoundException ex) {
            return ex.getMessage();                    // if you knew these two existed
        } catch (CourseFullException ex) {
            return "Course " + courseId + " is full";
        } catch (RuntimeException ex) {                // payment failed, or anything else at all
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            return "Payment failed: " + cause.getMessage();
        }
    }
    // #endregion describe
}
