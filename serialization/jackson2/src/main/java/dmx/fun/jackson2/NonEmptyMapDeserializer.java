package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import dmx.fun.NonEmptyMap;
import java.io.IOException;
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
class NonEmptyMapDeserializer extends StdDeserializer<NonEmptyMap> implements ContextualDeserializer {

    private final @Nullable KeyDeserializer keyDeserializer;
    private final @Nullable JavaType valueType;

    NonEmptyMapDeserializer() {
        super(NonEmptyMap.class);
        this.keyDeserializer = null;
        this.valueType = null;
    }

    private NonEmptyMapDeserializer(KeyDeserializer keyDeserializer, JavaType valueType) {
        super(NonEmptyMap.class);
        this.keyDeserializer = keyDeserializer;
        this.valueType = valueType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property)
            throws JsonMappingException {
        var contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 1) {
            return new NonEmptyMapDeserializer(
                ctxt.findKeyDeserializer(contextualType.containedType(0), property), contextualType.containedType(1));
        }
        return this;
    }

    @Override
    public NonEmptyMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw ctxt.wrongTokenException(p, NonEmptyMap.class, JsonToken.START_OBJECT, "Expected object");
        }
        Map<Object, Object> entries = new LinkedHashMap<>();
        while (p.nextToken() == JsonToken.FIELD_NAME) {
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

        if (entries.isEmpty()) {
            throw InvalidFormatException.from(p, "Cannot deserialize NonEmptyMap from empty object", null, NonEmptyMap.class);
        }
        return NonEmptyMap.fromMap(entries).get();
    }
}
