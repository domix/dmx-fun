package dmx.fun.tutorial.chapter08;

import dmx.fun.Either;
import dmx.fun.Lazy;
import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.tutorial.chapter08.EnrollmentError.AlreadyEnrolled;
import dmx.fun.tutorial.chapter08.EnrollmentError.CourseNotFound;
import dmx.fun.tutorial.chapter08.EnrollmentError.InvalidForm;
import dmx.fun.tutorial.chapter08.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.chapter08.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.chapter08.Outcome.Enrolled;
import dmx.fun.tutorial.chapter08.Outcome.Waitlisted;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import java.util.Map;

/**
 * Chapter 8 — laziness and resources.
 *
 * <p>The price depends on the course and the discount code and, in a real system, on a pricing
 * service that is slow to ask. A student who ends up on the waiting list is never charged, so
 * there is no reason to compute their price. {@code Lazy} describes the computation without
 * running it; only the seat-available branch calls {@code get()}. Exporting the roster to a
 * file lives in {@link RosterExport}.
 */
public class EnrollmentService {

    private static final Map<String, Integer> DISCOUNTS = Map.of("EARLY10", 10, "ALUMNI25", 25);

    private final Catalog catalog;
    private final PaymentGateway payments;
    private final WaitingList waitingList;
    private final Map<String, NonEmptyList<Schedule>> schedules;
    int priceComputations; // visible to the tests, which count how often the price was computed

    public EnrollmentService(Catalog catalog, PaymentGateway payments, WaitingList waitingList,
                             Map<String, NonEmptyList<Schedule>> schedules) {
        this.catalog = catalog;
        this.payments = payments;
        this.waitingList = waitingList;
        this.schedules = schedules;
    }

    public Result<Either<Waitlisted, Enrolled>, EnrollmentError> enroll(EnrollmentForm form) {
        return validate(form)
            .flatMap(f -> findStudent(f.studentId()))
            .flatMap(_ -> findOffering(form.courseId()))
            .flatMap(offering -> ensureNotEnrolled(form.studentId(), offering))
            .flatMap(offering -> seatOrWaitingList(form, offering,
                Lazy.of(() -> priceFor(offering.course(), form.discountCode()))))
            .recoverWith(error -> switch (error) {
                case AlreadyEnrolled(var s, var c) -> findOffering(c)
                    .map(offering -> Either.right(new Enrolled(new Enrollment(s, c), offering.schedules().head())));
                default -> Result.err(error);
            });
    }

    /** Everyone enrolled so far, or {@code None} if nobody is. Callers of {@code Some} get a list with a head. */
    public Option<NonEmptyList<Enrollment>> roster(String courseId) {
        return NonEmptyList.fromList(catalog.enrollmentsFor(courseId));
    }

    /** A course plus its schedules; a course without schedules is not offered, so it is "not found". */
    private Result<Offering, EnrollmentError> findOffering(String courseId) {
        return Option.ofNullable(catalog.findCourse(courseId))
            .flatMap(course -> Option.ofNullable(schedules.get(courseId)).map(s -> new Offering(course, s)))
            .toResult(new CourseNotFound(courseId));
    }

    /** The price is a description until a seat is confirmed; the waiting-list branch never evaluates it. */
    private Result<Either<Waitlisted, Enrolled>, EnrollmentError> seatOrWaitingList(
            EnrollmentForm form, Offering offering, Lazy<Integer> price) {
        Course course = offering.course();
        if (catalog.enrolledCount(course.id()) >= course.capacity()) {
            return Result.ok(Either.left(waitingList.add(form.studentId(), course.id())));
        }
        return charge(form.studentId(), price.get())
            .map(_ -> catalog.save(new Enrollment(form.studentId(), course.id())))
            .map(enrollment -> Either.right(new Enrolled(enrollment, offering.schedules().head())));
    }

    private Result<EnrollmentForm, EnrollmentError> validate(EnrollmentForm form) {
        return FormValidation.withGuards(form)
            .toResult()
            .<EnrollmentError>mapError(InvalidForm::new);
    }

    /** Imagine a call to a pricing service here: this is the work {@code Lazy} lets us skip. */
    int priceFor(Course course, Option<String> discountCode) {
        priceComputations++;
        return discountCode
            .flatMap(code -> Option.ofNullable(DISCOUNTS.get(code)))
            .map(percent -> course.feeCents() - course.feeCents() * percent / 100)
            .getOrElse(course.feeCents());
    }

    private Result<Student, EnrollmentError> findStudent(String studentId) {
        return Option.ofNullable(catalog.findStudent(studentId))
            .toResult(new StudentNotFound(studentId));
    }

    private Result<Offering, EnrollmentError> ensureNotEnrolled(String studentId, Offering offering) {
        return catalog.isEnrolled(studentId, offering.course().id())
            ? Result.err(new AlreadyEnrolled(studentId, offering.course().id()))
            : Result.ok(offering);
    }

    private Result<Integer, EnrollmentError> charge(String studentId, int amountCents) {
        return Try.run(() -> payments.charge(studentId, amountCents))
            .rethrowFatal()
            .map(_ -> amountCents)
            .toResult(e -> new PaymentFailed(e.getMessage()));
    }
}
