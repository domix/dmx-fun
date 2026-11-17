package dmx.fun.tutorial.chapter00;

import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.PaymentException;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chapter 0 from the caller's seat: four different ways to learn that something went wrong,
 * none of them visible in the method signature.
 */
class EnrollmentServiceTest {

    private final PaymentGateway acceptingGateway = (studentId, amount) -> {};
    private final PaymentGateway failingGateway = (studentId, amount) -> {
        throw new PaymentException("card declined");
    };

    private Catalog catalog() {
        return new Catalog()
            .addStudent(new Student("s1", "Ada"))
            .addCourse(new Course("c1", "Functional Java", 1, 10_000));
    }

    @Test
    void happyPath_returnsEnrollment() {
        var service = new EnrollmentService(catalog(), acceptingGateway);

        var enrollment = service.enroll("s1", "c1");

        assertThat(enrollment).isNotNull();
        assertThat(enrollment.courseId()).isEqualTo("c1");
    }

    @Test
    void unknownStudent_throws() {
        var service = new EnrollmentService(catalog(), acceptingGateway);

        assertThatThrownBy(() -> service.enroll("ghost", "c1"))
            .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void alreadyEnrolled_returnsNull_whichCallersMustRememberToCheck() {
        var service = new EnrollmentService(catalog(), acceptingGateway);
        service.enroll("s1", "c1");

        assertThat(service.enroll("s1", "c1")).isNull();
    }

    @Test
    void fullCourse_throws() {
        var catalog = catalog().addStudent(new Student("s2", "Grace"));
        var service = new EnrollmentService(catalog, acceptingGateway);
        service.enroll("s1", "c1");

        assertThatThrownBy(() -> service.enroll("s2", "c1"))
            .isInstanceOf(CourseFullException.class);
    }

    @Test
    void paymentProblem_surfacesAsGenericRuntimeException() {
        var service = new EnrollmentService(catalog(), failingGateway);

        assertThatThrownBy(() -> service.enroll("s1", "c1"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("payment failed")
            .cause().isInstanceOf(PaymentException.class);
    }
}
