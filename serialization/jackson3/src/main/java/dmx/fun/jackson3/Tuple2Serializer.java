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
import dmx.fun.Tuple2;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link Tuple2} to a JSON array of its 2 elements, in order, each written through the
 * serializer of its declared component type (see {@link ContentWriter}).
 *
 * <p>When the slot holding the tuple asks for a type id, the id is written around that array,
 * as it is for any array-shaped value.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class Tuple2Serializer extends StdSerializer<Tuple2> {

    private final JavaType[] types;
    private final ContentWriter @Nullable [] writers;

    Tuple2Serializer(JavaType t1, JavaType t2) {
        this(new JavaType[] {t1, t2}, null);
    }

    private Tuple2Serializer(JavaType[] types, ContentWriter @Nullable [] writers) {
        super(Tuple2.class);
        this.types = types;
        this.writers = writers;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        ContentWriter[] w = new ContentWriter[types.length];
        for (int i = 0; i < types.length; i++) {
            w[i] = ContentWriter.of(ctxt, types[i], property);
        }
        return new Tuple2Serializer(types, w);
    }

    @Override
    public void serialize(Tuple2 value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray();
        writeElements(value, gen, ctxt);
        gen.writeEndArray();
    }

    @Override
    public void serializeWithType(Tuple2 value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, typeSer.typeId(value, JsonToken.START_ARRAY));
        writeElements(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    private void writeElements(Tuple2 value, JsonGenerator gen, SerializationContext ctxt) {
        writer(0, ctxt).write(value._1(), gen, ctxt);
        writer(1, ctxt).write(value._2(), gen, ctxt);
    }

    private ContentWriter writer(int index, SerializationContext ctxt) {
        return ContentWriter.resolved(writers != null ? writers[index] : null, ctxt, types[index]);
    }
}
