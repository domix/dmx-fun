package dmx.fun.jackson2;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import dmx.fun.Either;
import dmx.fun.NonEmptyList;
import dmx.fun.NonEmptyMap;
import dmx.fun.NonEmptySet;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Tuple2;
import dmx.fun.Tuple3;
import dmx.fun.Tuple4;
import dmx.fun.Validated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Resolves every dmx-fun serializer from the full {@link JavaType}, so the declared type
 * parameters (payloads, elements, keys and values, with their generic bindings) drive content
 * serialization the way they do for Jackson's own {@code List}, {@code Set} and {@code Map}.
 * Unresolved type parameters fall back to {@code Object}.
 */
@NullMarked
final class DmxFunSerializers extends Serializers.Base {

    @Override
    public @Nullable JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type,
                                                      BeanDescription beanDesc) {
        Class<?> raw = type.getRawClass();
        TypeFactory tf = config.getTypeFactory();
        if (Option.class.isAssignableFrom(raw)) {
            return new OptionSerializer(params(tf, type, Option.class, 1)[0]);
        }
        if (Try.class.isAssignableFrom(raw)) {
            return new TrySerializer(params(tf, type, Try.class, 1)[0]);
        }
        if (Result.class.isAssignableFrom(raw)) {
            JavaType[] p = params(tf, type, Result.class, 2);
            return new ResultSerializer(p[0], p[1]);
        }
        if (Validated.class.isAssignableFrom(raw)) {
            JavaType[] p = params(tf, type, Validated.class, 2);
            return new ValidatedSerializer(p[0], p[1]);
        }
        if (Either.class.isAssignableFrom(raw)) {
            JavaType[] p = params(tf, type, Either.class, 2);
            return new EitherSerializer(p[0], p[1]);
        }
        if (Tuple2.class.isAssignableFrom(raw)) {
            JavaType[] p = params(tf, type, Tuple2.class, 2);
            return new Tuple2Serializer(p[0], p[1]);
        }
        if (Tuple3.class.isAssignableFrom(raw)) {
            JavaType[] p = params(tf, type, Tuple3.class, 3);
            return new Tuple3Serializer(p[0], p[1], p[2]);
        }
        if (Tuple4.class.isAssignableFrom(raw)) {
            JavaType[] p = params(tf, type, Tuple4.class, 4);
            return new Tuple4Serializer(p[0], p[1], p[2], p[3]);
        }
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

    /**
     * The {@code count} type parameters of {@code dmxFunType} as bound in {@code type}, which may be
     * the dmx-fun type itself or an implementation such as {@code Result.Ok}; {@code Object} where unbound.
     */
    static JavaType[] params(TypeFactory tf, JavaType type, Class<?> dmxFunType, int count) {
        JavaType[] found = tf.findTypeParameters(type, dmxFunType);
        JavaType[] result = new JavaType[count];
        for (int i = 0; i < count; i++) {
            result[i] = found != null && i < found.length && found[i] != null ? found[i] : tf.constructType(Object.class);
        }
        return result;
    }

}
