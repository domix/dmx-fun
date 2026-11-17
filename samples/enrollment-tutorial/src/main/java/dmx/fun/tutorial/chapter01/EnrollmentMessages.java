package dmx.fun.tutorial.chapter01;

import dmx.fun.Result;
import dmx.fun.tutorial.chapter01.EnrollmentError.AlreadyEnrolled;
import dmx.fun.tutorial.chapter01.EnrollmentError.CourseFull;
import dmx.fun.tutorial.chapter01.EnrollmentError.CourseNotFound;
import dmx.fun.tutorial.chapter01.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.chapter01.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.domain.Enrollment;

/**
 * The caller's side of chapter 1: one exhaustive {@code switch}, no {@code try}, no {@code null}
 * check. Remove any {@code case} and the class stops compiling.
 */
public final class EnrollmentMessages {

    private EnrollmentMessages() {}

    // #region describe
    public static String describe(Result<Enrollment, EnrollmentError> result) {
        return switch (result) {
            case Result.Ok(Enrollment e) ->
                "Enrolled " + e.studentId() + " in " + e.courseId();
            case Result.Err(StudentNotFound(var id)) ->
                "We have no student with id " + id;
            case Result.Err(CourseNotFound(var id)) ->
                "We have no course with id " + id;
            case Result.Err(CourseFull(var id, var capacity)) ->
                "Course " + id + " is full (" + capacity + " seats)";
            case Result.Err(AlreadyEnrolled(var studentId, var courseId)) ->
                studentId + " is already enrolled in " + courseId;
            case Result.Err(PaymentFailed(var reason)) ->
                "Payment failed: " + reason;
        };
    }
    // #endregion describe
}
