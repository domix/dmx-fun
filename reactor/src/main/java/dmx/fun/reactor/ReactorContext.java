package dmx.fun.reactor;

import dmx.fun.Option;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import reactor.util.context.ContextView;

/**
 * Bridges Reactor's {@link ContextView} with dmx-fun's {@link Option}, so reading a
 * key that may be absent yields an {@code Option} instead of throwing.
 *
 * <p>Reactor's {@link ContextView#get(Object)} throws when a key is missing; this
 * helper models the absence explicitly as {@link Option#none()}. It introduces no
 * hidden global state — the {@code ContextView} is always passed in by the caller
 * (typically obtained from {@code Mono.deferContextual}).
 *
 * <p>{@link ContextView} was introduced in Reactor 3.4; these helpers therefore
 * require Reactor 3.4 or later, even though the {@link ReactorFun} conversions work
 * on earlier 3.x releases.
 */
@NullMarked
public final class ReactorContext {

    private ReactorContext() {
    }

    /**
     * Reads {@code key} from a Reactor {@link ContextView} as an {@link Option}:
     * {@link Option#some(Object)} when present, {@link Option#none()} when absent.
     *
     * @param context the Reactor context view to read from
     * @param key     the context key
     * @param <T>     the value type stored under the key
     * @return the value as an {@link Option}
     */
    public static <T> Option<T> getOrNone(ContextView context, Object key) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(key, "key");
        Optional<T> value = context.getOrEmpty(key);
        return value.map(Option::some).orElseGet(Option::none);
    }
}
