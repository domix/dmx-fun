package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Option;
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
    public void serialize(Option value, JsonGenerator gen, SerializationContext ctxt) {
        if (value.isEmpty()) {
            gen.writeNull();
        } else {
            ctxt.writeValue(gen, value.get());
        }
    }
}
