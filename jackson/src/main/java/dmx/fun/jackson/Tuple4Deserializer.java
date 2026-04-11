package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.Tuple4;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes JSON array {@code [a, b, c, d]} to {@link Tuple4}.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class Tuple4Deserializer extends StdDeserializer<Tuple4> implements ContextualDeserializer {

    private final @Nullable JavaType type1;
    private final @Nullable JavaType type2;
    private final @Nullable JavaType type3;
    private final @Nullable JavaType type4;

    Tuple4Deserializer() {
        super(Tuple4.class);
        this.type1 = null;
        this.type2 = null;
        this.type3 = null;
        this.type4 = null;
    }

    private Tuple4Deserializer(
            @Nullable JavaType type1,
            @Nullable JavaType type2,
            @Nullable JavaType type3,
            @Nullable JavaType type4) {
        super(Tuple4.class);
        this.type1 = type1;
        this.type2 = type2;
        this.type3 = type3;
        this.type4 = type4;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() >= 4) {
            return new Tuple4Deserializer(
                contextualType.containedType(0),
                contextualType.containedType(1),
                contextualType.containedType(2),
                contextualType.containedType(3));
        }
        return this;
    }

    @Override
    public Tuple4 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        p.nextToken(); // first element
        Object v1 = type1 != null ? ctxt.readValue(p, type1) : p.readValueAs(Object.class);
        p.nextToken();
        Object v2 = type2 != null ? ctxt.readValue(p, type2) : p.readValueAs(Object.class);
        p.nextToken();
        Object v3 = type3 != null ? ctxt.readValue(p, type3) : p.readValueAs(Object.class);
        p.nextToken();
        Object v4 = type4 != null ? ctxt.readValue(p, type4) : p.readValueAs(Object.class);
        p.nextToken(); // END_ARRAY
        return Tuple4.of(v1, v2, v3, v4);
    }
}
