package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptyMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link NonEmptyMap} to a JSON object, exactly like a {@code Map<K, V>},
 * head entry first. Values carry type metadata based on the declared value type
 * (or {@code Object} at the root), as a {@code Map} would; the object itself is never wrapped.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptyMapSerializer extends StdSerializer<NonEmptyMap> {

    private final @Nullable TypeSerializer valueTypeSerializer;

    NonEmptyMapSerializer() {
        this(null);
    }

    private NonEmptyMapSerializer(@Nullable TypeSerializer valueTypeSerializer) {
        super(NonEmptyMap.class);
        this.valueTypeSerializer = valueTypeSerializer;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        JavaType declared = property != null ? property.getType() : null;
        JavaType valueType = declared != null && declared.containedTypeCount() > 1
            ? declared.containedType(1)
            : ctxt.constructType(Object.class);
        return new NonEmptyMapSerializer(ctxt.findTypeSerializer(valueType));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(NonEmptyMap value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value.toMap()).entrySet()) {
            Object key = entry.getKey();
            ctxt.findKeySerializer(key.getClass(), null).serialize(key, gen, ctxt);
            writeValue(entry.getValue(), gen, ctxt);
        }
        gen.writeEndObject();
    }

    private void writeValue(Object element, JsonGenerator gen, SerializationContext ctxt) {
        if (valueTypeSerializer == null) {
            ctxt.writeValue(gen, element);
        } else {
            ctxt.findValueSerializer(element.getClass()).serializeWithType(element, gen, ctxt, valueTypeSerializer);
        }
    }
}
