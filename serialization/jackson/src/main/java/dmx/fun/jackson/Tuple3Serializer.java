package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Tuple3;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Tuple3} to a JSON array {@code [a, b, c]}.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class Tuple3Serializer extends StdSerializer<Tuple3> {

    Tuple3Serializer() {
        super(Tuple3.class);
    }

    @Override
    public void serialize(Tuple3 value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        provider.defaultSerializeValue(value._1(), gen);
        provider.defaultSerializeValue(value._2(), gen);
        provider.defaultSerializeValue(value._3(), gen);
        gen.writeEndArray();
    }
}
