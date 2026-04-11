package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.Either;
import java.io.IOException;
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
class EitherDeserializer extends StdDeserializer<Either> implements ContextualDeserializer {

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
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() >= 2) {
            return new EitherDeserializer(contextualType.containedType(0), contextualType.containedType(1));
        }
        return this;
    }

    @Override
    public Either deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        p.nextToken(); // move to field name
        String fieldName = p.currentName();
        p.nextToken(); // move to value

        Either result;
        if ("right".equals(fieldName)) {
            Object value = rightType != null ? ctxt.readValue(p, rightType) : p.readValueAs(Object.class);
            result = Either.right(value);
        } else if ("left".equals(fieldName)) {
            Object value = leftType != null ? ctxt.readValue(p, leftType) : p.readValueAs(Object.class);
            result = Either.left(value);
        } else {
            throw ctxt.weirdStringException(fieldName, Either.class, "Expected 'right' or 'left' field");
        }

        p.nextToken(); // move past END_OBJECT
        return result;
    }
}
