package dmx.fun.tutorial.chapter09;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.jackson3.DmxFunModule;
import dmx.fun.tutorial.chapter08.EnrollmentError.AlreadyEnrolled;
import dmx.fun.tutorial.chapter08.EnrollmentForm;
import dmx.fun.tutorial.chapter08.EnrollmentService;
import dmx.fun.tutorial.chapter08.Schedule;
import dmx.fun.tutorial.chapter08.WaitingList;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.PaymentException;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 9: one outcome type, three doors: an HTTP status, a metric, a JSON document. */
class OutTheDoorTest {

    private static final Schedule MONDAY = new Schedule(DayOfWeek.MONDAY, LocalTime.of(18, 0));

    private static EnrollmentForm form(String studentId, String courseId) {
        return new EnrollmentForm(studentId, courseId, studentId + "@example.com", "20", Option.none());
    }

    private EnrollmentService service(PaymentGateway gateway) {
        var catalog = new Catalog()
            .addStudent(new Student("s1", "Ada"))
            .addStudent(new Student("s2", "Grace"))
            .addCourse(new Course("c1", "Functional Java", 1, 10_000));
        return new EnrollmentService(catalog, gateway, new WaitingList(), Map.of("c1", NonEmptyList.singleton(MONDAY)));
    }

    @Test
    void everyOutcomeHasAStatus() {
        var declining = service((s, a) -> { throw new PaymentException("card declined"); });
        var service = service((s, a) -> {});

        assertThat(HttpMapping.statusFor(service.enroll(form("s1", "c1")))).isEqualTo(201);
        assertThat(HttpMapping.statusFor(service.enroll(form("s2", "c1")))).isEqualTo(202);
        assertThat(HttpMapping.statusFor(service.enroll(form("s1", "c1")))).isEqualTo(201); // idempotent since chapter 2
        assertThat(HttpMapping.statusFor(Result.err(new AlreadyEnrolled("s1", "c1")))).isEqualTo(409); // the case still needs a status
        assertThat(HttpMapping.statusFor(service.enroll(form("ghost", "c1")))).isEqualTo(404);
        assertThat(HttpMapping.statusFor(service.enroll(form("s1", "nope")))).isEqualTo(404);
        assertThat(HttpMapping.statusFor(service.enroll(new EnrollmentForm("", "c1", "x", "y", Option.none())))).isEqualTo(422);
        assertThat(HttpMapping.statusFor(declining.enroll(form("s1", "c1")))).isEqualTo(502);
    }

    @Test
    void outcomesAreCountedByVariant() {
        var registry = new SimpleMeterRegistry();
        var metrics = new EnrollmentMetrics(registry);
        var service = service((s, a) -> {});

        metrics.record(service.enroll(form("s1", "c1")));
        metrics.record(service.enroll(form("s2", "c1")));
        metrics.record(service.enroll(form("ghost", "c1")));

        assertThat(registry.counter("enrollment.outcome", "outcome", "Enrolled").count()).isEqualTo(1);
        assertThat(registry.counter("enrollment.outcome", "outcome", "Waitlisted").count()).isEqualTo(1);
        assertThat(registry.counter("enrollment.outcome", "outcome", "StudentNotFound").count()).isEqualTo(1);
    }

    @Test
    void theOutcomeSerializesWithTheSameShapeTheRestOfTheSiteDocuments() {
        var mapper = JsonMapper.builder().addModule(new DmxFunModule()).build();
        var service = service((s, a) -> {});

        String enrolled = mapper.writeValueAsString(service.enroll(form("s1", "c1")));
        String waitlisted = mapper.writeValueAsString(service.enroll(form("s2", "c1")));
        String notFound = mapper.writeValueAsString(service.enroll(form("ghost", "c1")));

        assertThat(enrolled).startsWith("{\"ok\":{\"right\":{\"enrollment\":{\"studentId\":\"s1\",\"courseId\":\"c1\"}");
        assertThat(waitlisted).isEqualTo("{\"ok\":{\"left\":{\"studentId\":\"s2\",\"courseId\":\"c1\",\"position\":1}}}");
        assertThat(notFound).isEqualTo("{\"err\":{\"studentId\":\"ghost\"}}");
    }
}
