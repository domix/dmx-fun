package dmx.fun.jakarta.jaxb;

import dmx.fun.Either;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * JAXB {@link XmlAdapter} for {@link Either}.
 *
 * <p>XML shapes:
 * <pre>{@code
 * <!-- Either.right("yes") -->
 * <field><right>yes</right></field>
 *
 * <!-- Either.left("no") -->
 * <field><left>no</left></field>
 * }</pre>
 *
 * <p>Usage:
 * <pre>{@code
 * @XmlJavaTypeAdapter(EitherXmlAdapter.class)
 * Either<String, String> decision;
 * }</pre>
 */
@NullMarked
public final class EitherXmlAdapter extends XmlAdapter<EitherXmlAdapter.EitherElement, Either<?, ?>> {

    /** Creates a new instance. */
    public EitherXmlAdapter() {}

    /** JAXB-mapped element for {@link Either}. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class EitherElement {
        /** Present when the value is {@code Left}; {@code null} otherwise. */
        @XmlElement(name = "left")
        public @Nullable String left;
        /** Present when the value is {@code Right}; {@code null} otherwise. */
        @XmlElement(name = "right")
        public @Nullable String right;

        /** JAXB-required no-arg constructor. */
        public EitherElement() {}
    }

    @Override
    public Either<?, ?> unmarshal(@Nullable EitherElement v) throws Exception {
        if (v == null) {
            throw new IllegalArgumentException("Cannot deserialize null XML element as Either");
        }
        if (v.left != null && v.right != null) {
            throw new IllegalArgumentException("Ambiguous Either XML: both <left> and <right> children present");
        }
        if (v.right != null) {
            return Either.right(v.right);
        }
        if (v.left != null) {
            return Either.left(v.left);
        }
        throw new IllegalArgumentException("Either XML element must have exactly one <left> or <right> child");
    }

    @Override
    public @Nullable EitherElement marshal(@Nullable Either<?, ?> v) throws Exception {
        if (v == null) {
            return null;
        }
        var element = new EitherElement();
        if (v.isRight()) {
            element.right = String.valueOf(v.getRight());
        } else {
            element.left = String.valueOf(v.getLeft());
        }
        return element;
    }
}
