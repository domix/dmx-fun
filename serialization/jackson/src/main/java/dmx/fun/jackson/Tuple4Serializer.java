package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Tuple4;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Tuple4} to a JSON array {@code [a, b, c, d]}.
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
        provider.defaultSerializeValue(value._1(), gen);
        provider.defaultSerializeValue(value._2(), gen);
        provider.defaultSerializeValue(value._3(), gen);
        provider.defaultSerializeValue(value._4(), gen);
        gen.writeEndArray();
    }
}
