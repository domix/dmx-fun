package dmx.fun.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Reads the elements of a container through the deserializer of their declared type, the way
 * Jackson's own collection and map deserializers do: the deserializer is contextualized with the
 * containing property (so formats such as {@code @JsonFormat} apply), a {@link TypeDeserializer}
 * for the declared type is honoured when default typing or {@code @JsonTypeInfo} asks for one,
 * and a JSON {@code null} goes through {@link ValueDeserializer#getNullValue}. Without a declared
 * type (raw container), values are read as {@code Object} through the active context.
 *
 * <p>Shared by {@link NonEmptyListDeserializer}, {@link NonEmptySetDeserializer} and
 * {@link NonEmptyMapDeserializer}. The result
 * may be {@code null}; callers decide whether that is acceptable.
 */
@NullMarked
final class ContentReader {

    private static final ContentReader RAW = new ContentReader(null, null);

    private final @Nullable ValueDeserializer<Object> deserializer;
    private final @Nullable TypeDeserializer typeDeserializer;

    private ContentReader(@Nullable ValueDeserializer<Object> deserializer, @Nullable TypeDeserializer typeDeserializer) {
        this.deserializer = deserializer;
        this.typeDeserializer = typeDeserializer;
    }

    static ContentReader raw() {
        return RAW;
    }

    static ContentReader of(DeserializationContext ctxt, JavaType declaredType, @Nullable BeanProperty property) {
        TypeDeserializer typeDeserializer = ctxt.findTypeDeserializer(declaredType);
        if (typeDeserializer != null) {
            typeDeserializer = typeDeserializer.forProperty(property);
        }
        return new ContentReader(ctxt.findContextualValueDeserializer(declaredType, property), typeDeserializer);
    }

    @Nullable Object read(JsonParser p, DeserializationContext ctxt) {
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
