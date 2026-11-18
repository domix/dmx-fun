package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Result;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Result} to a JSON object with a single {@code "ok"} or {@code "err"} field.
 *
 * <p>When the slot holding the value asks for a type id, the id is written around that object
 * (inline for {@code As.PROPERTY}, a wrapper array for {@code As.WRAPPER_ARRAY}) and names
 * {@code Result}, not the implementation class.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class ResultSerializer extends StdSerializer<Result> {

    ResultSerializer() {
        super(Result.class);
    }

    @Override
    public void serialize(Result value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        writeEntry(value, gen, ctxt);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Result value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, value, Result.class, JsonToken.START_OBJECT));
        writeEntry(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    private static void writeEntry(Result value, JsonGenerator gen, SerializationContext ctxt) {
        if (value.isOk()) {
            gen.writeName("ok");
            ctxt.writeValue(gen, value.get());
        } else {
            gen.writeName("err");
            ctxt.writeValue(gen, value.getError());
        }
    }
}
