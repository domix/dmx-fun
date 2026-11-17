package dmx.fun.tutorial.chapter09;

import dmx.fun.Either;
import dmx.fun.Result;
import dmx.fun.tutorial.chapter08.EnrollmentError;
import dmx.fun.tutorial.chapter08.EnrollmentError.AlreadyEnrolled;
import dmx.fun.tutorial.chapter08.EnrollmentError.CourseNotFound;
import dmx.fun.tutorial.chapter08.EnrollmentError.InvalidForm;
import dmx.fun.tutorial.chapter08.EnrollmentError.PaymentFailed;
import dmx.fun.tutorial.chapter08.EnrollmentError.StudentNotFound;
import dmx.fun.tutorial.chapter08.Outcome.Enrolled;
import dmx.fun.tutorial.chapter08.Outcome.Waitlisted;

/**
 * Chapter 9 — out the door: the HTTP status for every outcome, decided in one place.
 *
 * <p>The error type is sealed, so this switch is exhaustive: add a case to
 * {@code EnrollmentError} and this method stops compiling until it says which status the new
 * case gets. No framework here; a controller calls {@link #statusFor} and builds the response.
 */
public final class HttpMapping {

    private HttpMapping() {}

    public static int statusFor(Result<Either<Waitlisted, Enrolled>, EnrollmentError> result) {
        return switch (result) {
            case Result.Ok(Either.Right(Enrolled _))     -> 201;  // Created
            case Result.Ok(Either.Left(Waitlisted _))    -> 202;  // Accepted, pending a seat
            case Result.Err(InvalidForm _)               -> 422;  // Unprocessable Content
            case Result.Err(StudentNotFound _)           -> 404;
            case Result.Err(CourseNotFound _)            -> 404;
            case Result.Err(AlreadyEnrolled _)           -> 409;  // Conflict
            case Result.Err(PaymentFailed _)             -> 502;  // Bad Gateway: the payment provider failed
        };
    }
}
