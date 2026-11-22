package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;

/**
 * Token handling shared by the single-field object codecs ({@code Result}, {@code Validated},
 * {@code Either}). Like Jackson's own object deserializers they accept a parser positioned on
 * {@code START_OBJECT} or, when an {@code As.PROPERTY} type id has already been consumed, on the
 * first {@code FIELD_NAME} or on {@code END_OBJECT}.
 */
@NullMarked
final class ObjectEntries {

    private ObjectEntries() {
    }

    /**
     * Moves the parser onto the first field of the object and returns its name.
     *
     * @throws IOException if the parser is not on an object or the object has no field
     */
    static String firstFieldName(JsonParser p, DeserializationContext ctxt, Class<?> type) throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.START_OBJECT) {
            token = p.nextToken();
        } else if (token != JsonToken.FIELD_NAME && token != JsonToken.END_OBJECT) {
            throw ctxt.wrongTokenException(p, type, JsonToken.START_OBJECT, "Expected object");
        }
        if (token != JsonToken.FIELD_NAME) {
            throw ctxt.wrongTokenException(p, type, JsonToken.FIELD_NAME, "Expected field name");
        }
        return p.currentName();
    }
}
