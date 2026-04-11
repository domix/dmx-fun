package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Validated;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Validated} to JSON.
 *
 * <ul>
 *   <li>{@code Validated.valid(a)}   → {@code {"valid": a}}</li>
 *   <li>{@code Validated.invalid(e)} → {@code {"invalid": e}}</li>
 * </ul>
 */
@NullMarked
@SuppressWarnings("rawtypes")
class ValidatedSerializer extends StdSerializer<Validated> {

    ValidatedSerializer() {
        super(Validated.class);
    }

    @Override
    public void serialize(Validated value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        if (value.isValid()) {
            gen.writeFieldName("valid");
            provider.defaultSerializeValue(value.get(), gen);
        } else {
            gen.writeFieldName("invalid");
            provider.defaultSerializeValue(value.getError(), gen);
        }
        gen.writeEndObject();
    }
}
