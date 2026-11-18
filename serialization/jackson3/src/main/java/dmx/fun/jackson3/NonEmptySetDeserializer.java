package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import dmx.fun.NonEmptySet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Deserializes a JSON array to {@link NonEmptySet}, keeping the first element as head. Elements
 * are read through the deserializer of the declared element type (see {@link ContentReader}).
 * Throws {@link InvalidFormatException} for empty arrays and {@link MismatchedInputException}
 * for {@code null} elements.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class NonEmptySetDeserializer extends StdDeserializer<NonEmptySet> {

    private final ContentReader elements;

    NonEmptySetDeserializer() {
        this(ContentReader.raw());
    }

    private NonEmptySetDeserializer(ContentReader elements) {
        super(NonEmptySet.class);
        this.elements = elements;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 0) {
            return new NonEmptySetDeserializer(ContentReader.of(ctxt, contextualType.containedType(0), property));
        }
        return this;
    }

    @Override
    public NonEmptySet deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            throw ctxt.wrongTokenException(p, NonEmptySet.class, JsonToken.START_ARRAY, "Expected array");
        }
        Set<Object> result = new LinkedHashSet<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            Object element = elements.read(p, ctxt);
            if (element == null) {
                throw MismatchedInputException.from(p, NonEmptySet.class,
                    "Cannot deserialize NonEmptySet with a null element");
            }
            result.add(element);
        }

        if (result.isEmpty()) {
            throw InvalidFormatException.from(p, "Cannot deserialize NonEmptySet from empty array", null, NonEmptySet.class);
        }
        return NonEmptySet.fromSet(result).get();
    }
}
