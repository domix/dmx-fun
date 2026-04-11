package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.Option;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes JSON to {@link Option}.
 *
 * <ul>
 *   <li>{@code null} → {@code Option.none()}</li>
 *   <li>any value → {@code Option.some(v)}</li>
 * </ul>
 */
@NullMarked
@SuppressWarnings("rawtypes")
class OptionDeserializer extends StdDeserializer<Option> implements ContextualDeserializer {

    private final @Nullable JavaType valueType;

    OptionDeserializer() {
        super(Option.class);
        this.valueType = null;
    }

    private OptionDeserializer(JavaType valueType) {
        super(Option.class);
        this.valueType = valueType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 0) {
            return new OptionDeserializer(contextualType.containedType(0));
        }
        return this;
    }

    @Override
    public Option getNullValue(DeserializationContext ctxt) {
        return Option.none();
    }

    @Override
    public Option deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (valueType != null) {
            Object value = ctxt.readValue(p, valueType);
            return Option.some(value);
        }
        Object value = p.readValueAs(Object.class);
        return Option.some(value);
    }
}
