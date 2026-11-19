package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Option;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link Option} as the unwrapped value when present, or JSON {@code null} when empty.
 * The value is written through the serializer of the declared value type (see {@link ContentWriter}).
 *
 * <p>When the slot holding the option asks for a type id, the whole value is wrapped the way a
 * scalar is (a wrapper array for {@code As.PROPERTY} and {@code As.WRAPPER_ARRAY}), since an
 * {@code Option} has no JSON shape of its own; the id names {@code Option}, not the
 * implementation class.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class OptionSerializer extends StdSerializer<Option> {

    private final JavaType valueType;
    private final @Nullable ContentWriter value;

    OptionSerializer(JavaType valueType) {
        this(valueType, null);
    }

    private OptionSerializer(JavaType valueType, @Nullable ContentWriter value) {
        super(Option.class);
        this.valueType = valueType;
        this.value = value;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new OptionSerializer(valueType, ContentWriter.of(ctxt, valueType, property));
    }

    @Override
    public void serialize(Option option, JsonGenerator gen, SerializationContext ctxt) {
        if (option.isEmpty()) {
            gen.writeNull();
        } else {
            ContentWriter.resolved(value, ctxt, valueType).write(option.get(), gen, ctxt);
        }
    }

    @Override
    public void serializeWithType(Option option, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, TypeIds.of(ctxt, typeSer, option, Option.class, JsonToken.VALUE_STRING));
        serialize(option, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }
}
