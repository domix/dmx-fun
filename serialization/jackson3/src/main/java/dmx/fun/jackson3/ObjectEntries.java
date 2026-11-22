package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import org.jspecify.annotations.NullMarked;

/**
 * Token handling shared by the single-field object codecs ({@code Result}, {@code Validated},
 * {@code Either}). Like Jackson's own object deserializers they accept a parser positioned on
 * {@code START_OBJECT} or, when an {@code As.PROPERTY} type id has already been consumed, on the
 * first {@code PROPERTY_NAME} or on {@code END_OBJECT}.
 */
@NullMarked
final class ObjectEntries {

    private ObjectEntries() {
    }

    /**
     * Moves the parser onto the first field of the object and returns its name.
     *
     * @throws tools.jackson.databind.DatabindException if the parser is not on an object or the object has no field
     */
    static String firstPropertyName(JsonParser p, DeserializationContext ctxt, Class<?> type) {
        JsonToken token = p.currentToken();
        if (token == JsonToken.START_OBJECT) {
            token = p.nextToken();
        } else if (token != JsonToken.PROPERTY_NAME && token != JsonToken.END_OBJECT) {
            throw ctxt.wrongTokenException(p, type, JsonToken.START_OBJECT, "Expected object");
        }
        if (token != JsonToken.PROPERTY_NAME) {
            throw ctxt.wrongTokenException(p, type, JsonToken.PROPERTY_NAME, "Expected property name");
        }
        return p.currentName();
    }
}
