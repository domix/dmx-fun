package dmx.fun.tutorial.chapter07;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.tutorial.chapter07.EnrollmentError.CourseNotFound;
import dmx.fun.tutorial.chapter07.Outcome.Enrolled;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 7: schedules and rosters carry "at least one" in their type. */
class EnrollmentServiceTest {

    private static final Schedule MONDAY = new Schedule(DayOfWeek.MONDAY, LocalTime.of(18, 0));
    private static final Schedule THURSDAY = new Schedule(DayOfWeek.THURSDAY, LocalTime.of(18, 0));
    private final PaymentGateway gateway = (studentId, amount) -> {};

    private static EnrollmentForm form(String studentId, String courseId) {
        return new EnrollmentForm(studentId, courseId, studentId + "@example.com", "20", Option.none());
    }

    private EnrollmentService service() {
        var catalog = new Catalog()
            .addStudent(new Student("s1", "Ada"))
            .addStudent(new Student("s2", "Grace"))
            .addCourse(new Course("c1", "Functional Java", 5, 10_000))
            .addCourse(new Course("c2", "Not offered this term", 5, 10_000));
        var schedules = Map.of("c1", NonEmptyList.of(MONDAY, List.of(THURSDAY)));
        return new EnrollmentService(catalog, gateway, new WaitingList(), schedules);
    }

    @Test
    void enrolled_knowsTheFirstMeeting_withoutAnEmptyCheck() {
        var result = service().enroll(form("s1", "c1"));

        assertThat(result).isOk();
        assertThat(result.get()).isRight()
            .containsRight(new Enrolled(new Enrollment("s1", "c1"), MONDAY));
    }

    @Test
    void courseWithoutSchedules_isNotOffered() {
        assertThat(service().enroll(form("s1", "c2"))).isErr().containsError(new CourseNotFound("c2"));
    }

    @Test
    void roster_isNone_whenNobodyIsEnrolled() {
        assertThat(service().roster("c1")).isNone();
    }

    @Test
    void roster_isSome_withAHead_onceSomeoneIsEnrolled() {
        var service = service();
        service.enroll(form("s1", "c1"));
        service.enroll(form("s2", "c1"));

        var roster = service.roster("c1");

        assertThat(roster).isSome();
        assertThat(roster.get().head()).isEqualTo(new Enrollment("s1", "c1"));
        assertThat(roster.get().size()).isEqualTo(2);
        assertThat(WelcomeMail.compose("c1", roster.get())).isEqualTo("Welcome to c1, s1 and 1 more");
    }

    @Test
    void theWelcomeMailCannotBeSentToNobody() {
        var service = service();

        // The only way to call compose is through a Some: None has no list to hand over.
        String sent = service.roster("c1")
            .map(cohort -> WelcomeMail.compose("c1", cohort))
            .getOrElse("nobody to welcome yet");

        assertThat(sent).isEqualTo("nobody to welcome yet");
    }
}
