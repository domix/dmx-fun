package dmx.fun.tutorial.chapter08;

import dmx.fun.tutorial.chapter08.Outcome.Waitlisted;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** In-memory waiting list per course. Position is 1-based, in order of arrival. */
public class WaitingList {

    private final Map<String, List<String>> byCourse = new HashMap<>();

    public Waitlisted add(String studentId, String courseId) {
        List<String> queue = byCourse.computeIfAbsent(courseId, _ -> new ArrayList<>());
        queue.add(studentId);
        return new Waitlisted(studentId, courseId, queue.size());
    }
}
