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
import dmx.fun.Either;
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
class EitherSerializer extends StdSerializer<Either> {

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
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new EitherSerializer(leftType, rightType,
            ContentWriter.of(ctxt, leftType, property), ContentWriter.of(ctxt, rightType, property));
    }

    @Override
    public void serialize(Either value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        writeEntry(value, gen, ctxt);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Either value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, value, Either.class, JsonToken.START_OBJECT));
        writeEntry(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    private void writeEntry(Either value, JsonGenerator gen, SerializationContext ctxt) {
        if (value.isRight()) {
            gen.writeName("right");
            ContentWriter.resolved(rightWriter, ctxt, rightType).write(value.getRight(), gen, ctxt);
        } else {
            gen.writeName("left");
            ContentWriter.resolved(leftWriter, ctxt, leftType).write(value.getLeft(), gen, ctxt);
        }
    }
}
