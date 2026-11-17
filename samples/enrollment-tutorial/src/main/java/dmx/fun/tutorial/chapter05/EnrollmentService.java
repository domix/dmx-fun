package dmx.fun.tutorial.chapter05;

import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.tutorial.chapter05.EnrollmentError.AlreadyEnrolled;
import dmx.fun.tutorial.chapter05.EnrollmentError.CourseFull;
import dmx.fun.tutorial.chapter05.EnrollmentError.InvalidForm;
import dmx.fun.tutorial.chapter05.EnrollmentError.CourseNotFound;
import dmx.fun.tutorial.chapter05.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.chapter05.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import java.util.Map;

/**
 * Chapter 5 — all the errors at once.
 *
 * <p>{@code enroll} now takes the raw {@link EnrollmentForm}. The form is validated first, with
 * every rule applied and every failure collected, and the result enters the pipeline as a
 * {@code Result}: a bad form becomes one {@code InvalidForm} error carrying all the messages.
 * Input errors are messages for the user; everything after the form is a typed domain error.
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

    public Result<Enrollment, EnrollmentError> enroll(EnrollmentForm form) {
        return validate(form)
            .flatMap(f -> findStudent(f.studentId()))
            .flatMap(_ -> findCourse(form.courseId()))
            .flatMap(course -> ensureNotEnrolled(form.studentId(), course))
            .flatMap(course -> ensureSeatAvailable(course))
            .flatMap(course -> charge(form.studentId(), priceFor(course, form.discountCode())))
            .map(_ -> catalog.save(new Enrollment(form.studentId(), form.courseId())))
            .recoverWith(error -> switch (error) {
                case AlreadyEnrolled(var s, var c) -> Result.ok(new Enrollment(s, c));
                default -> Result.err(error);
            });
    }

    /** The bridge: all form errors, as one {@code EnrollmentError}, on the failure track. */
    // #region validate
    private Result<EnrollmentForm, EnrollmentError> validate(EnrollmentForm form) {
        return FormValidation.withGuards(form)
            .toResult()
            .<EnrollmentError>mapError(InvalidForm::new);
    }
    // #endregion validate

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
