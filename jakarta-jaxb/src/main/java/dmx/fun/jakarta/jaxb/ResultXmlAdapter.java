package dmx.fun.jakarta.jaxb;

import dmx.fun.Result;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * JAXB {@link XmlAdapter} for {@link Result}.
 *
 * <p>XML shapes:
 * <pre>{@code
 * <!-- Result.ok(42) -->
 * <field><ok>42</ok></field>
 *
 * <!-- Result.err("oops") -->
 * <field><err>oops</err></field>
 * }</pre>
 *
 * <p>Usage:
 * <pre>{@code
 * @XmlJavaTypeAdapter(ResultXmlAdapter.class)
 * Result<Integer, String> value;
 * }</pre>
 */
@NullMarked
public final class ResultXmlAdapter extends XmlAdapter<ResultXmlAdapter.ResultElement, Result<?, ?>> {

    /** Creates a new instance. */
    public ResultXmlAdapter() {}

    /** JAXB-mapped element for {@link Result}. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ResultElement {
        /** Present when the result is {@code Ok}; {@code null} otherwise. */
        @XmlElement(name = "ok")
        public @Nullable String ok;
        /** Present when the result is {@code Err}; {@code null} otherwise. */
        @XmlElement(name = "err")
        public @Nullable String err;

        /** JAXB-required no-arg constructor. */
        public ResultElement() {}
    }

    @Override
    public Result<?, ?> unmarshal(@Nullable ResultElement v) throws Exception {
        if (v == null) {
            throw new IllegalArgumentException("Cannot deserialize null XML element as Result");
        }
        if (v.ok != null) {
            return Result.ok(v.ok);
        }
        if (v.err != null) {
            return Result.err(v.err);
        }
        throw new IllegalArgumentException("Result XML element must have an <ok> or <err> child");
    }

    @Override
    public @Nullable ResultElement marshal(@Nullable Result<?, ?> v) throws Exception {
        if (v == null) {
            return null;
        }
        var element = new ResultElement();
        if (v.isOk()) {
            element.ok = String.valueOf(v.get());
        } else {
            element.err = String.valueOf(v.getError());
        }
        return element;
    }
}
