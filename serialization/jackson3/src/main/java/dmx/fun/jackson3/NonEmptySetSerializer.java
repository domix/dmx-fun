package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptySet;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link NonEmptySet} to a JSON array {@code [head, ...rest]}, head first.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptySetSerializer extends StdSerializer<NonEmptySet> {

    NonEmptySetSerializer() {
        super(NonEmptySet.class);
    }

    @Override
    public void serialize(NonEmptySet value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        for (Object element : value) {
            ctxt.writeValue(gen, element);
        }
        gen.writeEndArray();
    }
}
