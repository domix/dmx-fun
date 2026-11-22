package dmx.fun.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import dmx.fun.NonEmptyMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serializes {@link NonEmptyMap} to a JSON object, exactly like a {@code Map<K, V>},
 * head entry first. Keys use the key serializer of the declared key type ({@code Object}, hence
 * dynamic, when unresolved). Values are written through the serializer of the declared value type
 * and carry the type metadata that type calls for, as a {@code Map} would (see
 * {@link ContentWriter}). The object itself carries a type id only when the slot holding the map
 * asks for one (for example an {@code Object} field under default typing), again as a
 * {@code Map} would.
 */
@NullMarked
@SuppressWarnings("rawtypes")
class NonEmptyMapSerializer extends StdSerializer<NonEmptyMap> {

    private final JavaType keyType;
    private final JavaType valueType;
    private final @Nullable ValueSerializer<Object> keySerializer;
    private final @Nullable ContentWriter values;

    NonEmptyMapSerializer(JavaType keyType, JavaType valueType) {
        this(keyType, valueType, null, null);
    }

    private NonEmptyMapSerializer(JavaType keyType, JavaType valueType,
                                  @Nullable ValueSerializer<Object> keySerializer, @Nullable ContentWriter values) {
        super(NonEmptyMap.class);
        this.keyType = keyType;
        this.valueType = valueType;
        this.keySerializer = keySerializer;
        this.values = values;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new NonEmptyMapSerializer(keyType, valueType,
            ctxt.findKeySerializer(keyType, property), ContentWriter.of(ctxt, valueType, property));
    }

    @Override
    public void serialize(NonEmptyMap value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        writeEntries(value, gen, ctxt);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(NonEmptyMap value, JsonGenerator gen, SerializationContext ctxt,
                                  TypeSerializer typeSer) {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt, typeSer.typeId(value, JsonToken.START_OBJECT));
        writeEntries(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    @SuppressWarnings("unchecked")
    private void writeEntries(NonEmptyMap value, JsonGenerator gen, SerializationContext ctxt) {
        ValueSerializer<Object> keySer = keySerializer != null ? keySerializer : ctxt.findKeySerializer(keyType, null);
        ContentWriter writer = ContentWriter.resolved(values, ctxt, valueType);
        for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value.toMap()).entrySet()) {
            keySer.serialize(entry.getKey(), gen, ctxt);
            writer.write(entry.getValue(), gen, ctxt);
        }
    }
}
