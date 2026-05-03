package dmx.fun.jakarta.jaxb;

import dmx.fun.Tuple2;
import jakarta.json.bind.adapter.JsonbAdapter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Jakarta JSON-B adapter for {@link Tuple2}.
 *
 * <p>JSON shape: {@code {"_1": a, "_2": b}}
 */
@NullMarked
public final class Tuple2JsonbAdapter implements JsonbAdapter<Tuple2<?, ?>, Map<String, Object>> {

    /** Creates a new instance. */
    public Tuple2JsonbAdapter() {}

    @Override
    public Map<String, Object> adaptToJson(Tuple2<?, ?> obj) throws Exception {
        var map = new LinkedHashMap<String, Object>();
        map.put("_1", obj._1());
        map.put("_2", obj._2());
        return map;
    }

    @Override
    public Tuple2<?, ?> adaptFromJson(Map<String, Object> obj) throws Exception {
        if (!obj.containsKey("_1") || !obj.containsKey("_2")) {
            throw new IllegalArgumentException("Malformed Tuple2 JSON: missing '_1' or '_2'; got: " + obj.keySet());
        }
        return Tuple2.of(obj.get("_1"), obj.get("_2"));
    }
}
