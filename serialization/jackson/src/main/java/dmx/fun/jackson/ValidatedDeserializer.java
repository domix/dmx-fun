package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.Validated;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes JSON to {@link Validated}.
 *
 * <ul>
 *   <li>{@code {"valid": a}}   → {@code Validated.valid(a)}</li>
 *   <li>{@code {"invalid": e}} → {@code Validated.invalid(e)}</li>
 * </ul>
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class ValidatedDeserializer extends StdDeserializer<Validated> implements ContextualDeserializer {

    private final @Nullable JavaType errorType;
    private final @Nullable JavaType valueType;

    ValidatedDeserializer() {
        super(Validated.class);
        this.errorType = null;
        this.valueType = null;
    }

    private ValidatedDeserializer(@Nullable JavaType errorType, @Nullable JavaType valueType) {
        super(Validated.class);
        this.errorType = errorType;
        this.valueType = valueType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() >= 2) {
            return new ValidatedDeserializer(contextualType.containedType(0), contextualType.containedType(1));
        }
        return this;
    }

    @Override
    public Validated deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw ctxt.wrongTokenException(p, Validated.class, JsonToken.START_OBJECT, "Expected object");
        }
        if (p.nextToken() != JsonToken.FIELD_NAME) {
            throw ctxt.wrongTokenException(p, Validated.class, JsonToken.FIELD_NAME, "Expected field name");
        }
        String fieldName = p.currentName();
        p.nextToken(); // move to value

        Validated result;
        if ("valid".equals(fieldName)) {
            Object value = valueType != null ? ctxt.readValue(p, valueType) : p.readValueAs(Object.class);
            result = Validated.valid(value);
        } else if ("invalid".equals(fieldName)) {
            Object error = errorType != null ? ctxt.readValue(p, errorType) : p.readValueAs(Object.class);
            result = Validated.invalid(error);
        } else {
            throw ctxt.weirdStringException(fieldName, Validated.class, "Expected 'valid' or 'invalid' field");
        }

        if (p.nextToken() == JsonToken.FIELD_NAME) {
            throw ctxt.weirdStringException(p.currentName(), Validated.class, "Unexpected extra field");
        }
        return result;
    }
}
