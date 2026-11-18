package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Option;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Option} as the unwrapped value when present, or JSON {@code null} when empty.
 *
 * <p>When the slot holding the option asks for a type id, the whole value is wrapped the way a
 * scalar is (a wrapper array for {@code As.PROPERTY} and {@code As.WRAPPER_ARRAY}), since an
 * {@code Option} has no JSON shape of its own; the id names {@code Option}, not the
 * implementation class.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class OptionSerializer extends StdSerializer<Option> {

    OptionSerializer() {
        super(Option.class);
    }

    @Override
    public void serialize(Option value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value.isEmpty()) {
            gen.writeNull();
        } else {
            provider.defaultSerializeValue(value.get(), gen);
        }
    }

    @Override
    public void serializeWithType(Option value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, TypeIds.of(typeSer, value, Option.class, JsonToken.VALUE_STRING));
        serialize(value, gen, provider);
        typeSer.writeTypeSuffix(gen, typeId);
    }
}
