package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Either;
import java.io.IOException;
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
    public void serialize(Either value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        writeEntry(value, gen, provider);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Either value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, TypeIds.of(typeSer, value, Either.class, JsonToken.START_OBJECT));
        writeEntry(value, gen, provider);
        typeSer.writeTypeSuffix(gen, typeId);
    }

    private static void writeEntry(Either value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value.isRight()) {
            gen.writeFieldName("right");
            provider.defaultSerializeValue(value.getRight(), gen);
        } else {
            gen.writeFieldName("left");
            provider.defaultSerializeValue(value.getLeft(), gen);
        }
    }
}
