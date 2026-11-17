/**
 * Jackson 3 ({@code tools.jackson.*}) serializers and deserializers for dmx-fun types.
 *
 * <p>Register all codecs by adding {@link dmx.fun.jackson3.DmxFunModule} to your
 * {@code JsonMapper} builder, or rely on auto-discovery via
 * {@code JsonMapper.builder().findAndAddModules()}.
 *
 * <p>For the Jackson 2 line ({@code com.fasterxml.jackson.*}) use the {@code dmx.fun.jackson2}
 * module instead. Both can coexist on the same module path.
 */
module dmx.fun.jackson3 {
    requires dmx.fun;
    requires tools.jackson.databind;
    requires org.jspecify;

    exports dmx.fun.jackson3;

    provides tools.jackson.databind.JacksonModule with dmx.fun.jackson3.DmxFunModule;
}
