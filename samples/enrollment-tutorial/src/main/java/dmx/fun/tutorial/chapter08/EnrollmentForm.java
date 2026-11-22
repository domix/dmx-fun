package dmx.fun.tutorial.chapter08;

import dmx.fun.Option;

/**
 * What arrives from the sign-up form: raw strings, the way HTTP delivers them. Nothing here is
 * validated yet; that is the job of {@link FormValidation}.
 */
public record EnrollmentForm(
    String studentId,
    String courseId,
    String email,
    String age,
    Option<String> discountCode
) {}
