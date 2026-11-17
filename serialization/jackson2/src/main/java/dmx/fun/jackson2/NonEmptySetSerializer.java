package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptySet;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link NonEmptySet} to a JSON array {@code [head, ...rest]}, head first.
 * Elements are written through the serializer of the declared element type and carry the type
 * metadata that type calls for, as a {@code Set} would (see {@link ContentWriter}); the array
 * itself is never wrapped.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptySetSerializer extends StdSerializer<NonEmptySet> implements ContextualSerializer {

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
    public JsonSerializer<?> createContextual(SerializerProvider provider, @Nullable BeanProperty property)
            throws JsonMappingException {
        return new NonEmptySetSerializer(elementType, ContentWriter.of(provider, elementType, property));
    }

    @Override
    public void serialize(NonEmptySet value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        ContentWriter writer = elements != null ? elements : ContentWriter.of(provider, elementType, null);
        gen.writeStartArray();
        for (Object element : value) {
            writer.write(element, gen, provider);
        }
        gen.writeEndArray();
    }
}
