package dmx.fun.tutorial.chapter00;

public class CourseFullException extends RuntimeException {
    public CourseFullException(String courseId) {
        super("course is full: " + courseId);
    }
}
