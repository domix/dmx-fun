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
import dmx.fun.Validated;
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
class ValidatedSerializer extends StdSerializer<Validated> {

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
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new ValidatedSerializer(errorType, valueType,
            ContentWriter.of(ctxt, errorType, property), ContentWriter.of(ctxt, valueType, property));
    }

    @Override
    public void serialize(Validated value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        writeEntry(value, gen, ctxt);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Validated value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, value, Validated.class, JsonToken.START_OBJECT));
        writeEntry(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    private void writeEntry(Validated value, JsonGenerator gen, SerializationContext ctxt) {
        if (value.isValid()) {
            gen.writeName("valid");
            ContentWriter.resolved(valueWriter, ctxt, valueType).write(value.get(), gen, ctxt);
        } else {
            gen.writeName("invalid");
            ContentWriter.resolved(errorWriter, ctxt, errorType).write(value.getError(), gen, ctxt);
        }
    }
}
