package dmx.fun.tutorial.chapter02;

import dmx.fun.Result;
import dmx.fun.tutorial.chapter02.EnrollmentError.AlreadyEnrolled;
import dmx.fun.tutorial.chapter02.EnrollmentError.CourseFull;
import dmx.fun.tutorial.chapter02.EnrollmentError.CourseNotFound;
import dmx.fun.tutorial.chapter02.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.chapter02.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentException;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;

/**
 * Chapter 2 — two tracks.
 *
 * <p>{@code enroll} is now a chain of small steps. Each step returns a {@code Result}; while
 * they succeed, the value moves to the next step; the first failure skips everything after it.
 * Nobody checks {@code isOk()} in between. At the end, {@code recoverWith} turns one specific
 * failure, {@code AlreadyEnrolled}, back into a success, because enrolling twice is not an error
 * we want callers to handle.
 */
public class EnrollmentService {

    private final Catalog catalog;
    private final PaymentGateway payments;

    public EnrollmentService(Catalog catalog, PaymentGateway payments) {
        this.catalog = catalog;
        this.payments = payments;
    }

    public Result<Enrollment, EnrollmentError> enroll(String studentId, String courseId) {
        return findStudent(studentId)
            .flatMap(_ -> findCourse(courseId))
            .flatMap(course -> ensureNotEnrolled(studentId, course))
            .flatMap(course -> ensureSeatAvailable(course))
            .flatMap(course -> charge(studentId, course))
            .map(_ -> catalog.save(new Enrollment(studentId, courseId)))
            .recoverWith(error -> switch (error) {
                case AlreadyEnrolled(var s, var c) -> Result.ok(new Enrollment(s, c));
                default -> Result.err(error);
            });
    }

    private Result<Student, EnrollmentError> findStudent(String studentId) {
        Student student = catalog.findStudent(studentId);
        return student == null ? Result.err(new StudentNotFound(studentId)) : Result.ok(student);
    }

    private Result<Course, EnrollmentError> findCourse(String courseId) {
        Course course = catalog.findCourse(courseId);
        return course == null ? Result.err(new CourseNotFound(courseId)) : Result.ok(course);
    }

    private Result<Course, EnrollmentError> ensureNotEnrolled(String studentId, Course course) {
        return catalog.isEnrolled(studentId, course.id())
            ? Result.err(new AlreadyEnrolled(studentId, course.id()))
            : Result.ok(course);
    }

    private Result<Course, EnrollmentError> ensureSeatAvailable(Course course) {
        return catalog.enrolledCount(course.id()) >= course.capacity()
            ? Result.err(new CourseFull(course.id(), course.capacity()))
            : Result.ok(course);
    }

    private Result<Course, EnrollmentError> charge(String studentId, Course course) {
        try {
            payments.charge(studentId, course.feeCents());
            return Result.ok(course);
        } catch (PaymentException e) {
            return Result.err(new PaymentFailed(e.getMessage())); // chapter 4 removes this try/catch
        }
    }
}
