package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Result;
import java.io.IOException;
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
    public void serialize(Result value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        writeEntry(value, gen, provider);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Result value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, TypeIds.of(typeSer, value, Result.class, JsonToken.START_OBJECT));
        writeEntry(value, gen, provider);
        typeSer.writeTypeSuffix(gen, typeId);
    }

    private static void writeEntry(Result value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value.isOk()) {
            gen.writeFieldName("ok");
            provider.defaultSerializeValue(value.get(), gen);
        } else {
            gen.writeFieldName("err");
            provider.defaultSerializeValue(value.getError(), gen);
        }
    }
}
