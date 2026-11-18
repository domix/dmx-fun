package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptyList;
import java.io.IOException;
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
class NonEmptyListSerializer extends StdSerializer<NonEmptyList> implements ContextualSerializer {

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
    public JsonSerializer<?> createContextual(SerializerProvider provider, @Nullable BeanProperty property)
            throws JsonMappingException {
        return new NonEmptyListSerializer(elementType, ContentWriter.of(provider, elementType, property));
    }

    @Override
    public void serialize(NonEmptyList value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        writeElements(value, gen, provider);
        gen.writeEndArray();
    }

    @Override
    public void serializeWithType(NonEmptyList value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.START_ARRAY));
        writeElements(value, gen, provider);
        typeSer.writeTypeSuffix(gen, typeId);
    }

    private void writeElements(NonEmptyList value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        ContentWriter writer = elements != null ? elements : ContentWriter.of(provider, elementType, null);
        for (Object element : value) {
            writer.write(element, gen, provider);
        }
    }
}
