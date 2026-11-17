package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Validated;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Validated} to JSON.
 *
 * <ul>
 *   <li>{@code Validated.valid(a)}   → {@code {"valid": a}}</li>
 *   <li>{@code Validated.invalid(e)} → {@code {"invalid": e}}</li>
 * </ul>
 */
@NullMarked
@SuppressWarnings("rawtypes")
class ValidatedSerializer extends StdSerializer<Validated> {

    ValidatedSerializer() {
        super(Validated.class);
    }

    @Override
    public void serialize(Validated value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        if (value.isValid()) {
            gen.writeName("valid");
            ctxt.writeValue(gen, value.get());
        } else {
            gen.writeName("invalid");
            ctxt.writeValue(gen, value.getError());
        }
        gen.writeEndObject();
    }
}
