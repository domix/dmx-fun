package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.Option;
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
class OptionDeserializer extends StdDeserializer<Option> {

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
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
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
    public Option deserialize(JsonParser p, DeserializationContext ctxt) {
        Object value = valueType != null ? ctxt.readValue(p, valueType) : p.readValueAs(Object.class);
        return Option.some(value);
    }
}
