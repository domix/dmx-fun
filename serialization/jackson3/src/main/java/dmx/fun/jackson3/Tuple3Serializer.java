package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Tuple3;
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
    public void serialize(Tuple3 value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        ctxt.writeValue(gen, value._1());
        ctxt.writeValue(gen, value._2());
        ctxt.writeValue(gen, value._3());
        gen.writeEndArray();
    }
}
