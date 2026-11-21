package dmx.fun.tutorial.chapter00;

import dmx.fun.tutorial.domain.Catalog;
import dmx.fun.tutorial.domain.Course;
import dmx.fun.tutorial.domain.PaymentException;
import dmx.fun.tutorial.domain.PaymentGateway;
import dmx.fun.tutorial.domain.Student;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentMessagesTest {

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
    void happyPath_describesTheEnrollment() {
        var service = new EnrollmentService(catalog(), acceptingGateway);

        assertThat(EnrollmentMessages.describe(service, "s1", "c1")).isEqualTo("Enrolled s1 in c1");
    }

    @Test
    void secondEnrollment_isTheNullTheCallerHadToRemember() {
        var service = new EnrollmentService(catalog(), acceptingGateway);
        service.enroll("s1", "c1");

        assertThat(EnrollmentMessages.describe(service, "s1", "c1")).isEqualTo("s1 is already enrolled in c1");
    }

    @Test
    void unknownStudent_isTheExceptionNobodyDeclared() {
        var service = new EnrollmentService(catalog(), acceptingGateway);

        assertThat(EnrollmentMessages.describe(service, "ghost", "c1")).isEqualTo("student not found: ghost");
    }

    @Test
    void paymentFailure_isTheCauseBuriedInARuntimeException() {
        var service = new EnrollmentService(catalog(), failingGateway);

        assertThat(EnrollmentMessages.describe(service, "s1", "c1")).isEqualTo("Payment failed: card declined");
    }
}
