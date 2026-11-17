package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptyMap;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link NonEmptyMap} to a JSON object, exactly like a {@code Map<K, V>},
 * head entry first.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptyMapSerializer extends StdSerializer<NonEmptyMap> {

    NonEmptyMapSerializer() {
        super(NonEmptyMap.class);
    }

    @Override
    public void serialize(NonEmptyMap value, JsonGenerator gen, SerializationContext ctxt) {
        ctxt.writeValue(gen, value.toMap());
    }
}
