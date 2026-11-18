package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Option;
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
    public void serialize(Option value, JsonGenerator gen, SerializationContext ctxt) {
        if (value.isEmpty()) {
            gen.writeNull();
        } else {
            ctxt.writeValue(gen, value.get());
        }
    }

    @Override
    public void serializeWithType(Option value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, value, Option.class, JsonToken.VALUE_STRING));
        serialize(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }
}
