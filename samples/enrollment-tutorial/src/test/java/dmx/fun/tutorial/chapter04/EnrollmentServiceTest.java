package dmx.fun.tutorial.chapter04;

import dmx.fun.Option;
import dmx.fun.tutorial.chapter04.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.Enrollment;
import dmx.fun.tutorial.domain.PaymentException;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chapter 4: whatever the gateway throws becomes a {@code PaymentFailed}, except fatal errors,
 * which keep propagating because no business rule should swallow them.
 */
class EnrollmentServiceTest {

    private Catalog catalog() {
        return new Catalog()
            .addStudent(new Student("s1", "Ada"))
            .addCourse(new Course("c1", "Functional Java", 5, 10_000));
    }

    @Test
    void checkedException_becomesPaymentFailed() {
        PaymentGateway declining = (s, a) -> { throw new PaymentException("card declined"); };
        var service = new EnrollmentService(catalog(), declining);

        assertThat(service.enroll("s1", "c1", Option.none()))
            .isErr().containsError(new PaymentFailed("card declined"));
    }

    @Test
    void runtimeException_alsoBecomesPaymentFailed() {
        PaymentGateway flaky = (s, a) -> { throw new IllegalStateException("gateway timeout"); };
        var service = new EnrollmentService(catalog(), flaky);

        assertThat(service.enroll("s1", "c1", Option.none()))
            .isErr().containsError(new PaymentFailed("gateway timeout"));
    }

    @Test
    void fatalError_isNotCaptured() {
        PaymentGateway broken = (s, a) -> { throw new OutOfMemoryError("simulated"); };
        var service = new EnrollmentService(catalog(), broken);

        assertThatThrownBy(() -> service.enroll("s1", "c1", Option.none()))
            .isInstanceOf(OutOfMemoryError.class);
    }

    @Test
    void happyPath_unchanged() {
        var service = new EnrollmentService(catalog(), (s, a) -> {});

        assertThat(service.enroll("s1", "c1", Option.none()))
            .isOk().containsValue(new Enrollment("s1", "c1"));
    }
}
