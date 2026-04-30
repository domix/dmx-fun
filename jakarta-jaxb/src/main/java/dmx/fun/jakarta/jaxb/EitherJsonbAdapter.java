package dmx.fun.jakarta.jaxb;

import dmx.fun.Either;
import jakarta.json.bind.adapter.JsonbAdapter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Jakarta JSON-B adapter for {@link Either}.
 *
 * <p>JSON shapes:
 * <ul>
 *   <li>{@code Either.right(v)} ↔ {@code {"right": v}}</li>
 *   <li>{@code Either.left(e)} ↔ {@code {"left": e}}</li>
 * </ul>
 */
@NullMarked
public final class EitherJsonbAdapter implements JsonbAdapter<Either<?, ?>, Map<String, Object>> {

    /** Creates a new instance. */
    public EitherJsonbAdapter() {}

    @Override
    public Map<String, Object> adaptToJson(Either<?, ?> obj) throws Exception {
        var map = new LinkedHashMap<String, Object>();
        if (obj.isRight()) {
            map.put("right", obj.getRight());
        } else {
            map.put("left", obj.getLeft());
        }
        return map;
    }

    @Override
    public Either<?, ?> adaptFromJson(Map<String, Object> obj) throws Exception {
        if (obj.containsKey("right")) {
            return Either.right(obj.get("right"));
        }
        if (obj.containsKey("left")) {
            return Either.left(obj.get("left"));
        }
        throw new IllegalArgumentException("Expected JSON object with 'right' or 'left' key, got: " + obj.keySet());
    }
}
