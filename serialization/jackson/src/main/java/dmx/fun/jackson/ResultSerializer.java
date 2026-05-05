package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Result;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Result} to JSON.
 *
 * <ul>
 *   <li>{@code Result.ok(v)}  → {@code {"ok": v}}</li>
 *   <li>{@code Result.err(e)} → {@code {"err": e}}</li>
 * </ul>
 */
@NullMarked
@SuppressWarnings("rawtypes")
class ResultSerializer extends StdSerializer<Result> {

    ResultSerializer() {
        super(Result.class);
    }

    @Override
    public void serialize(Result value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        if (value.isOk()) {
            gen.writeFieldName("ok");
            provider.defaultSerializeValue(value.get(), gen);
        } else {
            gen.writeFieldName("err");
            provider.defaultSerializeValue(value.getError(), gen);
        }
        gen.writeEndObject();
    }
}
