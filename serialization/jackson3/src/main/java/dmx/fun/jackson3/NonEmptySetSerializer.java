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
 * Elements carry type metadata based on the declared element type (or {@code Object} at the
 * root), as a {@code Set} would; the array itself is never wrapped.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptySetSerializer extends StdSerializer<NonEmptySet> {

    private final @Nullable TypeSerializer elementTypeSerializer;

    NonEmptySetSerializer() {
        this(null);
    }

    private NonEmptySetSerializer(@Nullable TypeSerializer elementTypeSerializer) {
        super(NonEmptySet.class);
        this.elementTypeSerializer = elementTypeSerializer;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        JavaType declared = property != null ? property.getType() : null;
        JavaType elementType = declared != null && declared.containedTypeCount() > 0
            ? declared.containedType(0)
            : ctxt.constructType(Object.class);
        return new NonEmptySetSerializer(ctxt.findTypeSerializer(elementType));
    }

    @Override
    public void serialize(NonEmptySet value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        for (Object element : value) {
            if (elementTypeSerializer == null) {
                ctxt.writeValue(gen, element);
            } else {
                ctxt.findValueSerializer(element.getClass())
                    .serializeWithType(element, gen, ctxt, elementTypeSerializer);
            }
        }
        gen.writeEndArray();
    }
}
