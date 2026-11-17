package dmx.fun.jackson3;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.Serializers;
import dmx.fun.NonEmptyMap;
import dmx.fun.NonEmptySet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Resolves {@link NonEmptySetSerializer} and {@link NonEmptyMapSerializer} from the full
 * {@link JavaType}, so the declared element, key and value types (with their generic bindings)
 * drive content serialization the way they do for {@code Set} and {@code Map}. Unresolved
 * type parameters fall back to {@code Object}.
 */
@NullMarked
final class NonEmptySerializers extends Serializers.Base {

    @Override
    public @Nullable ValueSerializer<?> findSerializer(SerializationConfig config, JavaType type,
                                                       BeanDescription.Supplier beanDescRef,
                                                       JsonFormat.@Nullable Value formatOverrides) {
        Class<?> raw = type.getRawClass();
        if (NonEmptySet.class.isAssignableFrom(raw)) {
            return new NonEmptySetSerializer(type.containedTypeOrUnknown(0));
        }
        if (NonEmptyMap.class.isAssignableFrom(raw)) {
            return new NonEmptyMapSerializer(type.containedTypeOrUnknown(0), type.containedTypeOrUnknown(1));
        }
        return null;
    }
}
