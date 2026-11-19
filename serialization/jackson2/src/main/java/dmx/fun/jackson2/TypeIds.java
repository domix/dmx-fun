package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import org.jspecify.annotations.NullMarked;

/**
 * Builds the type id written around a dmx-fun value. The id is resolved from the public dmx-fun
 * type ({@code Result}, {@code Option}, ...) rather than from the runtime class, so every id style
 * ({@code CLASS}, {@code MINIMAL_CLASS}, {@code NAME}, ...) names a type the module's deserializers
 * are registered for, not an implementation such as {@code Result.Ok}.
 */
@NullMarked
final class TypeIds {

    private TypeIds() {
    }

    static WritableTypeId of(TypeSerializer typeSer, Object value, Class<?> dmxFunType, JsonToken shape) {
        TypeIdResolver resolver = typeSer.getTypeIdResolver();
        if (resolver == null) {
            // Id.DEDUCTION writes no id at all; only the shape matters.
            return typeSer.typeId(value, shape);
        }
        return typeSer.typeId(value, shape, resolver.idFromValueAndType(null, dmxFunType));
    }
}
