package dmx.fun.tutorial.chapter01;

import dmx.fun.Result;
import dmx.fun.tutorial.chapter01.EnrollmentError.AlreadyEnrolled;
import dmx.fun.tutorial.chapter01.EnrollmentError.CourseFull;
import dmx.fun.tutorial.chapter01.EnrollmentError.CourseNotFound;
import dmx.fun.tutorial.chapter01.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.chapter01.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentException;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;

/**
 * Chapter 1 — return instead of throw.
 *
 * <p>The signature now tells the truth: {@code Result<Enrollment, EnrollmentError>}. Either you
 * get an {@code Enrollment} or you get one of the five {@link EnrollmentError} cases, and the
 * compiler makes callers deal with both. The body is still a list of checks with early returns;
 * chapter 2 cleans that up.
 */
public class EnrollmentService {

    private final Catalog catalog;
    private final PaymentGateway payments;

    public EnrollmentService(Catalog catalog, PaymentGateway payments) {
        this.catalog = catalog;
        this.payments = payments;
    }

    // #region signature
    public Result<Enrollment, EnrollmentError> enroll(String studentId, String courseId) {
        Student student = catalog.findStudent(studentId);
        if (student == null) {
            return Result.err(new StudentNotFound(studentId));
        }
    // #endregion signature
        Course course = catalog.findCourse(courseId);
        if (course == null) {
            return Result.err(new CourseNotFound(courseId));
        }
        if (catalog.isEnrolled(studentId, courseId)) {
            return Result.err(new AlreadyEnrolled(studentId, courseId));
        }
        if (catalog.enrolledCount(courseId) >= course.capacity()) {
            return Result.err(new CourseFull(courseId, course.capacity()));
        }
        try {
            payments.charge(studentId, course.feeCents());
        } catch (PaymentException e) {
            // Still a try/catch, but now the failure becomes a value. Chapter 4 does this properly.
            return Result.err(new PaymentFailed(e.getMessage()));
        }
        return Result.ok(catalog.save(new Enrollment(studentId, courseId)));
    }
}
