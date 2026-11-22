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
import dmx.fun.Result;
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
class ResultSerializer extends StdSerializer<Result> {

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
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new ResultSerializer(valueType, errorType,
            ContentWriter.of(ctxt, valueType, property), ContentWriter.of(ctxt, errorType, property));
    }

    @Override
    public void serialize(Result value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        writeEntry(value, gen, ctxt);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Result value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, value, Result.class, JsonToken.START_OBJECT));
        writeEntry(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    private void writeEntry(Result value, JsonGenerator gen, SerializationContext ctxt) {
        if (value.isOk()) {
            gen.writeName("ok");
            ContentWriter.resolved(valueWriter, ctxt, valueType).write(value.get(), gen, ctxt);
        } else {
            gen.writeName("err");
            ContentWriter.resolved(errorWriter, ctxt, errorType).write(value.getError(), gen, ctxt);
        }
    }
}
