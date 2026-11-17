package dmx.fun.tutorial.chapter05;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.tutorial.chapter05.EnrollmentError.InvalidForm;
import dmx.fun.tutorial.chapter05.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 5: a bad form is one {@code InvalidForm} carrying every message, and it never reaches
 * the catalog or the gateway. A good form goes through the chapter 4 pipeline unchanged.
 */
class EnrollmentServiceTest {

    private final List<Integer> amounts = new ArrayList<>();
    private final PaymentGateway recordingGateway = (studentId, amount) -> amounts.add(amount);

    private Catalog catalog() {
        return new Catalog()
            .addStudent(new Student("s1", "Ada"))
            .addCourse(new Course("c1", "Functional Java", 5, 10_000));
    }

    @Test
    void invalidForm_isOneError_withAllMessages_andStopsEverything() {
        var service = new EnrollmentService(catalog(), recordingGateway);
        var form = new EnrollmentForm("", "c1", "ada-at-example.com", "fifteen", Option.none());

        assertThat(service.enroll(form)).isErr().containsError(new InvalidForm(NonEmptyList.of(
            "studentId: must not be blank",
            List.of("email: must contain @", "age: must be a number, 16 or more"))));
        assertThat(amounts).isEmpty();
    }

    @Test
    void validForm_goesThroughThePipeline() {
        var service = new EnrollmentService(catalog(), recordingGateway);
        var form = new EnrollmentForm("s1", "c1", "ada@example.com", "21", Option.some("EARLY10"));

        assertThat(service.enroll(form)).isOk().containsValue(new Enrollment("s1", "c1"));
        assertThat(amounts).containsExactly(9_000);
    }

    @Test
    void domainErrors_areStillTyped_notMessages() {
        var service = new EnrollmentService(catalog(), recordingGateway);
        var form = new EnrollmentForm("ghost", "c1", "ghost@example.com", "30", Option.none());

        assertThat(service.enroll(form)).isErr().containsError(new StudentNotFound("ghost"));
    }
}
