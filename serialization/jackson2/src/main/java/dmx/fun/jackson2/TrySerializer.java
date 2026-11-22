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
import dmx.fun.Try;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link Try} as the unwrapped value on success, or {@code {"error": message}} on
 * failure. The value is written through the serializer of the declared value type (see
 * {@link ContentWriter}).
 *
 * <p>When the slot holding the value asks for a type id, a success is wrapped the way a scalar is
 * (it has no JSON shape of its own) and a failure carries the id around its object; in both cases
 * the id names {@code Try}, not the implementation class.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class TrySerializer extends StdSerializer<Try> implements ContextualSerializer {

    private final JavaType valueType;
    private final @Nullable ContentWriter value;

    TrySerializer(JavaType valueType) {
        this(valueType, null);
    }

    private TrySerializer(JavaType valueType, @Nullable ContentWriter value) {
        super(Try.class);
        this.valueType = valueType;
        this.value = value;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, @Nullable BeanProperty property)
            throws JsonMappingException {
        return new TrySerializer(valueType, ContentWriter.of(provider, valueType, property));
    }

    @Override
    public void serialize(Try attempt, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (attempt.isSuccess()) {
            writeValue(attempt, gen, provider);
        } else {
            gen.writeStartObject();
            writeError(attempt, gen);
            gen.writeEndObject();
        }
    }

    @Override
    public void serializeWithType(Try attempt, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        if (attempt.isSuccess()) {
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, TypeIds.of(typeSer, attempt, Try.class, JsonToken.VALUE_STRING));
            writeValue(attempt, gen, provider);
            typeSer.writeTypeSuffix(gen, typeId);
        } else {
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, TypeIds.of(typeSer, attempt, Try.class, JsonToken.START_OBJECT));
            writeError(attempt, gen);
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }

    private void writeValue(Try attempt, JsonGenerator gen, SerializerProvider provider) throws IOException {
        ContentWriter.resolved(value, provider, valueType).write(attempt.get(), gen, provider);
    }

    private static void writeError(Try attempt, JsonGenerator gen) throws IOException {
        var message = attempt.getCause().getMessage();
        if (message != null) {
            gen.writeStringField("error", message);
        } else {
            gen.writeNullField("error");
        }
    }
}
