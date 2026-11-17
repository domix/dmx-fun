package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Either;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link Either} to JSON.
 *
 * <ul>
 *   <li>{@code Either.right(v)} → {@code {"right": v}}</li>
 *   <li>{@code Either.left(e)}  → {@code {"left": e}}</li>
 * </ul>
 */
@NullMarked
@SuppressWarnings("rawtypes")
class EitherSerializer extends StdSerializer<Either> {

    EitherSerializer() {
        super(Either.class);
    }

    @Override
    public void serialize(Either value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        if (value.isRight()) {
            gen.writeName("right");
            ctxt.writeValue(gen, value.getRight());
        } else {
            gen.writeName("left");
            ctxt.writeValue(gen, value.getLeft());
        }
        gen.writeEndObject();
    }
}
