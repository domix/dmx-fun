package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptyList;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link NonEmptyList} to a JSON array {@code [head, ...tail]}.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptyListSerializer extends StdSerializer<NonEmptyList> {

    NonEmptyListSerializer() {
        super(NonEmptyList.class);
    }

    @Override
    public void serialize(NonEmptyList value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        ctxt.writeValue(gen, value.head());
        for (Object element : value.tail()) {
            ctxt.writeValue(gen, element);
        }
        gen.writeEndArray();
    }
}
