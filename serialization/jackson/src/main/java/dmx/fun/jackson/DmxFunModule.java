package dmx.fun.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import dmx.fun.Either;
import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Tuple2;
import dmx.fun.Tuple3;
import dmx.fun.Tuple4;
import dmx.fun.Validated;
import org.jspecify.annotations.NullMarked;

/**
 * Jackson module that registers serializers and deserializers for all dmx-fun types.
 *
 * <p>Register manually:
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper().registerModule(new DmxFunModule());
 * }</pre>
 *
 * <p>Or rely on auto-discovery:
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
 * }</pre>
 */
@NullMarked
public class DmxFunModule extends SimpleModule {

    /**
     * Constructs a new instance of DmxFunModule.
     * <p>
     * This module extends {@code SimpleModule} and is specifically designed to handle
     * serialization and deserialization for various dmx-fun types. During construction,
     * it registers custom serializers and deserializers for types such as {@code Try},
     * {@code Either}, {@code Option}, {@code Result}, {@code Validated}, {@code Tuple2},
     * {@code Tuple3}, {@code Tuple4}, and {@code NonEmptyList}.
     * <p>
     * It is used to ensure these types are correctly processed by Jackson's {@code ObjectMapper}.
     * <p>
     * Typical usage includes registering this module with an {@code ObjectMapper} either manually
     * or automatically.
     */
    public DmxFunModule() {
        super("DmxFunModule");
        registerSerializers();
        registerDeserializers();
    }

    @SuppressWarnings("unchecked")
    private void registerSerializers() {
        addSerializer(new OptionSerializer());
        addSerializer(new ResultSerializer());
        addSerializer(new TrySerializer());
        addSerializer(new ValidatedSerializer());
        addSerializer(new EitherSerializer());
        addSerializer(new Tuple2Serializer());
        addSerializer(new Tuple3Serializer());
        addSerializer(new Tuple4Serializer());
        addSerializer(new NonEmptyListSerializer());
    }

    @SuppressWarnings("unchecked")
    private void registerDeserializers() {
        addDeserializer(Option.class, new OptionDeserializer());
        addDeserializer(Result.class, new ResultDeserializer());
        addDeserializer(Try.class, new TryDeserializer());
        addDeserializer(Validated.class, new ValidatedDeserializer());
        addDeserializer(Either.class, new EitherDeserializer());
        addDeserializer(Tuple2.class, new Tuple2Deserializer());
        addDeserializer(Tuple3.class, new Tuple3Deserializer());
        addDeserializer(Tuple4.class, new Tuple4Deserializer());
        addDeserializer(NonEmptyList.class, new NonEmptyListDeserializer());
    }
}
