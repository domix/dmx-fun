package dmx.fun.tutorial.chapter06;

import dmx.fun.Either;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.tutorial.chapter06.EnrollmentError.AlreadyEnrolled;
import dmx.fun.tutorial.chapter06.EnrollmentError.CourseNotFound;
import dmx.fun.tutorial.chapter06.EnrollmentError.InvalidForm;
import dmx.fun.tutorial.chapter06.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.chapter06.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.chapter06.Outcome.Enrolled;
import dmx.fun.tutorial.chapter06.Outcome.Waitlisted;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import java.util.Map;

/**
 * Chapter 6 — two outcomes, no villain.
 *
 * <p>A full course used to be {@code CourseFull}, an error. Business says otherwise: the student
 * joins a waiting list and is told their position. Two legitimate answers, so the success value
 * becomes {@code Either<Waitlisted, Enrolled>}. The failure track is untouched; it only lost a
 * case that never belonged there.
 */
public class EnrollmentService {

    private static final Map<String, Integer> DISCOUNTS = Map.of("EARLY10", 10, "ALUMNI25", 25);

    private final Catalog catalog;
    private final PaymentGateway payments;
    private final WaitingList waitingList;

    public EnrollmentService(Catalog catalog, PaymentGateway payments, WaitingList waitingList) {
        this.catalog = catalog;
        this.payments = payments;
        this.waitingList = waitingList;
    }

    public Result<Either<Waitlisted, Enrolled>, EnrollmentError> enroll(EnrollmentForm form) {
        return validate(form)
            .flatMap(f -> findStudent(f.studentId()))
            .flatMap(_ -> findCourse(form.courseId()))
            .flatMap(course -> ensureNotEnrolled(form.studentId(), course))
            .flatMap(course -> seatOrWaitingList(form, course))
            .recoverWith(error -> switch (error) {
                case AlreadyEnrolled(var s, var c) -> Result.ok(Either.right(new Enrolled(new Enrollment(s, c))));
                default -> Result.err(error);
            });
    }

    /** Full course: left, nothing charged. Seat available: charge, save, right. */
    private Result<Either<Waitlisted, Enrolled>, EnrollmentError> seatOrWaitingList(EnrollmentForm form, Course course) {
        if (catalog.enrolledCount(course.id()) >= course.capacity()) {
            return Result.ok(Either.left(waitingList.add(form.studentId(), course.id())));
        }
        return charge(form.studentId(), priceFor(course, form.discountCode()))
            .map(_ -> catalog.save(new Enrollment(form.studentId(), course.id())))
            .map(enrollment -> Either.right(new Enrolled(enrollment)));
    }

    private Result<EnrollmentForm, EnrollmentError> validate(EnrollmentForm form) {
        return FormValidation.withGuards(form)
            .toResult()
            .<EnrollmentError>mapError(InvalidForm::new);
    }

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

    private Result<Integer, EnrollmentError> charge(String studentId, int amountCents) {
        return Try.run(() -> payments.charge(studentId, amountCents))
            .rethrowFatal()
            .map(_ -> amountCents)
            .toResult(e -> new PaymentFailed(e.getMessage()));
    }
}
