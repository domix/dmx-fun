package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptySet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link NonEmptySet} to a JSON array {@code [head, ...rest]}, head first.
 * Elements are written through the serializer of the declared element type and carry the type
 * metadata that type calls for, as a {@code Set} would (see {@link ContentWriter}). The array
 * itself carries a type id only when the slot holding the set asks for one (for example an
 * {@code Object} field under default typing), again as a {@code Set} would.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptySetSerializer extends StdSerializer<NonEmptySet> {

    private final JavaType elementType;
    private final @Nullable ContentWriter elements;

    NonEmptySetSerializer(JavaType elementType) {
        this(elementType, null);
    }

    private NonEmptySetSerializer(JavaType elementType, @Nullable ContentWriter elements) {
        super(NonEmptySet.class);
        this.elementType = elementType;
        this.elements = elements;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new NonEmptySetSerializer(elementType, ContentWriter.of(ctxt, elementType, property));
    }

    @Override
    public void serialize(NonEmptySet value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        writeElements(value, gen, ctxt);
        gen.writeEndArray();
    }

    @Override
    public void serializeWithType(NonEmptySet value, JsonGenerator gen, SerializationContext ctxt,
                                  TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, typeSer.typeId(value, JsonToken.START_ARRAY));
        writeElements(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    private void writeElements(NonEmptySet value, JsonGenerator gen, SerializationContext ctxt) {
        ContentWriter writer = ContentWriter.resolved(elements, ctxt, elementType);
        for (Object element : value) {
            writer.write(element, gen, ctxt);
        }
    }
}
