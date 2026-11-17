package dmx.fun.jackson3;

import tools.jackson.databind.module.SimpleModule;
import dmx.fun.Either;
import dmx.fun.NonEmptyList;
import dmx.fun.NonEmptyMap;
import dmx.fun.NonEmptySet;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Tuple2;
import dmx.fun.Tuple3;
import dmx.fun.Tuple4;
import dmx.fun.Validated;
import org.jspecify.annotations.NullMarked;

/**
 * Jackson 3 module that registers serializers and deserializers for all dmx-fun types.
 *
 * <p>Register manually:
 * <pre>{@code
 * ObjectMapper mapper = JsonMapper.builder().addModule(new DmxFunModule()).build();
 * }</pre>
 *
 * <p>Or rely on auto-discovery through the {@code tools.jackson.databind.JacksonModule}
 * service provider:
 * <pre>{@code
 * ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
 * }</pre>
 *
 * <p>This module targets Jackson 3 ({@code tools.jackson.*}). For Jackson 2 use
 * {@code dmx.fun.jackson2.DmxFunModule} from {@code fun-jackson2}.
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
     * {@code Tuple3}, {@code Tuple4}, {@code NonEmptyList}, {@code NonEmptySet}, and {@code NonEmptyMap}.
     * <p>
     * It is used to ensure these types are correctly processed by a Jackson 3 {@code ObjectMapper}.
     * <p>
     * Typical usage includes registering this module through {@code JsonMapper.builder()} either manually
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

    /**
     * {@inheritDoc}
     *
     * <p>Besides the class-keyed serializers above, registers {@link NonEmptySerializers}, which
     * builds the {@code NonEmptySet} and {@code NonEmptyMap} serializers from the full
     * {@code JavaType} so the declared element and value types drive content serialization.
     * It is registered first so that class-keyed serializers added to this module afterwards
     * take precedence over it.
     */
    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new NonEmptySerializers());
        super.setupModule(context);
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
        addDeserializer(NonEmptySet.class, new NonEmptySetDeserializer());
        addDeserializer(NonEmptyMap.class, new NonEmptyMapDeserializer());
    }
}
