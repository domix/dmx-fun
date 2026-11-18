package dmx.fun.jackson2;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.CollectionType;
import dmx.fun.NonEmptyList;
import dmx.fun.NonEmptyMap;
import dmx.fun.NonEmptySet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Resolves {@link NonEmptyListSerializer}, {@link NonEmptySetSerializer} and
 * {@link NonEmptyMapSerializer} from the full {@link JavaType}, so the declared element, key and
 * value types (with their generic bindings) drive content serialization the way they do for
 * {@code List}, {@code Set} and {@code Map}. Unresolved
 * type parameters fall back to {@code Object}.
 */
@NullMarked
final class NonEmptySerializers extends Serializers.Base {

    @Override
    public @Nullable JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type,
                                                      BeanDescription beanDesc) {
        Class<?> raw = type.getRawClass();
        if (NonEmptySet.class.isAssignableFrom(raw)) {
            return new NonEmptySetSerializer(type.containedTypeOrUnknown(0));
        }
        if (NonEmptyMap.class.isAssignableFrom(raw)) {
            return new NonEmptyMapSerializer(type.containedTypeOrUnknown(0), type.containedTypeOrUnknown(1));
        }
        return null;
    }

    /**
     * {@code NonEmptyList} is a {@code Collection}, so Jackson resolves it through this hook rather
     * than {@link #findSerializer}; the content type is the declared element type.
     */
    @Override
    public @Nullable JsonSerializer<?> findCollectionSerializer(SerializationConfig config, CollectionType type,
                                                                BeanDescription beanDesc,
                                                                @Nullable TypeSerializer elementTypeSerializer,
                                                                @Nullable JsonSerializer<Object> elementValueSerializer) {
        if (NonEmptyList.class.isAssignableFrom(type.getRawClass())) {
            return new NonEmptyListSerializer(type.getContentType());
        }
        return null;
    }
}
