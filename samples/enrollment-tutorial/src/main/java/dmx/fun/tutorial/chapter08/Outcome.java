package dmx.fun.tutorial.chapter08;

import dmx.fun.tutorial.domain.Enrollment;

/** The two legitimate answers to a valid request (see chapter 6). {@code Enrolled} now says when the course meets. */
public final class Outcome {

    private Outcome() {}

    public record Enrolled(Enrollment enrollment, Schedule firstMeeting) {}

    public record Waitlisted(String studentId, String courseId, int position) {}
}
