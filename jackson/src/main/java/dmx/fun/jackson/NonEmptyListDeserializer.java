package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import dmx.fun.NonEmptyList;
import java.io.IOException;
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
class NonEmptyListDeserializer extends StdDeserializer<NonEmptyList> implements ContextualDeserializer {

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
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 0) {
            return new NonEmptyListDeserializer(contextualType.containedType(0));
        }
        return this;
    }

    @Override
    public NonEmptyList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // p is at START_ARRAY
        List<Object> elements = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            Object element = elementType != null ? ctxt.readValue(p, elementType) : p.readValueAs(Object.class);
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
