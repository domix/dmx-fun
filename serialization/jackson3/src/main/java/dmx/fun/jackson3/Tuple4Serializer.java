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
import dmx.fun.Tuple4;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link Tuple4} to a JSON array of its 4 elements, in order, each written through the
 * serializer of its declared component type (see {@link ContentWriter}).
 *
 * <p>When the slot holding the tuple asks for a type id, the id is written around that array,
 * as it is for any array-shaped value.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class Tuple4Serializer extends StdSerializer<Tuple4> {

    private final JavaType[] types;
    private final ContentWriter @Nullable [] writers;

    Tuple4Serializer(JavaType t1, JavaType t2, JavaType t3, JavaType t4) {
        this(new JavaType[] {t1, t2, t3, t4}, null);
    }

    private Tuple4Serializer(JavaType[] types, ContentWriter @Nullable [] writers) {
        super(Tuple4.class);
        this.types = types;
        this.writers = writers;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        ContentWriter[] w = new ContentWriter[types.length];
        for (int i = 0; i < types.length; i++) {
            w[i] = ContentWriter.of(ctxt, types[i], property);
        }
        return new Tuple4Serializer(types, w);
    }

    @Override
    public void serialize(Tuple4 value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        writeElements(value, gen, ctxt);
        gen.writeEndArray();
    }

    @Override
    public void serializeWithType(Tuple4 value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, typeSer.typeId(value, JsonToken.START_ARRAY));
        writeElements(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    private void writeElements(Tuple4 value, JsonGenerator gen, SerializationContext ctxt) {
        writer(0, ctxt).write(value._1(), gen, ctxt);
        writer(1, ctxt).write(value._2(), gen, ctxt);
        writer(2, ctxt).write(value._3(), gen, ctxt);
        writer(3, ctxt).write(value._4(), gen, ctxt);
    }

    private ContentWriter writer(int index, SerializationContext ctxt) {
        return ContentWriter.resolved(writers != null ? writers[index] : null, ctxt, types[index]);
    }
}
