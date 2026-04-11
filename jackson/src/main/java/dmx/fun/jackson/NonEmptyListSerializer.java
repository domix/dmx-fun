package dmx.fun.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptyList;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;

/**
 * Serializes {@link NonEmptyList} to a JSON array {@code [head, ...tail]}.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptyListSerializer extends StdSerializer<NonEmptyList> {

    NonEmptyListSerializer() {
        super(NonEmptyList.class);
    }

    @Override
    public void serialize(NonEmptyList value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        provider.defaultSerializeValue(value.head(), gen);
        for (Object element : value.tail()) {
            provider.defaultSerializeValue(element, gen);
        }
        gen.writeEndArray();
    }
}
