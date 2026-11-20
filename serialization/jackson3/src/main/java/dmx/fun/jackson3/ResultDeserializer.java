package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.Result;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes JSON to {@link Result}.
 *
 * <ul>
 *   <li>{@code {"ok": v}}  → {@code Result.ok(v)}</li>
 *   <li>{@code {"err": e}} → {@code Result.err(e)}</li>
 * </ul>
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class ResultDeserializer extends StdDeserializer<Result> {

    private final @Nullable JavaType valueType;
    private final @Nullable JavaType errorType;

    ResultDeserializer() {
        super(Result.class);
        this.valueType = null;
        this.errorType = null;
    }

    private ResultDeserializer(@Nullable JavaType valueType, @Nullable JavaType errorType) {
        super(Result.class);
        this.valueType = valueType;
        this.errorType = errorType;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() >= 2) {
            return new ResultDeserializer(contextualType.containedType(0), contextualType.containedType(1));
        }
        return this;
    }

    @Override
    public Result deserialize(JsonParser p, DeserializationContext ctxt) {
        String fieldName = ObjectEntries.firstPropertyName(p, ctxt, Result.class);
        p.nextToken(); // move to value

        Result result;
        if ("ok".equals(fieldName)) {
            Object value = valueType != null ? ctxt.readValue(p, valueType) : ctxt.readValue(p, Object.class);
            result = Result.ok(value);
        } else if ("err".equals(fieldName)) {
            Object error = errorType != null ? ctxt.readValue(p, errorType) : ctxt.readValue(p, Object.class);
            result = Result.err(error);
        } else {
            throw ctxt.weirdStringException(fieldName, Result.class, "Expected 'ok' or 'err' field");
        }

        if (p.nextToken() == JsonToken.PROPERTY_NAME) {
            throw ctxt.weirdStringException(p.currentName(), Result.class, "Unexpected extra property");
        }
        return result;
    }
}
