package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.Result;
import java.io.IOException;
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
class ResultDeserializer extends StdDeserializer<Result> implements ContextualDeserializer {

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
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() >= 2) {
            return new ResultDeserializer(contextualType.containedType(0), contextualType.containedType(1));
        }
        return this;
    }

    @Override
    public Result deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw ctxt.wrongTokenException(p, Result.class, JsonToken.START_OBJECT, "Expected object");
        }
        if (p.nextToken() != JsonToken.FIELD_NAME) {
            throw ctxt.wrongTokenException(p, Result.class, JsonToken.FIELD_NAME, "Expected field name");
        }
        String fieldName = p.currentName();
        p.nextToken(); // move to value

        Result result;
        if ("ok".equals(fieldName)) {
            Object value = valueType != null ? ctxt.readValue(p, valueType) : p.readValueAs(Object.class);
            result = Result.ok(value);
        } else if ("err".equals(fieldName)) {
            Object error = errorType != null ? ctxt.readValue(p, errorType) : p.readValueAs(Object.class);
            result = Result.err(error);
        } else {
            throw ctxt.weirdStringException(fieldName, Result.class, "Expected 'ok' or 'err' field");
        }

        if (p.nextToken() == JsonToken.FIELD_NAME) {
            throw ctxt.weirdStringException(p.currentName(), Result.class, "Unexpected extra field");
        }
        return result;
    }
}
