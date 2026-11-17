package dmx.fun.tutorial.chapter07;

import dmx.fun.NonEmptyList;
import dmx.fun.tutorial.domain.Enrollment;

/**
 * Sends the welcome mail to a cohort. The parameter type is the precondition: you cannot call
 * this with nobody, so there is no empty-list branch and no "sent 0 mails" log line.
 */
public final class WelcomeMail {

    private WelcomeMail() {}

    public static String compose(String courseId, NonEmptyList<Enrollment> cohort) {
        return "Welcome to " + courseId + ", " + cohort.head().studentId()
            + (cohort.size() == 1 ? "" : " and " + (cohort.size() - 1) + " more");
    }
}
