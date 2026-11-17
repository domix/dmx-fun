package dmx.fun.tutorial.chapter04;

import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.tutorial.chapter04.EnrollmentError.AlreadyEnrolled;
import dmx.fun.tutorial.chapter04.EnrollmentError.CourseFull;
import dmx.fun.tutorial.chapter04.EnrollmentError.CourseNotFound;
import dmx.fun.tutorial.chapter04.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.chapter04.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import java.util.Map;

/**
 * Chapter 4 — capture at the edge.
 *
 * <p>The payment gateway throws; that is how its API works and we do not own it. The last
 * {@code try/catch} in the service is replaced by {@code Try.run}, which captures whatever the
 * gateway throws, lets fatal errors through with {@code rethrowFatal}, and converts the rest to
 * our own {@code PaymentFailed} with {@code toResult}. The exception exists for exactly one line
 * and never crosses into the pipeline.
 */
public class EnrollmentService {

    /** Discount codes and the percentage they take off the fee. */
    private static final Map<String, Integer> DISCOUNTS = Map.of("EARLY10", 10, "ALUMNI25", 25);

    private final Catalog catalog;
    private final PaymentGateway payments;

    public EnrollmentService(Catalog catalog, PaymentGateway payments) {
        this.catalog = catalog;
        this.payments = payments;
    }

    public Result<Enrollment, EnrollmentError> enroll(String studentId, String courseId, Option<String> discountCode) {
        return findStudent(studentId)
            .flatMap(_ -> findCourse(courseId))
            .flatMap(course -> ensureNotEnrolled(studentId, course))
            .flatMap(course -> ensureSeatAvailable(course))
            .flatMap(course -> charge(studentId, priceFor(course, discountCode)))
            .map(_ -> catalog.save(new Enrollment(studentId, courseId)))
            .recoverWith(error -> switch (error) {
                case AlreadyEnrolled(var s, var c) -> Result.ok(new Enrollment(s, c));
                default -> Result.err(error);
            });
    }

    /** The fee, minus the discount if the code is present and known. Unknown codes are ignored. */
    int priceFor(Course course, Option<String> discountCode) {
        return discountCode
            .flatMap(code -> Option.ofNullable(DISCOUNTS.get(code)))
            .map(percent -> course.feeCents() - course.feeCents() * percent / 100)
            .getOrElse(course.feeCents());
    }

    private Result<Student, EnrollmentError> findStudent(String studentId) {
        return Option.ofNullable(catalog.findStudent(studentId))
            .toResult(new StudentNotFound(studentId));
    }

    private Result<Course, EnrollmentError> findCourse(String courseId) {
        return Option.ofNullable(catalog.findCourse(courseId))
            .toResult(new CourseNotFound(courseId));
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

    private Result<Integer, EnrollmentError> charge(String studentId, int amountCents) {
        return Try.run(() -> payments.charge(studentId, amountCents))
            .rethrowFatal()
            .map(_ -> amountCents)
            .toResult(e -> new PaymentFailed(e.getMessage()));
    }
}
