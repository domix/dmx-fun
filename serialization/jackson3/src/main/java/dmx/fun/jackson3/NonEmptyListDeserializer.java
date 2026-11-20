package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import dmx.fun.NonEmptyList;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes a JSON array to {@link NonEmptyList}. Elements are read through the deserializer
 * of the declared element type (see {@link ContentReader}). Throws {@link InvalidFormatException}
 * for empty arrays and {@link MismatchedInputException} for {@code null} elements.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class NonEmptyListDeserializer extends StdDeserializer<NonEmptyList> {

    private final ContentReader elements;

    NonEmptyListDeserializer() {
        this(ContentReader.raw());
    }

    private NonEmptyListDeserializer(ContentReader elements) {
        super(NonEmptyList.class);
        this.elements = elements;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 0) {
            return new NonEmptyListDeserializer(ContentReader.of(ctxt, contextualType.containedType(0), property));
        }
        return this;
    }

    @Override
    public NonEmptyList deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            throw ctxt.wrongTokenException(p, NonEmptyList.class, JsonToken.START_ARRAY, "Expected array");
        }
        List<Object> result = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            Object element = elements.read(p, ctxt);
            if (element == null) {
                throw MismatchedInputException.from(p, NonEmptyList.class,
                    "Cannot deserialize NonEmptyList with a null element");
            }
            result.add(element);
        }

        if (result.isEmpty()) {
            throw InvalidFormatException.from(p, "Cannot deserialize NonEmptyList from empty array", null, NonEmptyList.class);
        }
        return NonEmptyList.of(result.get(0), result.subList(1, result.size()));
    }
}
