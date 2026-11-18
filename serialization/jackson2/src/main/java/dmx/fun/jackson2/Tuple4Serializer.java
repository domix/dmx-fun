package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Tuple4;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Tuple4} to a JSON array of its 4 elements, in order.
 *
 * <p>When the slot holding the tuple asks for a type id, the id is written around that array,
 * as it is for any array-shaped value.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class Tuple4Serializer extends StdSerializer<Tuple4> {

    Tuple4Serializer() {
        super(Tuple4.class);
    }

    @Override
    public void serialize(Tuple4 value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        writeElements(value, gen, provider);
        gen.writeEndArray();
    }

    @Override
    public void serializeWithType(Tuple4 value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.START_ARRAY));
        writeElements(value, gen, provider);
        typeSer.writeTypeSuffix(gen, typeId);
    }

    private static void writeElements(Tuple4 value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        provider.defaultSerializeValue(value._1(), gen);
        provider.defaultSerializeValue(value._2(), gen);
        provider.defaultSerializeValue(value._3(), gen);
        provider.defaultSerializeValue(value._4(), gen);
    }
}
