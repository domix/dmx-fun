package dmx.fun.tutorial.chapter06;

import dmx.fun.Either;
import dmx.fun.Option;
import dmx.fun.tutorial.chapter06.Outcome.Enrolled;
import dmx.fun.tutorial.chapter06.Outcome.Waitlisted;
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

/** Chapter 6: a full course is a left, not an error; nothing is charged for a waiting-list spot. */
class EnrollmentServiceTest {

    private final List<String> charged = new ArrayList<>();
    private final PaymentGateway recordingGateway = (studentId, amount) -> charged.add(studentId);

    private static EnrollmentForm form(String studentId) {
        return new EnrollmentForm(studentId, "c1", studentId + "@example.com", "20", Option.none());
    }

    private EnrollmentService service() {
        var catalog = new Catalog()
            .addStudent(new Student("s1", "Ada"))
            .addStudent(new Student("s2", "Grace"))
            .addStudent(new Student("s3", "Linus"))
            .addCourse(new Course("c1", "Functional Java", 1, 10_000));
        return new EnrollmentService(catalog, recordingGateway, new WaitingList());
    }

    @Test
    void seatAvailable_isRight_andCharges() {
        var service = service();

        var result = service.enroll(form("s1"));

        assertThat(result).isOk();
        assertThat(result.get()).isRight().containsRight(new Enrolled(new Enrollment("s1", "c1")));
        assertThat(charged).containsExactly("s1");
    }

    @Test
    void courseFull_isLeft_withPosition_andNothingCharged() {
        var service = service();
        service.enroll(form("s1"));

        var second = service.enroll(form("s2"));
        var third = service.enroll(form("s3"));

        assertThat(second).isOk();
        assertThat(second.get()).isLeft().containsLeft(new Waitlisted("s2", "c1", 1));
        assertThat(third.get()).isLeft().containsLeft(new Waitlisted("s3", "c1", 2));
        assertThat(charged).containsExactly("s1");
    }

    @Test
    void describe_coversBothShapesOfSuccess() {
        var service = service();

        assertThat(EnrollmentMessages.describe(service.enroll(form("s1")))).isEqualTo("Enrolled s1 in c1");
        assertThat(EnrollmentMessages.describe(service.enroll(form("s2"))))
            .isEqualTo("s2 is number 1 on the waiting list for c1");
        assertThat(EnrollmentMessages.describe(service.enroll(form("ghost"))))
            .startsWith("Could not process the request: StudentNotFound");
    }

    @Test
    void eitherIsNeutral_bothSidesAreValues() {
        Either<Waitlisted, Enrolled> waitlisted = Either.left(new Waitlisted("s2", "c1", 1));

        // Nothing here is an error: fold treats both sides the same way.
        String text = waitlisted.fold(w -> "wait #" + w.position(), e -> "seat " + e.enrollment().courseId());
        assertThat(text).isEqualTo("wait #1");
    }
}
