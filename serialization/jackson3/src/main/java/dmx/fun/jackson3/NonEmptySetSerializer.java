package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptySet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link NonEmptySet} to a JSON array {@code [head, ...rest]}, head first.
 * Elements are written through the serializer of the declared element type (specialized by
 * the runtime class when it is generic) and carry the type metadata that type calls for, as a
 * {@code Set} would; the array itself is never wrapped.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptySetSerializer extends StdSerializer<NonEmptySet> {

    private final JavaType elementType;
    private final @Nullable BeanProperty property;
    private final @Nullable TypeSerializer elementTypeSerializer;

    NonEmptySetSerializer(JavaType elementType) {
        this(elementType, null, null);
    }

    private NonEmptySetSerializer(JavaType elementType, @Nullable BeanProperty property,
                                  @Nullable TypeSerializer elementTypeSerializer) {
        super(NonEmptySet.class);
        this.elementType = elementType;
        this.property = property;
        this.elementTypeSerializer = elementTypeSerializer;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new NonEmptySetSerializer(elementType, property, ctxt.findTypeSerializer(elementType));
    }

    @Override
    public void serialize(NonEmptySet value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        for (Object element : value) {
            ValueSerializer<Object> serializer = elementType.hasGenericTypes()
                ? ctxt.findContentValueSerializer(ctxt.constructSpecializedType(elementType, element.getClass()), property)
                : ctxt.findContentValueSerializer(element.getClass(), property);
            if (elementTypeSerializer == null) {
                serializer.serialize(element, gen, ctxt);
            } else {
                serializer.serializeWithType(element, gen, ctxt, elementTypeSerializer);
            }
        }
        gen.writeEndArray();
    }
}
