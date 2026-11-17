package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptyMap;
import java.io.IOException;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link NonEmptyMap} to a JSON object, exactly like a {@code Map<K, V>},
 * head entry first. Keys use the key serializer of the declared key type ({@code Object}, hence
 * dynamic, when unresolved). Values are written through the serializer of the declared value type
 * and carry the type metadata that type calls for, as a {@code Map} would (see
 * {@link ContentWriter}); the object itself is never wrapped.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptyMapSerializer extends StdSerializer<NonEmptyMap> implements ContextualSerializer {

    private final JavaType keyType;
    private final JavaType valueType;
    private final @Nullable JsonSerializer<Object> keySerializer;
    private final @Nullable ContentWriter values;

    NonEmptyMapSerializer(JavaType keyType, JavaType valueType) {
        this(keyType, valueType, null, null);
    }

    private NonEmptyMapSerializer(JavaType keyType, JavaType valueType,
                                  @Nullable JsonSerializer<Object> keySerializer, @Nullable ContentWriter values) {
        super(NonEmptyMap.class);
        this.keyType = keyType;
        this.valueType = valueType;
        this.keySerializer = keySerializer;
        this.values = values;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, @Nullable BeanProperty property)
            throws JsonMappingException {
        return new NonEmptyMapSerializer(keyType, valueType,
            provider.findKeySerializer(keyType, property), ContentWriter.of(provider, valueType, property));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(NonEmptyMap value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        JsonSerializer<Object> keySer = keySerializer != null ? keySerializer : provider.findKeySerializer(keyType, null);
        ContentWriter writer = values != null ? values : ContentWriter.of(provider, valueType, null);
        gen.writeStartObject();
        for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value.toMap()).entrySet()) {
            keySer.serialize(entry.getKey(), gen, provider);
            writer.write(entry.getValue(), gen, provider);
        }
        gen.writeEndObject();
    }
}
