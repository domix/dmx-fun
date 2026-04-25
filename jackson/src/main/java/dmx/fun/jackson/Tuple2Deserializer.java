package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.Tuple2;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes JSON array {@code [a, b]} to {@link Tuple2}.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class Tuple2Deserializer extends StdDeserializer<Tuple2> implements ContextualDeserializer {

    private final @Nullable JavaType type1;
    private final @Nullable JavaType type2;

    Tuple2Deserializer() {
        super(Tuple2.class);
        this.type1 = null;
        this.type2 = null;
    }

    private Tuple2Deserializer(@Nullable JavaType type1, @Nullable JavaType type2) {
        super(Tuple2.class);
        this.type1 = type1;
        this.type2 = type2;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() >= 2) {
            return new Tuple2Deserializer(contextualType.containedType(0), contextualType.containedType(1));
        }
        return this;
    }

    @Override
    public Tuple2 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            throw ctxt.wrongTokenException(p, Tuple2.class, JsonToken.START_ARRAY, "Expected array");
        }
        if (p.nextToken() == JsonToken.END_ARRAY) {
            throw InvalidFormatException.from(p, "Tuple2 requires exactly 2 elements, got 0", null, Tuple2.class);
        }
        Object v1 = type1 != null ? ctxt.readValue(p, type1) : p.readValueAs(Object.class);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            throw InvalidFormatException.from(p, "Tuple2 requires exactly 2 elements, got 1", null, Tuple2.class);
        }
        Object v2 = type2 != null ? ctxt.readValue(p, type2) : p.readValueAs(Object.class);
        if (p.nextToken() != JsonToken.END_ARRAY) {
            throw InvalidFormatException.from(p, "Tuple2 requires exactly 2 elements, got more", null, Tuple2.class);
        }
        return Tuple2.of(v1, v2);
    }
}
