package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Try;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Try} as the unwrapped value on success, or {@code {"error": message}} on
 * failure.
 *
 * <p>When the slot holding the value asks for a type id, a success is wrapped the way a scalar is
 * (it has no JSON shape of its own) and a failure carries the id around its object; in both cases
 * the id names {@code Try}, not the implementation class.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class TrySerializer extends StdSerializer<Try> {

    TrySerializer() {
        super(Try.class);
    }

    @Override
    public void serialize(Try value, JsonGenerator gen, SerializationContext ctxt) {
        if (value.isSuccess()) {
            ctxt.writeValue(gen, value.get());
        } else {
            gen.writeStartObject();
            writeError(value, gen);
            gen.writeEndObject();
        }
    }

    @Override
    public void serializeWithType(Try value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        if (value.isSuccess()) {
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, value, Try.class, JsonToken.VALUE_STRING));
            ctxt.writeValue(gen, value.get());
            typeSer.writeTypeSuffix(gen, ctxt, typeId);
        } else {
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, value, Try.class, JsonToken.START_OBJECT));
            writeError(value, gen);
            typeSer.writeTypeSuffix(gen, ctxt, typeId);
        }
    }

    private static void writeError(Try value, JsonGenerator gen) {
        var message = value.getCause().getMessage();
        if (message != null) {
            gen.writeStringProperty("error", message);
        } else {
            gen.writeNullProperty("error");
        }
    }
}
