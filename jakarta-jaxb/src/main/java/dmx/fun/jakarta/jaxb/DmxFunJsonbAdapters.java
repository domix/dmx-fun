package dmx.fun.jakarta.jaxb;

import jakarta.json.bind.adapter.JsonbAdapter;
import org.jspecify.annotations.NullMarked;

/**
 * Registry of all dmx-fun Jakarta JSON-B adapters.
 *
 * <p>Pass {@link #all()} to {@code JsonbConfig.withAdapters()} to register
 * JSON-B support for every dmx-fun type in one step:
 *
 * <pre>{@code
 * Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
 *     .withAdapters(DmxFunJsonbAdapters.all()));
 * }</pre>
 *
 * <p>Included adapters and their JSON shapes:
 * <table border="1">
 *   <caption>JSON shapes by type</caption>
 *   <tr><th>Type</th><th>Present / success</th><th>Absent / failure</th></tr>
 *   <tr><td>{@code Option<T>}</td><td>{@code {"value":…}}</td><td>{@code {}}</td></tr>
 *   <tr><td>{@code Result<V,E>}</td><td>{@code {"ok":…}}</td><td>{@code {"err":…}}</td></tr>
 *   <tr><td>{@code Try<V>}</td><td>{@code {"value":…}}</td><td>{@code {"error":"…"}}</td></tr>
 *   <tr><td>{@code Either<L,R>}</td><td>{@code {"right":…}}</td><td>{@code {"left":…}}</td></tr>
 *   <tr><td>{@code Validated<E,A>}</td><td>{@code {"valid":…}}</td><td>{@code {"invalid":…}}</td></tr>
 *   <tr><td>{@code Tuple2<A,B>}</td><td>{@code {"_1":…,"_2":…}}</td><td>—</td></tr>
 *   <tr><td>{@code Tuple3<A,B,C>}</td><td>{@code {"_1":…,"_2":…,"_3":…}}</td><td>—</td></tr>
 *   <tr><td>{@code Tuple4<A,B,C,D>}</td><td>{@code {"_1":…,"_2":…,"_3":…,"_4":…}}</td><td>—</td></tr>
 *   <tr><td>{@code NonEmptyList<T>}</td><td>{@code [head,…tail]}</td><td>—</td></tr>
 * </table>
 */
@NullMarked
public final class DmxFunJsonbAdapters {

    private DmxFunJsonbAdapters() {}

    /**
     * Returns all dmx-fun JSON-B adapters, ready for
     * {@code JsonbConfig.withAdapters(JsonbAdapter...)}.
     *
     * @return array of all adapters
     */
    public static JsonbAdapter<?, ?>[] all() {
        return new JsonbAdapter<?, ?>[] {
            new OptionJsonbAdapter(),
            new ResultJsonbAdapter(),
            new TryJsonbAdapter(),
            new EitherJsonbAdapter(),
            new ValidatedJsonbAdapter(),
            new Tuple2JsonbAdapter(),
            new Tuple3JsonbAdapter(),
            new Tuple4JsonbAdapter(),
            new NonEmptyListJsonbAdapter()
        };
    }
}
