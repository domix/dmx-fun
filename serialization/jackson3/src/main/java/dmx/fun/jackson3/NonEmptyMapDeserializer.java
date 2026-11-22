package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import dmx.fun.NonEmptyMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes a JSON object to {@link NonEmptyMap}, keeping the first entry as head. Keys go
 * through the key deserializer of the declared key type and values through the deserializer of
 * the declared value type (see {@link ContentReader}). Like Jackson's own map deserializer it
 * accepts a parser positioned on {@code START_OBJECT} or, when a type id in {@code As.PROPERTY}
 * style has already been consumed, on the first {@code PROPERTY_NAME} or on {@code END_OBJECT}.
 * Throws {@link InvalidFormatException} for empty objects and {@link MismatchedInputException}
 * for {@code null} keys or values.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class NonEmptyMapDeserializer extends StdDeserializer<NonEmptyMap> {

    private final @Nullable KeyDeserializer keyDeserializer;
    private final ContentReader values;

    NonEmptyMapDeserializer() {
        this(null, ContentReader.raw());
    }

    private NonEmptyMapDeserializer(@Nullable KeyDeserializer keyDeserializer, ContentReader values) {
        super(NonEmptyMap.class);
        this.keyDeserializer = keyDeserializer;
        this.values = values;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 1) {
            return new NonEmptyMapDeserializer(
                ctxt.findKeyDeserializer(contextualType.containedType(0), property),
                ContentReader.of(ctxt, contextualType.containedType(1), property));
        }
        return this;
    }

    @Override
    public NonEmptyMap deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonToken token = p.currentToken();
        if (token == JsonToken.START_OBJECT) {
            token = p.nextToken();
        } else if (token != JsonToken.PROPERTY_NAME && token != JsonToken.END_OBJECT) {
            throw ctxt.wrongTokenException(p, NonEmptyMap.class, JsonToken.START_OBJECT, "Expected object");
        }
        Map<Object, Object> entries = new LinkedHashMap<>();
        for (; token == JsonToken.PROPERTY_NAME; token = p.nextToken()) {
            String name = p.currentName();
            Object key = keyDeserializer != null ? keyDeserializer.deserializeKey(name, ctxt) : name;
            if (key == null) {
                throw MismatchedInputException.from(p, NonEmptyMap.class,
                    "Cannot deserialize NonEmptyMap with a null key for '" + name + "'");
            }
            p.nextToken();
            Object value = values.read(p, ctxt);
            if (value == null) {
                throw MismatchedInputException.from(p, NonEmptyMap.class,
                    "Cannot deserialize NonEmptyMap with a null value for key '" + name + "'");
            }
            entries.put(key, value);
        }

        if (entries.isEmpty()) {
            throw InvalidFormatException.from(p, "Cannot deserialize NonEmptyMap from empty object", null, NonEmptyMap.class);
        }
        return NonEmptyMap.fromMap(entries).get();
    }
}
