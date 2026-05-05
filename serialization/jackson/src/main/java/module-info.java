/**
 * Jackson serializers and deserializers for dmx-fun types.
 *
 * <p>Register all codecs by adding {@link dmx.fun.jackson.DmxFunModule} to your
 * {@code ObjectMapper}, or rely on auto-discovery via {@code ObjectMapper.findAndRegisterModules()}.
 */
module dmx.fun.jackson {
    requires dmx.fun;
    requires com.fasterxml.jackson.databind;
    requires org.jspecify;

    exports dmx.fun.jackson;

    provides com.fasterxml.jackson.databind.Module with dmx.fun.jackson.DmxFunModule;
}
