package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Try;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Try} to JSON.
 *
 * <ul>
 *   <li>{@code Try.success(v)}  → {@code v} (unwrapped)</li>
 *   <li>{@code Try.failure(ex)} → {@code {"error": "message"}} or {@code {"error": null}} if the
 *       exception message is null</li>
 * </ul>
 */
@NullMarked
@SuppressWarnings("rawtypes")
class TrySerializer extends StdSerializer<Try> {

    TrySerializer() {
        super(Try.class);
    }

    @Override
    public void serialize(Try value, JsonGenerator gen, SerializationContext ctxt) {
        if (value.isSuccess()) {
            ctxt.writeValue(gen, value.get());
        } else {
            gen.writeStartObject();
            var message = value.getCause().getMessage();
            if (message != null) {
                gen.writeStringProperty("error", message);
            } else {
                gen.writeNullProperty("error");
            }
            gen.writeEndObject();
        }
    }
}
