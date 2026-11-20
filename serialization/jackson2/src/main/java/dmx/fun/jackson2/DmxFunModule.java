package dmx.fun.jackson2;

import com.fasterxml.jackson.databind.module.SimpleModule;
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
     * it registers custom deserializers for {@code Try}, {@code Either}, {@code Option},
     * {@code Result}, {@code Validated}, {@code Tuple2}, {@code Tuple3}, {@code Tuple4},
     * {@code NonEmptyList}, {@code NonEmptySet}, and {@code NonEmptyMap}. The matching serializers
     * are contributed by {@link #setupModule(SetupContext)} when the module is registered, because
     * they are built from the full declared type.
     * <p>
     * It is used to ensure these types are correctly processed by Jackson's {@code ObjectMapper}.
     * <p>
     * Typical usage includes registering this module with an {@code ObjectMapper} either manually
     * or automatically.
     */
    public DmxFunModule() {
        super("DmxFunModule");
        registerDeserializers();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Registers {@link DmxFunSerializers}, which builds every dmx-fun serializer from the full
     * {@code JavaType} so the declared payload, element, key and value types drive content
     * serialization. It is registered first so that class-keyed serializers added to this module
     * with {@code addSerializer} take precedence over it.
     */
    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new DmxFunSerializers());
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
