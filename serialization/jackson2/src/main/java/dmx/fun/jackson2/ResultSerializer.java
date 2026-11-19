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
import dmx.fun.Result;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link Result} to a JSON object with a single {@code "ok"} or {@code "err"} field, whose
 * value is written through the serializer of the declared value or error type (see {@link ContentWriter}).
 *
 * <p>When the slot holding the value asks for a type id, the id is written around that object
 * (inline for {@code As.PROPERTY}, a wrapper array for {@code As.WRAPPER_ARRAY}) and names
 * {@code Result}, not the implementation class.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class ResultSerializer extends StdSerializer<Result> implements ContextualSerializer {

    private final JavaType valueType;
    private final JavaType errorType;
    private final @Nullable ContentWriter valueWriter;
    private final @Nullable ContentWriter errorWriter;

    ResultSerializer(JavaType valueType, JavaType errorType) {
        this(valueType, errorType, null, null);
    }

    private ResultSerializer(JavaType valueType, JavaType errorType,
                         @Nullable ContentWriter valueWriter, @Nullable ContentWriter errorWriter) {
        super(Result.class);
        this.valueType = valueType;
        this.errorType = errorType;
        this.valueWriter = valueWriter;
        this.errorWriter = errorWriter;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, @Nullable BeanProperty property)
            throws JsonMappingException {
        return new ResultSerializer(valueType, errorType,
            ContentWriter.of(provider, valueType, property), ContentWriter.of(provider, errorType, property));
    }

    @Override
    public void serialize(Result value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        writeEntry(value, gen, provider);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Result value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, TypeIds.of(typeSer, value, Result.class, JsonToken.START_OBJECT));
        writeEntry(value, gen, provider);
        typeSer.writeTypeSuffix(gen, typeId);
    }

    private void writeEntry(Result value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value.isOk()) {
            gen.writeFieldName("ok");
            ContentWriter.resolved(valueWriter, provider, valueType).write(value.get(), gen, provider);
        } else {
            gen.writeFieldName("err");
            ContentWriter.resolved(errorWriter, provider, errorType).write(value.getError(), gen, provider);
        }
    }
}
