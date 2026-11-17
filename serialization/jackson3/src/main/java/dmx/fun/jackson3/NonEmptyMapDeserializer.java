package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.NonEmptyMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes a JSON object to {@link NonEmptyMap}, keeping the first entry as head.
 * Throws {@link InvalidFormatException} for empty objects.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class NonEmptyMapDeserializer extends StdDeserializer<NonEmptyMap> {

    private final @Nullable JavaType mapType;

    NonEmptyMapDeserializer() {
        super(NonEmptyMap.class);
        this.mapType = null;
    }

    private NonEmptyMapDeserializer(JavaType mapType) {
        super(NonEmptyMap.class);
        this.mapType = mapType;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 1) {
            var mapType = ctxt.getTypeFactory().constructMapType(
                LinkedHashMap.class, contextualType.containedType(0), contextualType.containedType(1));
            return new NonEmptyMapDeserializer(mapType);
        }
        return this;
    }

    @Override
    public NonEmptyMap deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw ctxt.wrongTokenException(p, NonEmptyMap.class, JsonToken.START_OBJECT, "Expected object");
        }
        Map<Object, Object> entries = mapType != null
            ? ctxt.readValue(p, mapType)
            : p.readValueAs(LinkedHashMap.class);

        return (NonEmptyMap) NonEmptyMap.fromMap(entries)
            .getOrThrow(() -> InvalidFormatException.from(
                p, "Cannot deserialize NonEmptyMap from empty object", null, NonEmptyMap.class));
    }
}
