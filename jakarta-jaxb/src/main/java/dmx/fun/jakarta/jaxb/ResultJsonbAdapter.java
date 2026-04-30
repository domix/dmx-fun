package dmx.fun.jakarta.jaxb;

import dmx.fun.Result;
import jakarta.json.bind.adapter.JsonbAdapter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Jakarta JSON-B adapter for {@link Result}.
 *
 * <p>JSON shapes:
 * <ul>
 *   <li>{@code Result.ok(v)} ↔ {@code {"ok": v}}</li>
 *   <li>{@code Result.err(e)} ↔ {@code {"err": e}}</li>
 * </ul>
 */
@NullMarked
public final class ResultJsonbAdapter implements JsonbAdapter<Result<?, ?>, Map<String, Object>> {

    @Override
    public Map<String, Object> adaptToJson(Result<?, ?> obj) throws Exception {
        var map = new LinkedHashMap<String, Object>();
        if (obj.isOk()) {
            map.put("ok", obj.get());
        } else {
            map.put("err", obj.getError());
        }
        return map;
    }

    @Override
    public Result<?, ?> adaptFromJson(Map<String, Object> obj) throws Exception {
        if (obj.containsKey("ok")) {
            return Result.ok(obj.get("ok"));
        }
        if (obj.containsKey("err")) {
            return Result.err(obj.get("err"));
        }
        throw new IllegalArgumentException("Expected JSON object with 'ok' or 'err' key, got: " + obj.keySet());
    }
}
