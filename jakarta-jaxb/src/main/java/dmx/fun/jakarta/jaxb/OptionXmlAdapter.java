package dmx.fun.jakarta.jaxb;

import dmx.fun.Option;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * JAXB {@link XmlAdapter} for {@link Option}.
 *
 * <p>XML shapes:
 * <pre>{@code
 * <!-- Option.some("alice") -->
 * <field><value>alice</value></field>
 *
 * <!-- Option.none() -->
 * <field/>
 * }</pre>
 *
 * <p>Usage:
 * <pre>{@code
 * @XmlJavaTypeAdapter(OptionXmlAdapter.class)
 * Option<String> nickname;
 * }</pre>
 */
@NullMarked
public final class OptionXmlAdapter extends XmlAdapter<OptionXmlAdapter.OptionElement, Option<?>> {

    /** Creates a new instance. */
    public OptionXmlAdapter() {}

    /** JAXB-mapped element for {@link Option}. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OptionElement {
        /** The wrapped value; {@code null} represents {@code Option.none()}. */
        @XmlElement(name = "value")
        public @Nullable String value;

        /** JAXB-required no-arg constructor. */
        public OptionElement() {}
    }

    @Override
    public Option<?> unmarshal(@Nullable OptionElement v) throws Exception {
        if (v == null || v.value == null) {
            return Option.none();
        }
        return Option.some(v.value);
    }

    @Override
    public @Nullable OptionElement marshal(@Nullable Option<?> v) throws Exception {
        if (v == null || !v.isDefined()) {
            return null;
        }
        var element = new OptionElement();
        element.value = String.valueOf(v.get());
        return element;
    }
}
