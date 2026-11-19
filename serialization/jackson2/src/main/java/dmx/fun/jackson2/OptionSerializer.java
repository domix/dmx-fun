package dmx.fun.jackson2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Option;
import java.io.IOException;
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
class OptionSerializer extends StdSerializer<Option> implements ContextualSerializer {

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
    public JsonSerializer<?> createContextual(SerializerProvider provider, @Nullable BeanProperty property)
            throws JsonMappingException {
        return new OptionSerializer(valueType, ContentWriter.of(provider, valueType, property));
    }

    @Override
    public void serialize(Option option, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (option.isEmpty()) {
            gen.writeNull();
        } else {
            ContentWriter.resolved(value, provider, valueType).write(option.get(), gen, provider);
        }
    }

    @Override
    public void serializeWithType(Option option, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, TypeIds.of(typeSer, option, Option.class, JsonToken.VALUE_STRING));
        serialize(option, gen, provider);
        typeSer.writeTypeSuffix(gen, typeId);
    }
}
