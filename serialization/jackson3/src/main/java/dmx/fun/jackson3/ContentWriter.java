package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.impl.PropertySerializerMap;
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
    private final @Nullable ValueSerializer<Object> staticSerializer;
    private PropertySerializerMap dynamicSerializers = PropertySerializerMap.emptyForProperties();

    private ContentWriter(JavaType declaredType, @Nullable BeanProperty property,
                          @Nullable TypeSerializer typeSerializer,
                          @Nullable ValueSerializer<Object> staticSerializer) {
        this.declaredType = declaredType;
        this.property = property;
        this.typeSerializer = typeSerializer;
        this.staticSerializer = staticSerializer;
    }

    static ContentWriter of(SerializationContext ctxt, JavaType declaredType, @Nullable BeanProperty property) {
        TypeSerializer typeSerializer = ctxt.findTypeSerializer(declaredType);
        // Like BasicSerializerFactory.buildContainerSerializer: a declared type that carries type ids
        // is always resolved dynamically, so subtypes keep their own fields.
        boolean staticTyping = typeSerializer == null
            && (declaredType.isFinal()
                || (ctxt.isEnabled(MapperFeature.USE_STATIC_TYPING) && !declaredType.isJavaLangObject()));
        ValueSerializer<Object> staticSerializer = staticTyping
            ? ctxt.findContentValueSerializer(declaredType, property)
            : null;
        return new ContentWriter(declaredType, property, typeSerializer, staticSerializer);
    }

    void write(Object element, JsonGenerator gen, SerializationContext ctxt) {
        ValueSerializer<Object> serializer = staticSerializer != null ? staticSerializer : dynamicSerializer(element, ctxt);
        if (typeSerializer == null) {
            serializer.serialize(element, gen, ctxt);
        } else {
            serializer.serializeWithType(element, gen, ctxt, typeSerializer);
        }
    }

    private ValueSerializer<Object> dynamicSerializer(Object element, SerializationContext ctxt) {
        Class<?> runtimeClass = element.getClass();
        ValueSerializer<Object> serializer = dynamicSerializers.serializerFor(runtimeClass);
        if (serializer == null) {
            PropertySerializerMap.SerializerAndMapResult result = declaredType.hasGenericTypes()
                ? dynamicSerializers.findAndAddSecondarySerializer(
                    ctxt.constructSpecializedType(declaredType, runtimeClass), ctxt, property)
                : dynamicSerializers.findAndAddSecondarySerializer(runtimeClass, ctxt, property);
            dynamicSerializers = result.map;
            serializer = result.serializer;
        }
        return serializer;
    }
}
