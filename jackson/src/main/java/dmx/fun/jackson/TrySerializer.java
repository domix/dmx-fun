package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Try;
import java.io.IOException;
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
    public void serialize(Try value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value.isSuccess()) {
            provider.defaultSerializeValue(value.get(), gen);
        } else {
            gen.writeStartObject();
            var message = value.getCause().getMessage();
            if (message != null) {
                gen.writeStringField("error", message);
            } else {
                gen.writeNullField("error");
            }
            gen.writeEndObject();
        }
    }
}
