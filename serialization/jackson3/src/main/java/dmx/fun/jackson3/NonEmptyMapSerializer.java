package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptyMap;
import java.util.Map;
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
    @SuppressWarnings("unchecked")
    public void serialize(NonEmptyMap value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value.toMap()).entrySet()) {
            Object key = entry.getKey();
            ctxt.findKeySerializer(key.getClass(), null).serialize(key, gen, ctxt);
            ctxt.writeValue(gen, entry.getValue());
        }
        gen.writeEndObject();
    }
}
