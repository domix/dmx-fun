package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Tuple4;
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
    public void serialize(Tuple4 value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        writeElements(value, gen, ctxt);
        gen.writeEndArray();
    }

    @Override
    public void serializeWithType(Tuple4 value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, typeSer.typeId(value, JsonToken.START_ARRAY));
        writeElements(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    private static void writeElements(Tuple4 value, JsonGenerator gen, SerializationContext ctxt) {
        ctxt.writeValue(gen, value._1());
        ctxt.writeValue(gen, value._2());
        ctxt.writeValue(gen, value._3());
        ctxt.writeValue(gen, value._4());
    }
}
