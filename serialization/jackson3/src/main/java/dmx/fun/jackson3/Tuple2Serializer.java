package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Tuple2;
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
    public void serialize(Tuple2 value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        ctxt.writeValue(gen, value._1());
        ctxt.writeValue(gen, value._2());
        gen.writeEndArray();
    }
}
