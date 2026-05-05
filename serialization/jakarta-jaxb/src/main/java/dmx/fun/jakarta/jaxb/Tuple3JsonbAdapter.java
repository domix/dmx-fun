package dmx.fun.jakarta.jaxb;

import dmx.fun.Tuple3;
import jakarta.json.bind.adapter.JsonbAdapter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Jakarta JSON-B adapter for {@link Tuple3}.
 *
 * <p>JSON shape: {@code {"_1": a, "_2": b, "_3": c}}
 */
@NullMarked
public final class Tuple3JsonbAdapter implements JsonbAdapter<Tuple3<?, ?, ?>, Map<String, Object>> {

    /** Creates a new instance. */
    public Tuple3JsonbAdapter() {}

    @Override
    public Map<String, Object> adaptToJson(Tuple3<?, ?, ?> obj) throws Exception {
        var map = new LinkedHashMap<String, Object>();
        map.put("_1", obj._1());
        map.put("_2", obj._2());
        map.put("_3", obj._3());
        return map;
    }

    @Override
    public Tuple3<?, ?, ?> adaptFromJson(Map<String, Object> obj) throws Exception {
        return Tuple3.of(obj.get("_1"), obj.get("_2"), obj.get("_3"));
    }
}
