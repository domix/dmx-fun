package dmx.fun.tutorial.chapter00;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(String studentId) {
        super("student not found: " + studentId);
    }
}
