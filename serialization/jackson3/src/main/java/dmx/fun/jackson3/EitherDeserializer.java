package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.Either;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes JSON to {@link Either}.
 *
 * <ul>
 *   <li>{@code {"right": v}} → {@code Either.right(v)}</li>
 *   <li>{@code {"left": e}}  → {@code Either.left(e)}</li>
 * </ul>
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class EitherDeserializer extends StdDeserializer<Either> {

    private final @Nullable JavaType leftType;
    private final @Nullable JavaType rightType;

    EitherDeserializer() {
        super(Either.class);
        this.leftType = null;
        this.rightType = null;
    }

    private EitherDeserializer(@Nullable JavaType leftType, @Nullable JavaType rightType) {
        super(Either.class);
        this.leftType = leftType;
        this.rightType = rightType;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() >= 2) {
            return new EitherDeserializer(contextualType.containedType(0), contextualType.containedType(1));
        }
        return this;
    }

    @Override
    public Either deserialize(JsonParser p, DeserializationContext ctxt) {
        String fieldName = ObjectEntries.firstPropertyName(p, ctxt, Either.class);
        p.nextToken(); // move to value

        Either result;
        if ("right".equals(fieldName)) {
            var value = rightType != null ? ctxt.readValue(p, rightType) : ctxt.readValue(p, Object.class);
            result = Either.right(value);
        } else if ("left".equals(fieldName)) {
            var value = leftType != null ? ctxt.readValue(p, leftType) : ctxt.readValue(p, Object.class);
            result = Either.left(value);
        } else {
            throw ctxt.weirdStringException(fieldName, Either.class, "Expected 'right' or 'left' field");
        }

        if (p.nextToken() == JsonToken.PROPERTY_NAME) {
            throw ctxt.weirdStringException(p.currentName(), Either.class, "Unexpected extra property");
        }
        return result;
    }
}
