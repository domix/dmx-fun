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
import dmx.fun.Tuple3;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link Tuple3} to a JSON array of its 3 elements, in order, each written through the
 * serializer of its declared component type (see {@link ContentWriter}).
 *
 * <p>When the slot holding the tuple asks for a type id, the id is written around that array,
 * as it is for any array-shaped value.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class Tuple3Serializer extends StdSerializer<Tuple3> implements ContextualSerializer {

    private final JavaType[] types;
    private final ContentWriter @Nullable [] writers;

    Tuple3Serializer(JavaType t1, JavaType t2, JavaType t3) {
        this(new JavaType[] {t1, t2, t3}, null);
    }

    private Tuple3Serializer(JavaType[] types, ContentWriter @Nullable [] writers) {
        super(Tuple3.class);
        this.types = types;
        this.writers = writers;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, @Nullable BeanProperty property)
            throws JsonMappingException {
        ContentWriter[] w = new ContentWriter[types.length];
        for (int i = 0; i < types.length; i++) {
            w[i] = ContentWriter.of(provider, types[i], property);
        }
        return new Tuple3Serializer(types, w);
    }

    @Override
    public void serialize(Tuple3 value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        writeElements(value, gen, provider);
        gen.writeEndArray();
    }

    @Override
    public void serializeWithType(Tuple3 value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.START_ARRAY));
        writeElements(value, gen, provider);
        typeSer.writeTypeSuffix(gen, typeId);
    }

    private void writeElements(Tuple3 value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        writer(0, provider).write(value._1(), gen, provider);
        writer(1, provider).write(value._2(), gen, provider);
        writer(2, provider).write(value._3(), gen, provider);
    }

    private ContentWriter writer(int index, SerializerProvider provider) throws JsonMappingException {
        return ContentWriter.resolved(writers != null ? writers[index] : null, provider, types[index]);
    }
}
