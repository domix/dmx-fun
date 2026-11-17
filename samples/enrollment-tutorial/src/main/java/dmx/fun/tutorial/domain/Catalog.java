package dmx.fun.tutorial.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * In-memory store for students, courses and enrollments.
 *
 * <p>Deliberately old-school: lookups return {@code null} when nothing is found, the way most
 * repositories did before {@code Optional}. Later chapters wrap these calls instead of rewriting
 * the store, because that is what you do with the data layer you already have.
 */
public class Catalog {

    private final Map<String, Student> students = new HashMap<>();
    private final Map<String, Course> courses = new HashMap<>();
    private final List<Enrollment> enrollments = new ArrayList<>();

    public Catalog addStudent(Student student) {
        students.put(student.id(), student);
        return this;
    }

    public Catalog addCourse(Course course) {
        courses.put(course.id(), course);
        return this;
    }

    public @Nullable Student findStudent(String id) {
        return students.get(id);
    }

    public @Nullable Course findCourse(String id) {
        return courses.get(id);
    }

    public boolean isEnrolled(String studentId, String courseId) {
        return enrollments.stream()
            .anyMatch(e -> e.studentId().equals(studentId) && e.courseId().equals(courseId));
    }

    public long enrolledCount(String courseId) {
        return enrollments.stream().filter(e -> e.courseId().equals(courseId)).count();
    }

    public Enrollment save(Enrollment enrollment) {
        enrollments.add(enrollment);
        return enrollment;
    }
}
