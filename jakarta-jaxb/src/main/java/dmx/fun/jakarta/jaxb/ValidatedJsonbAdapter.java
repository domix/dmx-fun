package dmx.fun.jakarta.jaxb;

import dmx.fun.Validated;
import jakarta.json.bind.adapter.JsonbAdapter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Jakarta JSON-B adapter for {@link Validated}.
 *
 * <p>JSON shapes:
 * <ul>
 *   <li>{@code Validated.valid(a)} ↔ {@code {"valid": a}}</li>
 *   <li>{@code Validated.invalid(e)} ↔ {@code {"invalid": e}}</li>
 * </ul>
 */
@NullMarked
public final class ValidatedJsonbAdapter implements JsonbAdapter<Validated<?, ?>, Map<String, Object>> {

    @Override
    public Map<String, Object> adaptToJson(Validated<?, ?> obj) throws Exception {
        var map = new LinkedHashMap<String, Object>();
        if (obj.isValid()) {
            map.put("valid", obj.get());
        } else {
            map.put("invalid", obj.getError());
        }
        return map;
    }

    @Override
    public Validated<?, ?> adaptFromJson(Map<String, Object> obj) throws Exception {
        if (obj.containsKey("valid")) {
            return Validated.valid(obj.get("valid"));
        }
        if (obj.containsKey("invalid")) {
            return Validated.invalid(obj.get("invalid"));
        }
        throw new IllegalArgumentException("Expected JSON object with 'valid' or 'invalid' key, got: " + obj.keySet());
    }
}
