package dmx.fun.jakarta.jaxb;

import dmx.fun.Try;
import jakarta.json.bind.adapter.JsonbAdapter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Jakarta JSON-B adapter for {@link Try}.
 *
 * <p>JSON shapes:
 * <ul>
 *   <li>{@code Try.success(v)} ↔ {@code {"value": v}}</li>
 *   <li>{@code Try.failure(ex)} ↔ {@code {"error": "message"}}</li>
 * </ul>
 */
@NullMarked
public final class TryJsonbAdapter implements JsonbAdapter<Try<?>, Map<String, Object>> {

    /** Creates a new instance. */
    public TryJsonbAdapter() {}

    @Override
    public Map<String, Object> adaptToJson(Try<?> obj) throws Exception {
        var map = new LinkedHashMap<String, Object>();
        if (obj.isSuccess()) {
            map.put("value", obj.get());
        } else {
            var message = obj.getCause().getMessage();
            map.put("error", message != null ? message : obj.getCause().getClass().getSimpleName());
        }
        return map;
    }

    @Override
    public Try<?> adaptFromJson(Map<String, Object> obj) throws Exception {
        boolean hasValue = obj.containsKey("value");
        boolean hasError = obj.containsKey("error");
        if (hasValue && hasError) {
            throw new IllegalArgumentException("Ambiguous Try JSON: both 'value' and 'error' keys present");
        }
        if (!hasValue && !hasError) {
            throw new IllegalArgumentException("Malformed Try JSON: must contain 'value' or 'error', got: " + obj.keySet());
        }
        if (hasError) {
            var msg = obj.get("error");
            return Try.failure(new RuntimeException(msg != null ? msg.toString() : null));
        }
        return Try.success(obj.get("value"));
    }
}
