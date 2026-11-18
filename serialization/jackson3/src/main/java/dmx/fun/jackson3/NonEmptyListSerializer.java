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
import dmx.fun.NonEmptyList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link NonEmptyList} to a JSON array {@code [head, ...tail]}.
 * Elements are written through the serializer of the declared element type and carry the type
 * metadata that type calls for, as a {@code List} would (see {@link ContentWriter}). The array
 * itself carries a type id only when the slot holding the list asks for one (for example an
 * {@code Object} field under default typing), again as a {@code List} would.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptyListSerializer extends StdSerializer<NonEmptyList> {

    private final JavaType elementType;
    private final @Nullable ContentWriter elements;

    NonEmptyListSerializer(JavaType elementType) {
        this(elementType, null);
    }

    private NonEmptyListSerializer(JavaType elementType, @Nullable ContentWriter elements) {
        super(NonEmptyList.class);
        this.elementType = elementType;
        this.elements = elements;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new NonEmptyListSerializer(elementType, ContentWriter.of(ctxt, elementType, property));
    }

    @Override
    public void serialize(NonEmptyList value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        writeElements(value, gen, ctxt);
        gen.writeEndArray();
    }

    @Override
    public void serializeWithType(NonEmptyList value, JsonGenerator gen, SerializationContext ctxt,
                                  TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, typeSer.typeId(value, JsonToken.START_ARRAY));
        writeElements(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    private void writeElements(NonEmptyList value, JsonGenerator gen, SerializationContext ctxt) {
        ContentWriter writer = elements != null ? elements : ContentWriter.of(ctxt, elementType, null);
        for (Object element : value) {
            writer.write(element, gen, ctxt);
        }
    }
}
