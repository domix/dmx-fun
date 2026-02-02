package codes.domix.fun;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.BiFunction;

/**
 * A monadic type that represents an optional value: either Some(value present)
 * or None(no value).
 *
 * @param <Value> the wrapped value type
 */
public sealed interface Option<Value> permits Option.Some, Option.None {

    record Some<Value>(Value value) implements Option<Value> {
        public Some {
            Objects.requireNonNull(value, "Option.Some cannot hold null. Use Option.none() or Option.ofNullable().");
        }
    }

    record None<Value>() implements Option<Value> {
    }

    record Tuple2<A, B>(A _1, B _2) {}

    // ---------- Factories ----------

    static <V> Option<V> some(V value) {
        return value == null ? none() : new Some<>(value);
    }

    static <V> Option<V> none() {
        return new None<>();
    }

    static <V> Option<V> ofNullable(V value) {
        return some(value);
    }

    static <V> Option<V> fromOptional(Optional<V> optional) {
        return optional.map(Option::some).orElseGet(Option::none);
    }

    // ---------- Predicates ----------

    default boolean isDefined() {
        return this instanceof Some<?>;
    }

    default boolean isEmpty() {
        return this instanceof None<?>;
    }

    // ---------- Accessors ----------

    default Value get() {
        return switch (this) {
            case Some<Value> s -> s.value();
            case None<Value> _ -> throw new NoSuchElementException("No value present. Option is None.");
        };
    }

    default Value getOrElse(Value fallback) {
        return this instanceof Some<Value>(Value v) ? v : fallback;
    }

    default Value getOrElseGet(Supplier<? extends Value> fallbackSupplier) {
        return this instanceof Some<Value>(Value v) ? v : fallbackSupplier.get();
    }

    default Value getOrNull() {
        return this instanceof Some<Value>(Value v) ? v : null;
    }

    default Value getOrThrow(Supplier<? extends RuntimeException> exceptionSupplier) {
        return switch (this) {
            case Some<Value> s -> s.value();
            case None<Value> _ -> throw exceptionSupplier.get();
        };
    }

    // ---------- Monadic operations ----------

    default <NewValue> Option<NewValue> map(Function<? super Value, ? extends NewValue> mapper) {
        return switch (this) {
            case Some<Value> s -> Option.ofNullable(mapper.apply(s.value()));
            case None<Value> _ -> Option.none();
        };
    }

    default <NewValue> Option<NewValue> flatMap(Function<? super Value, Option<NewValue>> mapper) {
        return switch (this) {
            case Some<Value> s -> Objects.requireNonNull(mapper.apply(s.value()),
                "flatMap mapper must not return null");
            case None<Value> _ -> Option.none();
        };
    }

    default Option<Value> filter(Predicate<? super Value> predicate) {
        return switch (this) {
            case Some<Value> s -> predicate.test(s.value()) ? this : Option.none();
            case None<Value> _ -> this;
        };
    }

    default Option<Value> peek(Consumer<? super Value> action) {
        if (this instanceof Some<Value>(Value v)) action.accept(v);
        return this;
    }

    default <Folded> Folded fold(Supplier<? extends Folded> onNone,
                                 Function<? super Value, ? extends Folded> onSome) {
        return switch (this) {
            case Some<Value> s -> onSome.apply(s.value());
            case None<Value> _ -> onNone.get();
        };
    }

    default void match(Runnable onNone, Consumer<? super Value> onSome) {
        switch (this) {
            case Some<Value> s -> onSome.accept(s.value());
            case None<Value> _ -> onNone.run();
        }
    }

    // ---------- Stream interoperability ----------

    /**
     * Java 9+ Optional has stream(); this mirrors that.
     * - Some(v) -> Stream.of(v)
     * - None    -> Stream.empty()
     */
    default Stream<Value> stream() {
        return switch (this) {
            case Some<Value> s -> Stream.of(s.value());
            case None<Value> _ -> Stream.empty();
        };
    }

    default Optional<Value> toOptional() {
        return switch (this) {
            case Some<Value> s -> Optional.of(s.value());
            case None<Value> _ -> Optional.empty();
        };
    }

    /**
     * Collects only present values from a stream of Options.
     * Equivalent to stream.flatMap(Option::stream).collect(toList()).
     */
    static <V> List<V> collectPresent(Stream<Option<V>> options) {
        return options.flatMap(Option::stream).collect(Collectors.toList());
    }

    /**
     * Collector that keeps present values from a stream of Option<T>.
     */
    static <V> Collector<Option<? extends V>, ?, List<V>> presentValuesToList() {
        return Collectors.flatMapping(Option::stream, Collectors.toList());
    }

    // ---------- sequence / traverse ----------

    /**
     * sequence: List<Option<T>> -> Option<List<T>>
     * Returns None if any element is None; otherwise Some(list of all values).
     */
    static <V> Option<List<V>> sequence(Iterable<Option<V>> options) {
        Objects.requireNonNull(options, "options");
        ArrayList<V> out = new ArrayList<>();
        for (Option<V> opt : options) {
            if (opt == null) {
                throw new NullPointerException("options contains null element (use Option.none() instead)");
            }
            if (opt instanceof None<V>) {
                return Option.none();
            }
            out.add(((Some<V>) opt).value());
        }
        return Option.some(List.copyOf(out));
    }

    /**
     * sequence: Stream<Option<T>> -> Option<List<T>>
     * Short-circuits by inspection (imperative loop over iterator).
     */
    static <V> Option<List<V>> sequence(Stream<Option<V>> options) {
        Objects.requireNonNull(options, "options");
        ArrayList<V> out = new ArrayList<>();
        Iterator<Option<V>> it = options.iterator();
        while (it.hasNext()) {
            Option<V> opt = it.next();
            if (opt == null) {
                throw new NullPointerException("options contains null element (use Option.none() instead)");
            }
            if (opt instanceof None<V>) {
                return Option.none();
            }
            out.add(((Some<V>) opt).value());
        }
        return Option.some(List.copyOf(out));
    }

    /**
     * traverse: Iterable<A> -> (A -> Option<B>) -> Option<List<B>>
     * Applies mapper to each element; returns None if any mapped result is None.
     */
    static <A, B> Option<List<B>> traverse(Iterable<A> values, Function<? super A, Option<B>> mapper) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mapper, "mapper");
        ArrayList<B> out = new ArrayList<>();
        for (A a : values) {
            Option<B> ob = Objects.requireNonNull(mapper.apply(a), "traverse mapper must not return null");
            if (ob instanceof None<B>) {
                return Option.none();
            }
            out.add(((Some<B>) ob).value());
        }
        return Option.some(List.copyOf(out));
    }

    /**
     * traverse: Stream<A> -> (A -> Option<B>) -> Option<List<B>>
     */
    static <A, B> Option<List<B>> traverse(Stream<A> values, Function<? super A, Option<B>> mapper) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mapper, "mapper");
        ArrayList<B> out = new ArrayList<>();
        Iterator<A> it = values.iterator();
        while (it.hasNext()) {
            A a = it.next();
            Option<B> ob = Objects.requireNonNull(mapper.apply(a), "traverse mapper must not return null");
            if (ob instanceof None<B>) {
                return Option.none();
            }
            out.add(((Some<B>) ob).value());
        }
        return Option.some(List.copyOf(out));
    }

    // ---------- Bridges (optional, but handy with your Result/Try) ----------

    default <TError> Result<Value, TError> toResult(TError errorIfNone) {
        return switch (this) {
            case Some<Value> s -> Result.ok(s.value());
            case None<Value> _ -> Result.err(errorIfNone);
        };
    }

    default Try<Value> toTry(Supplier<? extends Throwable> exceptionSupplier) {
        return switch (this) {
            case Some<Value> s -> Try.success(s.value());
            case None<Value> _ -> Try.failure(exceptionSupplier.get());
        };
    }

    // ---------- zip / map2 ----------

    default <B> Option<Tuple2<Value, B>> zip(Option<? extends B> other) {
        Objects.requireNonNull(other, "other");
        return zip(this, other);
    }

    default <B, R> Option<R> zipWith(Option<? extends B> other,
                                     BiFunction<? super Value, ? super B, ? extends R> combiner) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(combiner, "combiner");
        return map2(this, other, combiner);
    }

    /**
     * zip: Option<A> + Option<B> -> Option<Tuple2<A,B>>
     */
    static <A, B> Option<Tuple2<A, B>> zip(Option<? extends A> a, Option<? extends B> b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");

        if (a instanceof None<?> || b instanceof None<?>) return Option.none();

        A av = ((Some<? extends A>) a).value();
        B bv = ((Some<? extends B>) b).value();
        return Option.some(new Tuple2<>(av, bv));
    }

    /**
     * map2: Option<A> + Option<B> + (A,B)->R -> Option<R>
     * Si combiner regresa null, lo convertimos a None (igual que map()).
     */
    static <A, B, R> Option<R> map2(Option<? extends A> a,
                                    Option<? extends B> b,
                                    BiFunction<? super A, ? super B, ? extends R> combiner) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(combiner, "combiner");

        if (a instanceof None<?> || b instanceof None<?>) return Option.none();

        A av = ((Some<? extends A>) a).value();
        B bv = ((Some<? extends B>) b).value();
        return Option.ofNullable(combiner.apply(av, bv));
    }

}
