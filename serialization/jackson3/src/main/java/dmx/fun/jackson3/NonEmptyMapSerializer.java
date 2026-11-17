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
 * head entry first. Keys use the key serializer of the declared key type ({@code Object}, hence
 * dynamic, when unresolved). Values are written through the serializer of the declared value type
 * (specialized by the runtime class when it is generic) and carry the type metadata that type
 * calls for, as a {@code Map} would; the object itself is never wrapped.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptyMapSerializer extends StdSerializer<NonEmptyMap> {

    private final JavaType keyType;
    private final JavaType valueType;
    private final @Nullable BeanProperty property;
    private final @Nullable ValueSerializer<Object> keySerializer;
    private final @Nullable TypeSerializer valueTypeSerializer;

    NonEmptyMapSerializer(JavaType keyType, JavaType valueType) {
        this(keyType, valueType, null, null, null);
    }

    private NonEmptyMapSerializer(JavaType keyType, JavaType valueType, @Nullable BeanProperty property,
                                  @Nullable ValueSerializer<Object> keySerializer,
                                  @Nullable TypeSerializer valueTypeSerializer) {
        super(NonEmptyMap.class);
        this.keyType = keyType;
        this.valueType = valueType;
        this.property = property;
        this.keySerializer = keySerializer;
        this.valueTypeSerializer = valueTypeSerializer;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new NonEmptyMapSerializer(keyType, valueType, property,
            ctxt.findKeySerializer(keyType, property), ctxt.findTypeSerializer(valueType));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(NonEmptyMap value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value.toMap()).entrySet()) {
            Object key = entry.getKey();
            ValueSerializer<Object> keySer = keySerializer != null
                ? keySerializer
                : ctxt.findKeySerializer(keyType, property);
            keySer.serialize(key, gen, ctxt);
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
