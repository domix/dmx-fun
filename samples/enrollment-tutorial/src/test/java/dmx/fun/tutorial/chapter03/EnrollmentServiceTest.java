package dmx.fun.tutorial.chapter03;

import dmx.fun.Option;
import dmx.fun.tutorial.chapter03.EnrollmentError.CourseNotFound;
import dmx.fun.tutorial.chapter03.EnrollmentError.StudentNotFound;
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
 * Chapter 3: a missing discount code is not an error, a missing student still is, and the
 * {@code null}s from the catalog never leave the lookup methods.
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
    void noDiscountCode_chargesFullFee() {
        var service = new EnrollmentService(catalog(), recordingGateway);

        assertThat(service.enroll("s1", "c1", Option.none())).isOk().containsValue(new Enrollment("s1", "c1"));
        assertThat(amounts).containsExactly(10_000);
    }

    @Test
    void knownDiscountCode_reducesTheFee() {
        var service = new EnrollmentService(catalog(), recordingGateway);

        assertThat(service.enroll("s1", "c1", Option.some("EARLY10"))).isOk();
        assertThat(amounts).containsExactly(9_000);
    }

    @Test
    void unknownDiscountCode_isIgnored_notAnError() {
        var service = new EnrollmentService(catalog(), recordingGateway);

        assertThat(service.enroll("s1", "c1", Option.some("BOGUS"))).isOk();
        assertThat(amounts).containsExactly(10_000);
    }

    @Test
    void priceFor_readsLikeTheRule() {
        var service = new EnrollmentService(catalog(), recordingGateway);
        var course = new Course("c1", "Functional Java", 5, 10_000);

        assertThat(service.priceFor(course, Option.none())).isEqualTo(10_000);
        assertThat(service.priceFor(course, Option.some("ALUMNI25"))).isEqualTo(7_500);
        assertThat(service.priceFor(course, Option.some("nope"))).isEqualTo(10_000);
    }

    @Test
    void missingRows_areStillErrors() {
        var service = new EnrollmentService(catalog(), recordingGateway);

        assertThat(service.enroll("ghost", "c1", Option.none())).isErr().containsError(new StudentNotFound("ghost"));
        assertThat(service.enroll("s1", "nope", Option.none())).isErr().containsError(new CourseNotFound("nope"));
        assertThat(amounts).isEmpty();
    }
}
