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
import dmx.fun.Either;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link Either} to a JSON object with a single {@code "right"} or {@code "left"} field, whose
 * value is written through the serializer of the declared left or right type (see {@link ContentWriter}).
 *
 * <p>When the slot holding the value asks for a type id, the id is written around that object
 * (inline for {@code As.PROPERTY}, a wrapper array for {@code As.WRAPPER_ARRAY}) and names
 * {@code Either}, not the implementation class.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class EitherSerializer extends StdSerializer<Either> implements ContextualSerializer {

    private final JavaType leftType;
    private final JavaType rightType;
    private final @Nullable ContentWriter leftWriter;
    private final @Nullable ContentWriter rightWriter;

    EitherSerializer(JavaType leftType, JavaType rightType) {
        this(leftType, rightType, null, null);
    }

    private EitherSerializer(JavaType leftType, JavaType rightType,
                         @Nullable ContentWriter leftWriter, @Nullable ContentWriter rightWriter) {
        super(Either.class);
        this.leftType = leftType;
        this.rightType = rightType;
        this.leftWriter = leftWriter;
        this.rightWriter = rightWriter;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, @Nullable BeanProperty property)
            throws JsonMappingException {
        return new EitherSerializer(leftType, rightType,
            ContentWriter.of(provider, leftType, property), ContentWriter.of(provider, rightType, property));
    }

    @Override
    public void serialize(Either value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        writeEntry(value, gen, provider);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Either value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, TypeIds.of(typeSer, value, Either.class, JsonToken.START_OBJECT));
        writeEntry(value, gen, provider);
        typeSer.writeTypeSuffix(gen, typeId);
    }

    private void writeEntry(Either value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value.isRight()) {
            gen.writeFieldName("right");
            ContentWriter.resolved(rightWriter, provider, rightType).write(value.getRight(), gen, provider);
        } else {
            gen.writeFieldName("left");
            ContentWriter.resolved(leftWriter, provider, leftType).write(value.getLeft(), gen, provider);
        }
    }
}
