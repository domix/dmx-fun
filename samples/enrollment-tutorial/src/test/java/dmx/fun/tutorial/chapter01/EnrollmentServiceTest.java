package dmx.fun.tutorial.chapter01;

import dmx.fun.tutorial.chapter01.EnrollmentError.AlreadyEnrolled;
import dmx.fun.tutorial.chapter01.EnrollmentError.CourseFull;
import dmx.fun.tutorial.chapter01.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.chapter01.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.PaymentException;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 1 from the caller's seat: every outcome is a value, and {@link EnrollmentMessages}
 * turns each one into text with a single exhaustive switch.
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
    void happyPath_isOk() {
        var service = new EnrollmentService(catalog(), acceptingGateway);

        var result = service.enroll("s1", "c1");

        assertThat(result).isOk();
        assertThat(EnrollmentMessages.describe(result)).isEqualTo("Enrolled s1 in c1");
    }

    @Test
    void unknownStudent_isErr() {
        var service = new EnrollmentService(catalog(), acceptingGateway);

        var result = service.enroll("ghost", "c1");

        assertThat(result).isErr().containsError(new StudentNotFound("ghost"));
        assertThat(EnrollmentMessages.describe(result)).isEqualTo("We have no student with id ghost");
    }

    @Test
    void alreadyEnrolled_isErr_notNull() {
        var service = new EnrollmentService(catalog(), acceptingGateway);
        service.enroll("s1", "c1");

        var result = service.enroll("s1", "c1");

        assertThat(result).isErr().containsError(new AlreadyEnrolled("s1", "c1"));
    }

    @Test
    void fullCourse_isErr_withCapacity() {
        var catalog = catalog().addStudent(new Student("s2", "Grace"));
        var service = new EnrollmentService(catalog, acceptingGateway);
        service.enroll("s1", "c1");

        var result = service.enroll("s2", "c1");

        assertThat(result).isErr().containsError(new CourseFull("c1", 1));
        assertThat(EnrollmentMessages.describe(result)).isEqualTo("Course c1 is full (1 seats)");
    }

    @Test
    void paymentProblem_isErr_withTheRealReason() {
        var service = new EnrollmentService(catalog(), failingGateway);

        var result = service.enroll("s1", "c1");

        assertThat(result).isErr().containsError(new PaymentFailed("card declined"));
    }
}
