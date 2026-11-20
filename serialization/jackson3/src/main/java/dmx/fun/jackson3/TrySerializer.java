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
import dmx.fun.Try;
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
class TrySerializer extends StdSerializer<Try> {

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
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new TrySerializer(valueType, ContentWriter.of(ctxt, valueType, property));
    }

    @Override
    public void serialize(Try attempt, JsonGenerator gen, SerializationContext ctxt) {
        if (attempt.isSuccess()) {
            writeValue(attempt, gen, ctxt);
        } else {
            gen.writeStartObject();
            writeError(attempt, gen);
            gen.writeEndObject();
        }
    }

    @Override
    public void serializeWithType(Try attempt, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        if (attempt.isSuccess()) {
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, attempt, Try.class, JsonToken.VALUE_STRING));
            writeValue(attempt, gen, ctxt);
            typeSer.writeTypeSuffix(gen, ctxt, typeId);
        } else {
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, attempt, Try.class, JsonToken.START_OBJECT));
            writeError(attempt, gen);
            typeSer.writeTypeSuffix(gen, ctxt, typeId);
        }
    }

    private void writeValue(Try attempt, JsonGenerator gen, SerializationContext ctxt) {
        ContentWriter.resolved(value, ctxt, valueType).write(attempt.get(), gen, ctxt);
    }

    private static void writeError(Try attempt, JsonGenerator gen) {
        var message = attempt.getCause().getMessage();
        if (message != null) {
            gen.writeStringProperty("error", message);
        } else {
            gen.writeNullProperty("error");
        }
    }
}
