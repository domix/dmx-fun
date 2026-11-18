package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Reads the elements of a container through the deserializer of their declared type, the way
 * Jackson's own collection and map deserializers do: the deserializer is contextualized with the
 * containing property (so formats such as {@code @JsonFormat} apply), a {@link TypeDeserializer}
 * for the declared type is honoured when default typing or {@code @JsonTypeInfo} asks for one,
 * and a JSON {@code null} goes through {@link JsonDeserializer#getNullValue}. Without a declared
 * type (raw container), values are read as {@code Object} through the active context.
 *
 * <p>Shared by {@link NonEmptySetDeserializer} and {@link NonEmptyMapDeserializer}. The result
 * may be {@code null}; callers decide whether that is acceptable.
 */
@NullMarked
final class ContentReader {

    private static final ContentReader RAW = new ContentReader(null, null);

    private final @Nullable JsonDeserializer<Object> deserializer;
    private final @Nullable TypeDeserializer typeDeserializer;

    private ContentReader(@Nullable JsonDeserializer<Object> deserializer, @Nullable TypeDeserializer typeDeserializer) {
        this.deserializer = deserializer;
        this.typeDeserializer = typeDeserializer;
    }

    static ContentReader raw() {
        return RAW;
    }

    static ContentReader of(DeserializationContext ctxt, JavaType declaredType, @Nullable BeanProperty property)
            throws JsonMappingException {
        TypeDeserializer typeDeserializer = ctxt.getFactory().findTypeDeserializer(ctxt.getConfig(), declaredType);
        if (typeDeserializer != null) {
            typeDeserializer = typeDeserializer.forProperty(property);
        }
        return new ContentReader(ctxt.findContextualValueDeserializer(declaredType, property), typeDeserializer);
    }

    @Nullable Object read(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (deserializer == null) {
            return ctxt.readValue(p, Object.class);
        }
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            return deserializer.getNullValue(ctxt);
        }
        return typeDeserializer == null
            ? deserializer.deserialize(p, ctxt)
            : deserializer.deserializeWithType(p, ctxt, typeDeserializer);
    }
}
