package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.node.ObjectNode;
import dmx.fun.Try;
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
class TryDeserializer extends StdDeserializer<Try> {

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
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 0) {
            return new TryDeserializer(contextualType.containedType(0));
        }
        return this;
    }

    @Override
    public Try deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonToken token = p.currentToken();
        // An As.PROPERTY type id leaves the parser on the first PROPERTY_NAME (or END_OBJECT) of the object.
        if (token == JsonToken.START_OBJECT || token == JsonToken.PROPERTY_NAME || token == JsonToken.END_OBJECT) {
            ObjectNode node = token == JsonToken.END_OBJECT
                ? ctxt.getNodeFactory().objectNode()
                : (ObjectNode) ctxt.readTree(p);
            if (node.size() == 1 && node.has("error")) {
                var errorNode = node.get("error");
                if (errorNode.isString() || errorNode.isNull()) {
                    @Nullable String message = errorNode.isNull() ? null : errorNode.asString();
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
        Object value = ctxt.readValue(p, Object.class);
        return Try.success(value);
    }
}
