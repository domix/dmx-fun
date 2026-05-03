package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dmx.fun.Try;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes JSON to {@link Try}.
 *
 * <ul>
 *   <li>{@code {"error": "msg"}} → {@code Try.failure(new RuntimeException("msg"))}</li>
 *   <li>{@code {"error": null}}  → {@code Try.failure(new RuntimeException(null))}</li>
 *   <li>any other value → {@code Try.success(v)}</li>
 * </ul>
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class TryDeserializer extends StdDeserializer<Try> implements ContextualDeserializer {

    private final @Nullable JavaType valueType;

    TryDeserializer() {
        super(Try.class);
        this.valueType = null;
    }

    private TryDeserializer(JavaType valueType) {
        super(Try.class);
        this.valueType = valueType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 0) {
            return new TryDeserializer(contextualType.containedType(0));
        }
        return this;
    }

    @Override
    public Try deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.START_OBJECT) {
            ObjectNode node = p.readValueAsTree();
            if (node.size() == 1 && node.has("error")) {
                var errorNode = node.get("error");
                if (errorNode.isTextual() || errorNode.isNull()) {
                    @Nullable String message = errorNode.isNull() ? null : errorNode.asText();
                    return Try.failure(new RuntimeException(message));
                }
            }
            // Object that is not a failure — treat as success
            if (valueType != null) {
                Object value = ctxt.readTreeAsValue(node, valueType);
                return Try.success(value);
            }
            return Try.success(node);
        }
        // Non-object token: success with value
        if (valueType != null) {
            Object value = ctxt.readValue(p, valueType);
            return Try.success(value);
        }
        Object value = p.readValueAs(Object.class);
        return Try.success(value);
    }
}
