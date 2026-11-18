package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Validated;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Validated} to a JSON object with a single {@code "valid"} or {@code "invalid"} field.
 *
 * <p>When the slot holding the value asks for a type id, the id is written around that object
 * (inline for {@code As.PROPERTY}, a wrapper array for {@code As.WRAPPER_ARRAY}) and names
 * {@code Validated}, not the implementation class.
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
        writeEntry(value, gen, ctxt);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Validated value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, value, Validated.class, JsonToken.START_OBJECT));
        writeEntry(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    private static void writeEntry(Validated value, JsonGenerator gen, SerializationContext ctxt) {
        if (value.isValid()) {
            gen.writeName("valid");
            ctxt.writeValue(gen, value.get());
        } else {
            gen.writeName("invalid");
            ctxt.writeValue(gen, value.getError());
        }
    }
}
