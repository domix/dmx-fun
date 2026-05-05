/**
 * Jakarta JSON Binding (JSON-B) and Jakarta XML Binding (JAXB) adapters for all
 * dmx-fun types.
 *
 * <p>Register JSON-B adapters in one step via {@link dmx.fun.jakarta.jaxb.DmxFunJsonbAdapters}:
 * <pre>{@code
 * Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
 *     .withAdapters(DmxFunJsonbAdapters.all()));
 * }</pre>
 *
 * <p>Apply XML adapters declaratively via {@code @XmlJavaTypeAdapter}:
 * <pre>{@code
 * @XmlJavaTypeAdapter(OptionXmlAdapter.class)
 * Option<String> nickname;
 * }</pre>
 */
module dmx.fun.jakarta.jaxb {
    requires transitive dmx.fun;
    requires static transitive jakarta.json.bind;
    requires static transitive jakarta.xml.bind;
    requires static org.jspecify;

    exports dmx.fun.jakarta.jaxb;
}
