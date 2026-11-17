package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.Tuple2;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes JSON array {@code [a, b]} to {@link Tuple2}.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class Tuple2Deserializer extends StdDeserializer<Tuple2> {

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
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() >= 2) {
            return new Tuple2Deserializer(contextualType.containedType(0), contextualType.containedType(1));
        }
        return this;
    }

    @Override
    public Tuple2 deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            throw ctxt.wrongTokenException(p, Tuple2.class, JsonToken.START_ARRAY, "Expected array");
        }
        if (p.nextToken() == JsonToken.END_ARRAY) {
            throw InvalidFormatException.from(p, "Tuple2 requires exactly 2 elements, got 0", null, Tuple2.class);
        }
        @Nullable Object v1 = type1 != null ? ctxt.readValue(p, type1) : p.readValueAs(Object.class);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            throw InvalidFormatException.from(p, "Tuple2 requires exactly 2 elements, got 1", null, Tuple2.class);
        }
        @Nullable Object v2 = type2 != null ? ctxt.readValue(p, type2) : p.readValueAs(Object.class);
        if (p.nextToken() != JsonToken.END_ARRAY) {
            throw InvalidFormatException.from(p, "Tuple2 requires exactly 2 elements, got more", null, Tuple2.class);
        }
        if (v1 == null || v2 == null) {
            throw InvalidFormatException.from(p, "Tuple2 elements must not be null", null, Tuple2.class);
        }
        return Tuple2.of(v1, v2);
    }
}
