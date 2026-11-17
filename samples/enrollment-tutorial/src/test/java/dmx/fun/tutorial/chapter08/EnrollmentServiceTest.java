package dmx.fun.tutorial.chapter08;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 8: the price is computed for a seat, never for a waiting-list spot. */
class EnrollmentServiceTest {

    private static final Schedule MONDAY = new Schedule(DayOfWeek.MONDAY, LocalTime.of(18, 0));

    private static EnrollmentForm form(String studentId) {
        return new EnrollmentForm(studentId, "c1", studentId + "@example.com", "20", Option.some("EARLY10"));
    }

    private EnrollmentService service() {
        var catalog = new Catalog()
            .addStudent(new Student("s1", "Ada"))
            .addStudent(new Student("s2", "Grace"))
            .addCourse(new Course("c1", "Functional Java", 1, 10_000));
        PaymentGateway gateway = (studentId, amount) -> {};
        return new EnrollmentService(catalog, gateway, new WaitingList(), Map.of("c1", NonEmptyList.singleton(MONDAY)));
    }

    @Test
    void seatAvailable_computesThePriceOnce() {
        var service = service();

        assertThat(service.enroll(form("s1"))).isOk();
        assertThat(service.priceComputations).isEqualTo(1);
    }

    @Test
    void waitlisted_neverComputesThePrice() {
        var service = service();
        service.enroll(form("s1"));
        service.priceComputations = 0;

        var result = service.enroll(form("s2"));

        assertThat(result).isOk();
        assertThat(result.get()).isLeft();
        assertThat(service.priceComputations).isZero();
    }

    @Test
    void invalidForm_neverComputesThePriceEither() {
        var service = service();
        var bad = new EnrollmentForm("", "c1", "nope", "x", Option.none());

        assertThat(service.enroll(bad)).isErr();
        assertThat(service.priceComputations).isZero();
    }

    @Test
    void lazyIsMemoized_secondGetIsFree() {
        int[] calls = {0};
        var price = dmx.fun.Lazy.of(() -> { calls[0]++; return 9_000; });

        assertThat(price.isEvaluated()).isFalse();
        assertThat(price.get()).isEqualTo(9_000);
        assertThat(price.get()).isEqualTo(9_000);
        assertThat(calls[0]).isEqualTo(1);
        assertThat(List.of(price.isEvaluated())).containsExactly(true);
    }
}
