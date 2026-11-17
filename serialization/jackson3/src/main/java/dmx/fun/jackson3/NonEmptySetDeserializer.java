package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.NonEmptySet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes a JSON array to {@link NonEmptySet}, keeping the first element as head.
 * Throws {@link InvalidFormatException} for empty arrays.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class NonEmptySetDeserializer extends StdDeserializer<NonEmptySet> {

    private final @Nullable JavaType elementType;

    NonEmptySetDeserializer() {
        super(NonEmptySet.class);
        this.elementType = null;
    }

    private NonEmptySetDeserializer(JavaType elementType) {
        super(NonEmptySet.class);
        this.elementType = elementType;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 0) {
            return new NonEmptySetDeserializer(contextualType.containedType(0));
        }
        return this;
    }

    @Override
    public NonEmptySet deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            throw ctxt.wrongTokenException(p, NonEmptySet.class, JsonToken.START_ARRAY, "Expected array");
        }
        Set<Object> elements = new LinkedHashSet<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            var element = elementType != null ? ctxt.readValue(p, elementType) : p.readValueAs(Object.class);
            elements.add(element);
        }

        return (NonEmptySet) NonEmptySet.fromSet(elements)
            .getOrThrow(() -> InvalidFormatException.from(
                p, "Cannot deserialize NonEmptySet from empty array", null, NonEmptySet.class));
    }
}
