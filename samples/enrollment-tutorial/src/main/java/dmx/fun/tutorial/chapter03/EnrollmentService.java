package dmx.fun.tutorial.chapter03;

import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.tutorial.chapter03.EnrollmentError.AlreadyEnrolled;
import dmx.fun.tutorial.chapter03.EnrollmentError.CourseFull;
import dmx.fun.tutorial.chapter03.EnrollmentError.CourseNotFound;
import dmx.fun.tutorial.chapter03.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.chapter03.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentException;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import java.util.Map;

/**
 * Chapter 3 — absence is not failure.
 *
 * <p>Two things may legitimately be missing here and neither is an error. A discount code: most
 * students do not have one, so {@code enroll} takes an {@code Option<String>} and the price is
 * computed through it. And the {@code Catalog} lookups: the store returns {@code null} for a
 * missing row, so each lookup is wrapped in {@code Option} right where it happens and turned into
 * the matching {@code EnrollmentError} with {@code toResult}. The {@code == null} checks from
 * chapter 2 are gone.
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
        try {
            payments.charge(studentId, amountCents);
            return Result.ok(amountCents);
        } catch (PaymentException e) {
            return Result.err(new PaymentFailed(e.getMessage())); // chapter 4 removes this try/catch
        }
    }
}
