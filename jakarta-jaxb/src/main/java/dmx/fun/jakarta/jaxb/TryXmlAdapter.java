package dmx.fun.jakarta.jaxb;

import dmx.fun.Try;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * JAXB {@link XmlAdapter} for {@link Try}.
 *
 * <p>XML shapes:
 * <pre>{@code
 * <!-- Try.success("done") -->
 * <field><value>done</value></field>
 *
 * <!-- Try.failure(new RuntimeException("boom")) -->
 * <field><error>boom</error></field>
 * }</pre>
 *
 * <p>Usage:
 * <pre>{@code
 * @XmlJavaTypeAdapter(TryXmlAdapter.class)
 * Try<String> result;
 * }</pre>
 */
@NullMarked
public final class TryXmlAdapter extends XmlAdapter<TryXmlAdapter.TryElement, Try<?>> {

    /** Creates a new instance. */
    public TryXmlAdapter() {}

    /** JAXB-mapped element for {@link Try}. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TryElement {
        /** Present when the computation succeeded; {@code null} otherwise. */
        @XmlElement(name = "value")
        public @Nullable String value;
        /** Present when the computation failed; holds the exception message. */
        @XmlElement(name = "error")
        public @Nullable String error;

        /** JAXB-required no-arg constructor. */
        public TryElement() {}
    }

    @Override
    public Try<?> unmarshal(@Nullable TryElement v) throws Exception {
        if (v == null || v.value == null) {
            String message = v != null ? v.error : null;
            return Try.failure(new RuntimeException(message));
        }
        return Try.success(v.value);
    }

    @Override
    public @Nullable TryElement marshal(@Nullable Try<?> v) throws Exception {
        if (v == null) {
            return null;
        }
        var element = new TryElement();
        if (v.isSuccess()) {
            element.value = String.valueOf(v.get());
        } else {
            element.error = v.getCause().getMessage();
        }
        return element;
    }
}
