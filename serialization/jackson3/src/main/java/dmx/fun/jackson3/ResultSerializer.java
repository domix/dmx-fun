package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Result;
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
    public void serialize(Result value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        if (value.isOk()) {
            gen.writeName("ok");
            ctxt.writeValue(gen, value.get());
        } else {
            gen.writeName("err");
            ctxt.writeValue(gen, value.getError());
        }
        gen.writeEndObject();
    }
}
