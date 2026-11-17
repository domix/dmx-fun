package dmx.fun.tutorial.chapter06;

import dmx.fun.Either;
import dmx.fun.Result;
import dmx.fun.tutorial.chapter06.Outcome.Enrolled;
import dmx.fun.tutorial.chapter06.Outcome.Waitlisted;

/** The caller's side: the success track now has two shapes, and the switch still covers everything. */
public final class EnrollmentMessages {

    private EnrollmentMessages() {}

    // #region describe
    public static String describe(Result<Either<Waitlisted, Enrolled>, EnrollmentError> result) {
        return switch (result) {
            case Result.Ok(Either.Right(Enrolled(var e))) ->
                "Enrolled " + e.studentId() + " in " + e.courseId();
            case Result.Ok(Either.Left(Waitlisted(var s, var c, var position))) ->
                s + " is number " + position + " on the waiting list for " + c;
            case Result.Err(var error) ->
                "Could not process the request: " + error;
        };
    }
    // #endregion describe
}
