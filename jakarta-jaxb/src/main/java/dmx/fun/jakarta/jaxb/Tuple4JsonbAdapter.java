package dmx.fun.jakarta.jaxb;

import dmx.fun.Tuple4;
import jakarta.json.bind.adapter.JsonbAdapter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Jakarta JSON-B adapter for {@link Tuple4}.
 *
 * <p>JSON shape: {@code {"_1": a, "_2": b, "_3": c, "_4": d}}
 */
@NullMarked
public final class Tuple4JsonbAdapter implements JsonbAdapter<Tuple4<?, ?, ?, ?>, Map<String, Object>> {

    @Override
    public Map<String, Object> adaptToJson(Tuple4<?, ?, ?, ?> obj) throws Exception {
        var map = new LinkedHashMap<String, Object>();
        map.put("_1", obj._1());
        map.put("_2", obj._2());
        map.put("_3", obj._3());
        map.put("_4", obj._4());
        return map;
    }

    @Override
    public Tuple4<?, ?, ?, ?> adaptFromJson(Map<String, Object> obj) throws Exception {
        return Tuple4.of(obj.get("_1"), obj.get("_2"), obj.get("_3"), obj.get("_4"));
    }
}
