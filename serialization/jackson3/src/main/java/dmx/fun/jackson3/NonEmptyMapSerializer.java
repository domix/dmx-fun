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
 * head entry first. Values are written through the serializer of the declared value type
 * (specialized by the runtime class when it is generic) and carry the type metadata that type
 * calls for, as a {@code Map} would; the object itself is never wrapped.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptyMapSerializer extends StdSerializer<NonEmptyMap> {

    private final JavaType valueType;
    private final @Nullable BeanProperty property;
    private final @Nullable TypeSerializer valueTypeSerializer;

    NonEmptyMapSerializer(JavaType valueType) {
        this(valueType, null, null);
    }

    private NonEmptyMapSerializer(JavaType valueType, @Nullable BeanProperty property,
                                  @Nullable TypeSerializer valueTypeSerializer) {
        super(NonEmptyMap.class);
        this.valueType = valueType;
        this.property = property;
        this.valueTypeSerializer = valueTypeSerializer;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new NonEmptyMapSerializer(valueType, property, ctxt.findTypeSerializer(valueType));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(NonEmptyMap value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value.toMap()).entrySet()) {
            Object key = entry.getKey();
            ctxt.findKeySerializer(key.getClass(), property).serialize(key, gen, ctxt);
            Object element = entry.getValue();
            ValueSerializer<Object> serializer = valueType.hasGenericTypes()
                ? ctxt.findContentValueSerializer(ctxt.constructSpecializedType(valueType, element.getClass()), property)
                : ctxt.findContentValueSerializer(element.getClass(), property);
            if (valueTypeSerializer == null) {
                serializer.serialize(element, gen, ctxt);
            } else {
                serializer.serializeWithType(element, gen, ctxt, valueTypeSerializer);
            }
        }
        gen.writeEndObject();
    }
}
