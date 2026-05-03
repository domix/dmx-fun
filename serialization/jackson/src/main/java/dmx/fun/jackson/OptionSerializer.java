package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Option;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Option} to JSON.
 *
 * <ul>
 *   <li>{@code Option.some(v)} → {@code v} (unwrapped)</li>
 *   <li>{@code Option.none()} → {@code null}</li>
 * </ul>
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
}
