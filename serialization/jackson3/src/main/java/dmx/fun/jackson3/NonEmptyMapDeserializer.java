package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.NonEmptyMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes a JSON object to {@link NonEmptyMap}, keeping the first entry as head.
 * Throws {@link InvalidFormatException} for empty objects and {@link MismatchedInputException}
 * for {@code null} keys or values.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class NonEmptyMapDeserializer extends StdDeserializer<NonEmptyMap> {

    private final @Nullable JavaType keyType;
    private final @Nullable JavaType valueType;

    NonEmptyMapDeserializer() {
        super(NonEmptyMap.class);
        this.keyType = null;
        this.valueType = null;
    }

    private NonEmptyMapDeserializer(JavaType keyType, JavaType valueType) {
        super(NonEmptyMap.class);
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 1) {
            return new NonEmptyMapDeserializer(contextualType.containedType(0), contextualType.containedType(1));
        }
        return this;
    }

    @Override
    public NonEmptyMap deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw ctxt.wrongTokenException(p, NonEmptyMap.class, JsonToken.START_OBJECT, "Expected object");
        }
        KeyDeserializer keyDeserializer = keyType != null ? ctxt.findKeyDeserializer(keyType, null) : null;
        Map<Object, Object> entries = new LinkedHashMap<>();
        while (p.nextToken() == JsonToken.PROPERTY_NAME) {
            String name = p.currentName();
            Object key = keyDeserializer != null ? keyDeserializer.deserializeKey(name, ctxt) : name;
            if (key == null) {
                throw MismatchedInputException.from(p, NonEmptyMap.class,
                    "Cannot deserialize NonEmptyMap with a null key for '" + name + "'");
            }
            p.nextToken();
            Object value = valueType != null ? ctxt.readValue(p, valueType) : p.readValueAs(Object.class);
            if (value == null) {
                throw MismatchedInputException.from(p, NonEmptyMap.class,
                    "Cannot deserialize NonEmptyMap with a null value for key '" + name + "'");
            }
            entries.put(key, value);
        }

        return (NonEmptyMap) NonEmptyMap.fromMap(entries)
            .getOrThrow(() -> InvalidFormatException.from(
                p, "Cannot deserialize NonEmptyMap from empty object", null, NonEmptyMap.class));
    }
}
