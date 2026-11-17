package dmx.fun.tutorial.chapter00;

import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import org.jspecify.annotations.Nullable;

/**
 * Chapter 0 — the starting point: {@code enroll} written the way most Java code is written.
 *
 * <p>Read the signature: {@code Enrollment enroll(String, String)}. It promises an
 * {@code Enrollment}. It does not say that it may throw three different exceptions, that it
 * returns {@code null} when the student is already enrolled, or that a payment problem comes
 * back as a {@code RuntimeException} with the real cause buried inside.
 */
public class EnrollmentService {

    private final Catalog catalog;
    private final PaymentGateway payments;

    public EnrollmentService(Catalog catalog, PaymentGateway payments) {
        this.catalog = catalog;
        this.payments = payments;
    }

    public @Nullable Enrollment enroll(String studentId, String courseId) {
        Student student = catalog.findStudent(studentId);
        if (student == null) {
            throw new StudentNotFoundException(studentId);
        }
        Course course = catalog.findCourse(courseId);
        if (course == null) {
            throw new CourseNotFoundException(courseId);
        }
        if (catalog.isEnrolled(studentId, courseId)) {
            return null; // "already enrolled" — every caller has to remember to check for this
        }
        if (catalog.enrolledCount(courseId) >= course.capacity()) {
            throw new CourseFullException(courseId);
        }
        try {
            payments.charge(studentId, course.feeCents());
        } catch (Exception e) {
            throw new RuntimeException("payment failed", e);
        }
        return catalog.save(new Enrollment(studentId, courseId));
    }
}
