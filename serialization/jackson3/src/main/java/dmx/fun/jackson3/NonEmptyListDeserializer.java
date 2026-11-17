package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.NonEmptyList;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes JSON array to {@link NonEmptyList}.
 * Throws {@link InvalidFormatException} for empty arrays.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class NonEmptyListDeserializer extends StdDeserializer<NonEmptyList> {

    private final @Nullable JavaType elementType;

    NonEmptyListDeserializer() {
        super(NonEmptyList.class);
        this.elementType = null;
    }

    private NonEmptyListDeserializer(JavaType elementType) {
        super(NonEmptyList.class);
        this.elementType = elementType;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 0) {
            return new NonEmptyListDeserializer(contextualType.containedType(0));
        }
        return this;
    }

    @Override
    public NonEmptyList deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            throw ctxt.wrongTokenException(p, NonEmptyList.class, JsonToken.START_ARRAY, "Expected array");
        }
        List<Object> elements = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            var element = elementType != null ? ctxt.readValue(p, elementType) : p.readValueAs(Object.class);
            elements.add(element);
        }

        if (elements.isEmpty()) {
            throw InvalidFormatException.from(p, "Cannot deserialize NonEmptyList from empty array", null, NonEmptyList.class);
        }

        Object head = elements.get(0);
        List<Object> tail = elements.subList(1, elements.size());
        return NonEmptyList.of(head, tail);
    }
}
