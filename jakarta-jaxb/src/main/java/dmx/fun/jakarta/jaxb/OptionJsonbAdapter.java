package dmx.fun.jakarta.jaxb;

import dmx.fun.Option;
import jakarta.json.bind.adapter.JsonbAdapter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Jakarta JSON-B adapter for {@link Option}.
 *
 * <p>JSON shapes:
 * <ul>
 *   <li>{@code Option.some(v)} ↔ {@code {"value": v}}</li>
 *   <li>{@code Option.none()} ↔ {@code {}}</li>
 * </ul>
 */
@NullMarked
public final class OptionJsonbAdapter implements JsonbAdapter<Option<?>, Map<String, Object>> {

    /** Creates a new instance. */
    public OptionJsonbAdapter() {}

    @Override
    public Map<String, Object> adaptToJson(Option<?> obj) throws Exception {
        if (obj.isDefined()) {
            var map = new LinkedHashMap<String, Object>();
            map.put("value", obj.get());
            return map;
        }
        return Map.of();
    }

    @Override
    public Option<?> adaptFromJson(Map<String, Object> obj) throws Exception {
        var value = obj.get("value");
        if (value != null) {
            return Option.some(value);
        }
        return Option.none();
    }
}
