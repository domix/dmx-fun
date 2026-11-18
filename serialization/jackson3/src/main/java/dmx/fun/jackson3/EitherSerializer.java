package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Either;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Either} to a JSON object with a single {@code "right"} or {@code "left"} field.
 *
 * <p>When the slot holding the value asks for a type id, the id is written around that object
 * (inline for {@code As.PROPERTY}, a wrapper array for {@code As.WRAPPER_ARRAY}) and names
 * {@code Either}, not the implementation class.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class EitherSerializer extends StdSerializer<Either> {

    EitherSerializer() {
        super(Either.class);
    }

    @Override
    public void serialize(Either value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        writeEntry(value, gen, ctxt);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Either value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, value, Either.class, JsonToken.START_OBJECT));
        writeEntry(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    private static void writeEntry(Either value, JsonGenerator gen, SerializationContext ctxt) {
        if (value.isRight()) {
            gen.writeName("right");
            ctxt.writeValue(gen, value.getRight());
        } else {
            gen.writeName("left");
            ctxt.writeValue(gen, value.getLeft());
        }
    }
}
