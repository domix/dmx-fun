package dmx.fun.tutorial.chapter09;

import dmx.fun.Either;
import dmx.fun.Result;
import dmx.fun.tutorial.chapter08.EnrollmentError;
import dmx.fun.tutorial.chapter08.Outcome.Enrolled;
import dmx.fun.tutorial.chapter08.Outcome.Waitlisted;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Counts every outcome of {@code enroll} under one meter, tagged by what happened.
 *
 * <p>Because failures are values with names, the tag is the record's simple name: no exception
 * class to parse, no message to grep. A dashboard can show {@code PaymentFailed} next to
 * {@code Waitlisted} without anyone having agreed on a logging format.
 */
public final class EnrollmentMetrics {

    private final MeterRegistry registry;

    public EnrollmentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public Result<Either<Waitlisted, Enrolled>, EnrollmentError> record(Result<Either<Waitlisted, Enrolled>, EnrollmentError> result) {
        // #region record
        String outcome = switch (result) {
            case Result.Ok(Either.Right(Enrolled _))  -> "Enrolled";
            case Result.Ok(Either.Left(Waitlisted _)) -> "Waitlisted";
            case Result.Err(var error)                -> error.getClass().getSimpleName();
        };
        registry.counter("enrollment.outcome", "outcome", outcome).increment();
        return result;
        // #endregion record
    }
}
