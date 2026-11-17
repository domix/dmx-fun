package dmx.fun.tutorial.chapter00;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(String courseId) {
        super("course not found: " + courseId);
    }
}
