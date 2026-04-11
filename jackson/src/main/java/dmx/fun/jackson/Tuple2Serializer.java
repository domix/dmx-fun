package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Tuple2;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Tuple2} to a JSON array {@code [a, b]}.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class Tuple2Serializer extends StdSerializer<Tuple2> {

    Tuple2Serializer() {
        super(Tuple2.class);
    }

    @Override
    public void serialize(Tuple2 value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        provider.defaultSerializeValue(value._1(), gen);
        provider.defaultSerializeValue(value._2(), gen);
        gen.writeEndArray();
    }
}
