package dmx.fun.tutorial.chapter02;

import dmx.fun.tutorial.chapter02.EnrollmentError.CourseFull;
import dmx.fun.tutorial.chapter02.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.chapter02.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentException;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 2: same outcomes as chapter 1, but the pipeline stops at the first failure (the
 * gateway is never called when an earlier step fails) and enrolling twice is now a success.
 */
class EnrollmentServiceTest {

    private final List<String> charged = new ArrayList<>();
    private final PaymentGateway recordingGateway = (studentId, amount) -> charged.add(studentId);
    private final PaymentGateway failingGateway = (studentId, amount) -> {
        throw new PaymentException("card declined");
    };

    private Catalog catalog() {
        return new Catalog()
            .addStudent(new Student("s1", "Ada"))
            .addCourse(new Course("c1", "Functional Java", 1, 10_000));
    }

    @Test
    void happyPath_isOk_andChargesOnce() {
        var service = new EnrollmentService(catalog(), recordingGateway);

        assertThat(service.enroll("s1", "c1")).isOk().containsValue(new Enrollment("s1", "c1"));
        assertThat(charged).containsExactly("s1");
    }

    @Test
    void firstFailureWins_laterStepsNeverRun() {
        var service = new EnrollmentService(catalog(), recordingGateway);

        assertThat(service.enroll("ghost", "c1")).isErr().containsError(new StudentNotFound("ghost"));
        assertThat(charged).isEmpty();
    }

    @Test
    void enrollingTwice_isIdempotent_notAnError() {
        var service = new EnrollmentService(catalog(), recordingGateway);
        service.enroll("s1", "c1");

        assertThat(service.enroll("s1", "c1")).isOk().containsValue(new Enrollment("s1", "c1"));
        assertThat(charged).containsExactly("s1"); // the second call did not charge again
    }

    @Test
    void fullCourse_isErr() {
        var catalog = catalog().addStudent(new Student("s2", "Grace"));
        var service = new EnrollmentService(catalog, recordingGateway);
        service.enroll("s1", "c1");

        assertThat(service.enroll("s2", "c1")).isErr().containsError(new CourseFull("c1", 1));
    }

    @Test
    void paymentProblem_isErr_andNothingIsSaved() {
        var catalog = catalog();
        var service = new EnrollmentService(catalog, failingGateway);

        assertThat(service.enroll("s1", "c1")).isErr().containsError(new PaymentFailed("card declined"));
        assertThat(catalog.isEnrolled("s1", "c1")).isFalse();
    }
}
