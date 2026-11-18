package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Try;
import java.io.IOException;
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
    public void serialize(Try value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value.isSuccess()) {
            provider.defaultSerializeValue(value.get(), gen);
        } else {
            gen.writeStartObject();
            writeError(value, gen);
            gen.writeEndObject();
        }
    }

    @Override
    public void serializeWithType(Try value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        if (value.isSuccess()) {
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, TypeIds.of(typeSer, value, Try.class, JsonToken.VALUE_STRING));
            provider.defaultSerializeValue(value.get(), gen);
            typeSer.writeTypeSuffix(gen, typeId);
        } else {
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, TypeIds.of(typeSer, value, Try.class, JsonToken.START_OBJECT));
            writeError(value, gen);
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }

    private static void writeError(Try value, JsonGenerator gen) throws IOException {
        var message = value.getCause().getMessage();
        if (message != null) {
            gen.writeStringField("error", message);
        } else {
            gen.writeNullField("error");
        }
    }
}
