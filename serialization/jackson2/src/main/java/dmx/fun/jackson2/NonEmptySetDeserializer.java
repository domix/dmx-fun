package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import dmx.fun.NonEmptySet;
import java.io.IOException;
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
class NonEmptySetDeserializer extends StdDeserializer<NonEmptySet> implements ContextualDeserializer {

    private final ContentReader elements;

    NonEmptySetDeserializer() {
        this(ContentReader.raw());
    }

    private NonEmptySetDeserializer(ContentReader elements) {
        super(NonEmptySet.class);
        this.elements = elements;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property)
            throws JsonMappingException {
        var contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 0) {
            return new NonEmptySetDeserializer(ContentReader.of(ctxt, contextualType.containedType(0), property));
        }
        return this;
    }

    @Override
    public NonEmptySet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
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
