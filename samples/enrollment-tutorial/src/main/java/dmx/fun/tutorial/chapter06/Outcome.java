package dmx.fun.tutorial.chapter06;

import dmx.fun.tutorial.domain.Enrollment;

/**
 * The two things that can legitimately happen when a valid request meets a course.
 *
 * <p>Neither is an error. {@code Waitlisted} is not a failed {@code Enrolled}: nothing went wrong,
 * the student simply gets a different answer. That is why {@code enroll} returns
 * {@code Either<Waitlisted, Enrolled>} inside its {@code Result}, not a third error case.
 */
public final class Outcome {

    private Outcome() {}

    // #region outcome
    /** The student holds a seat; payment has been taken. */
    public record Enrolled(Enrollment enrollment) {}

    /** The course was full; the student is in line at {@code position}, and nothing was charged. */
    public record Waitlisted(String studentId, String courseId, int position) {}
    // #endregion outcome
}
