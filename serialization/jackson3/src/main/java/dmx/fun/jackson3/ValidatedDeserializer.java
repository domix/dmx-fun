package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.Validated;
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
class ValidatedDeserializer extends StdDeserializer<Validated> {

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
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() >= 2) {
            return new ValidatedDeserializer(contextualType.containedType(0), contextualType.containedType(1));
        }
        return this;
    }

    @Override
    public Validated deserialize(JsonParser p, DeserializationContext ctxt) {
        String fieldName = ObjectEntries.firstPropertyName(p, ctxt, Validated.class);
        p.nextToken(); // move to value

        Validated result;
        if ("valid".equals(fieldName)) {
            Object value = valueType != null ? ctxt.readValue(p, valueType) : ctxt.readValue(p, Object.class);
            result = Validated.valid(value);
        } else if ("invalid".equals(fieldName)) {
            Object error = errorType != null ? ctxt.readValue(p, errorType) : ctxt.readValue(p, Object.class);
            result = Validated.invalid(error);
        } else {
            throw ctxt.weirdStringException(fieldName, Validated.class, "Expected 'valid' or 'invalid' field");
        }

        if (p.nextToken() == JsonToken.PROPERTY_NAME) {
            throw ctxt.weirdStringException(p.currentName(), Validated.class, "Unexpected extra property");
        }
        return result;
    }
}
