package dmx.fun.jakarta.jaxb;

import dmx.fun.NonEmptyList;
import jakarta.json.bind.adapter.JsonbAdapter;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * Jakarta JSON-B adapter for {@link NonEmptyList}.
 *
 * <p>JSON shape: a JSON array containing at least one element, e.g.
 * {@code ["a", "b", "c"]}.
 */
@NullMarked
public final class NonEmptyListJsonbAdapter implements JsonbAdapter<NonEmptyList<?>, List<Object>> {

    @Override
    public List<Object> adaptToJson(NonEmptyList<?> obj) throws Exception {
        var list = new ArrayList<>();
        list.add(obj.head());
        list.addAll(obj.tail());
        return list;
    }

    @Override
    public NonEmptyList<?> adaptFromJson(List<Object> obj) throws Exception {
        if (obj.isEmpty()) {
            throw new IllegalArgumentException("Cannot deserialize empty JSON array as NonEmptyList");
        }
        var head = obj.get(0);
        if (head == null) {
            throw new IllegalArgumentException("NonEmptyList head element must not be null");
        }
        return NonEmptyList.of(head, obj.subList(1, obj.size()));
    }
}
