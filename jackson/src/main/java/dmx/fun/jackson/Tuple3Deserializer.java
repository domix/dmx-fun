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
import dmx.fun.Tuple3;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes JSON array {@code [a, b, c]} to {@link Tuple3}.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class Tuple3Deserializer extends StdDeserializer<Tuple3> implements ContextualDeserializer {

    private final @Nullable JavaType type1;
    private final @Nullable JavaType type2;
    private final @Nullable JavaType type3;

    Tuple3Deserializer() {
        super(Tuple3.class);
        this.type1 = null;
        this.type2 = null;
        this.type3 = null;
    }

    private Tuple3Deserializer(@Nullable JavaType type1, @Nullable JavaType type2, @Nullable JavaType type3) {
        super(Tuple3.class);
        this.type1 = type1;
        this.type2 = type2;
        this.type3 = type3;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() >= 3) {
            return new Tuple3Deserializer(
                contextualType.containedType(0),
                contextualType.containedType(1),
                contextualType.containedType(2));
        }
        return this;
    }

    @Override
    public Tuple3 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            throw ctxt.wrongTokenException(p, Tuple3.class, JsonToken.START_ARRAY, "Expected array");
        }
        if (p.nextToken() == JsonToken.END_ARRAY) {
            throw InvalidFormatException.from(p, "Tuple3 requires exactly 3 elements, got 0", null, Tuple3.class);
        }
        Object v1 = type1 != null ? ctxt.readValue(p, type1) : p.readValueAs(Object.class);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            throw InvalidFormatException.from(p, "Tuple3 requires exactly 3 elements, got 1", null, Tuple3.class);
        }
        Object v2 = type2 != null ? ctxt.readValue(p, type2) : p.readValueAs(Object.class);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            throw InvalidFormatException.from(p, "Tuple3 requires exactly 3 elements, got 2", null, Tuple3.class);
        }
        Object v3 = type3 != null ? ctxt.readValue(p, type3) : p.readValueAs(Object.class);
        if (p.nextToken() != JsonToken.END_ARRAY) {
            throw InvalidFormatException.from(p, "Tuple3 requires exactly 3 elements, got more", null, Tuple3.class);
        }
        return Tuple3.of(v1, v2, v3);
    }
}
