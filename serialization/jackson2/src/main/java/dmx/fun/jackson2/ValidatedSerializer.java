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
import dmx.fun.Validated;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link Validated} to a JSON object with a single {@code "valid"} or {@code "invalid"} field, whose
 * value is written through the serializer of the declared value or error type (see {@link ContentWriter}).
 *
 * <p>When the slot holding the value asks for a type id, the id is written around that object
 * (inline for {@code As.PROPERTY}, a wrapper array for {@code As.WRAPPER_ARRAY}) and names
 * {@code Validated}, not the implementation class.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class ValidatedSerializer extends StdSerializer<Validated> implements ContextualSerializer {

    private final JavaType errorType;
    private final JavaType valueType;
    private final @Nullable ContentWriter errorWriter;
    private final @Nullable ContentWriter valueWriter;

    ValidatedSerializer(JavaType errorType, JavaType valueType) {
        this(errorType, valueType, null, null);
    }

    private ValidatedSerializer(JavaType errorType, JavaType valueType,
                         @Nullable ContentWriter errorWriter, @Nullable ContentWriter valueWriter) {
        super(Validated.class);
        this.errorType = errorType;
        this.valueType = valueType;
        this.errorWriter = errorWriter;
        this.valueWriter = valueWriter;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, @Nullable BeanProperty property)
            throws JsonMappingException {
        return new ValidatedSerializer(errorType, valueType,
            ContentWriter.of(provider, errorType, property), ContentWriter.of(provider, valueType, property));
    }

    @Override
    public void serialize(Validated value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        writeEntry(value, gen, provider);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Validated value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, TypeIds.of(typeSer, value, Validated.class, JsonToken.START_OBJECT));
        writeEntry(value, gen, provider);
        typeSer.writeTypeSuffix(gen, typeId);
    }

    private void writeEntry(Validated value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value.isValid()) {
            gen.writeFieldName("valid");
            ContentWriter.resolved(valueWriter, provider, valueType).write(value.get(), gen, provider);
        } else {
            gen.writeFieldName("invalid");
            ContentWriter.resolved(errorWriter, provider, errorType).write(value.getError(), gen, provider);
        }
    }
}
