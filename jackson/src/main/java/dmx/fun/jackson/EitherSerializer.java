package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.Either;
import java.io.IOException;
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
    public void serialize(Either value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        if (value.isRight()) {
            gen.writeFieldName("right");
            provider.defaultSerializeValue(value.getRight(), gen);
        } else {
            gen.writeFieldName("left");
            provider.defaultSerializeValue(value.getLeft(), gen);
        }
        gen.writeEndObject();
    }
}
