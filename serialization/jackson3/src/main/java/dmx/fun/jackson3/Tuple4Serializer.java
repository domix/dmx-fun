package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Tuple4;
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
    public void serialize(Tuple4 value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        ctxt.writeValue(gen, value._1());
        ctxt.writeValue(gen, value._2());
        ctxt.writeValue(gen, value._3());
        ctxt.writeValue(gen, value._4());
        gen.writeEndArray();
    }
}
