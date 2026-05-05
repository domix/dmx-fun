package dmx.fun.jakarta.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.jspecify.annotations.NullMarked;

/**
 * Registry of all dmx-fun JAXB XML adapters.
 *
 * <p>Use individual adapters via {@code @XmlJavaTypeAdapter} on your fields:
 *
 * <pre>{@code
 * @XmlJavaTypeAdapter(OptionXmlAdapter.class)
 * Option<String> nickname;
 *
 * @XmlJavaTypeAdapter(ResultXmlAdapter.class)
 * Result<Integer, String> result;
 * }</pre>
 *
 * <p>Supported types and their XML shapes:
 * <table border="1">
 *   <caption>XML shapes by type</caption>
 *   <tr><th>Type</th><th>Present / success</th><th>Absent / failure</th></tr>
 *   <tr><td>{@code Option<T>}</td><td>{@code <f><value>v</value></f>}</td><td>{@code <f/>}</td></tr>
 *   <tr><td>{@code Result<V,E>}</td><td>{@code <f><ok>v</ok></f>}</td><td>{@code <f><err>e</err></f>}</td></tr>
 *   <tr><td>{@code Try<V>}</td><td>{@code <f><value>v</value></f>}</td><td>{@code <f><error>msg</error></f>}</td></tr>
 *   <tr><td>{@code Either<L,R>}</td><td>{@code <f><right>r</right></f>}</td><td>{@code <f><left>l</left></f>}</td></tr>
 * </table>
 */
@NullMarked
public final class DmxFunXmlAdapters {

    private DmxFunXmlAdapters() {}

    /**
     * Returns all dmx-fun JAXB adapters as an array, suitable for programmatic
     * registration.
     *
     * @return array of all XML adapters
     */
    public static XmlAdapter<?, ?>[] all() {
        return new XmlAdapter<?, ?>[] {
            new OptionXmlAdapter(),
            new ResultXmlAdapter(),
            new TryXmlAdapter(),
            new EitherXmlAdapter()
        };
    }
}
