package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Writes the elements of a container through the serializer of their declared type, the way
 * Jackson's own collection and map serializers do: a final declared type (or any declared type
 * other than {@code Object} under {@code MapperFeature.USE_STATIC_TYPING}, unless it carries type
 * ids) resolves its serializer once, any other type resolves per runtime class (specialized by the declared generic bindings
 * when there are any) and caches the result, and a {@link TypeSerializer} for the declared type
 * is applied when default typing or {@code @JsonTypeInfo} asks for one.
 *
 * <p>Shared by {@link NonEmptySetSerializer} and {@link NonEmptyMapSerializer}. Instances are
 * created in {@code createContextual}, one per contextualized serializer.
 */
@NullMarked
final class ContentWriter {

    private final JavaType declaredType;
    private final @Nullable BeanProperty property;
    private final @Nullable TypeSerializer typeSerializer;
    private final @Nullable JsonSerializer<Object> staticSerializer;
    private PropertySerializerMap dynamicSerializers = PropertySerializerMap.emptyForProperties();

    private ContentWriter(JavaType declaredType, @Nullable BeanProperty property,
                          @Nullable TypeSerializer typeSerializer,
                          @Nullable JsonSerializer<Object> staticSerializer) {
        this.declaredType = declaredType;
        this.property = property;
        this.typeSerializer = typeSerializer;
        this.staticSerializer = staticSerializer;
    }

    static ContentWriter of(SerializerProvider provider, JavaType declaredType, @Nullable BeanProperty property)
            throws JsonMappingException {
        TypeSerializer typeSerializer = provider.findTypeSerializer(declaredType);
        // Like BasicSerializerFactory.buildContainerSerializer: a declared type that carries type ids
        // is always resolved dynamically, so subtypes keep their own fields.
        boolean staticTyping = typeSerializer == null
            && (declaredType.isFinal()
                || (provider.isEnabled(MapperFeature.USE_STATIC_TYPING) && !declaredType.isJavaLangObject()));
        JsonSerializer<Object> staticSerializer = staticTyping
            ? provider.findContentValueSerializer(declaredType, property)
            : null;
        return new ContentWriter(declaredType, property, typeSerializer, staticSerializer);
    }

    void write(Object element, JsonGenerator gen, SerializerProvider provider) throws IOException {
        JsonSerializer<Object> serializer = staticSerializer != null ? staticSerializer : dynamicSerializer(element, provider);
        if (typeSerializer == null) {
            serializer.serialize(element, gen, provider);
        } else {
            serializer.serializeWithType(element, gen, provider, typeSerializer);
        }
    }

    private JsonSerializer<Object> dynamicSerializer(Object element, SerializerProvider provider)
            throws JsonMappingException {
        Class<?> runtimeClass = element.getClass();
        JsonSerializer<Object> serializer = dynamicSerializers.serializerFor(runtimeClass);
        if (serializer == null) {
            PropertySerializerMap.SerializerAndMapResult result = declaredType.hasGenericTypes()
                ? dynamicSerializers.findAndAddSecondarySerializer(
                    provider.constructSpecializedType(declaredType, runtimeClass), provider, property)
                : dynamicSerializers.findAndAddSecondarySerializer(runtimeClass, provider, property);
            dynamicSerializers = result.map;
            serializer = result.serializer;
        }
        return serializer;
    }
}
