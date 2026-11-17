package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
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
 * Deserializes a JSON array to {@link NonEmptySet}, keeping the first element as head.
 * Throws {@link InvalidFormatException} for empty arrays and {@link MismatchedInputException}
 * for {@code null} elements.
 */
@NullMarked
@SuppressWarnings({"rawtypes", "unchecked"})
class NonEmptySetDeserializer extends StdDeserializer<NonEmptySet> implements ContextualDeserializer {

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
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.containedTypeCount() > 0) {
            return new NonEmptySetDeserializer(contextualType.containedType(0));
        }
        return this;
    }

    @Override
    public NonEmptySet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            throw ctxt.wrongTokenException(p, NonEmptySet.class, JsonToken.START_ARRAY, "Expected array");
        }
        Set<Object> elements = new LinkedHashSet<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            var element = elementType != null ? ctxt.readValue(p, elementType) : p.readValueAs(Object.class);
            if (element == null) {
                throw MismatchedInputException.from(p, NonEmptySet.class,
                    "Cannot deserialize NonEmptySet with a null element");
            }
            elements.add(element);
        }

        if (elements.isEmpty()) {
            throw InvalidFormatException.from(p, "Cannot deserialize NonEmptySet from empty array", null, NonEmptySet.class);
        }
        return NonEmptySet.fromSet(elements).get();
    }
}
