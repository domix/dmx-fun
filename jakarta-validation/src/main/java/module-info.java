/**
 * Jakarta Validation adapter for dmx-fun.
 *
 * <p>Bridges Jakarta Bean Validation with dmx-fun's functional types.
 * {@link dmx.fun.jakarta.validation.DmxValidator} validates objects and returns
 * {@link dmx.fun.Validated} carrying error information as a
 * {@link dmx.fun.NonEmptyList} instead of throwing
 * {@code ConstraintViolationException}.
 */
module dmx.fun.jakarta.validation {
    requires dmx.fun;
    requires static jakarta.validation;
    requires org.jspecify;

    exports dmx.fun.jakarta.validation;
}
