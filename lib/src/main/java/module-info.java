/**
 * Functional programming constructions for Java.
 *
 * <p>Provides the core monadic types:
 * <ul>
 *   <li>{@link dmx.fun.Option} — represents an optional value</li>
 *   <li>{@link dmx.fun.Result} — represents a value or an error</li>
 *   <li>{@link dmx.fun.Try}    — represents a computation that may throw</li>
 *   <li>{@link dmx.fun.Tuple2}     — an immutable pair of two values</li>
 *   <li>{@link dmx.fun.Validated} — error-accumulating validation type</li>
 *   <li>{@link dmx.fun.CheckedSupplier} — supplier that may throw checked exceptions</li>
 *   <li>{@link dmx.fun.CheckedRunnable}  — runnable that may throw checked exceptions</li>
 *   <li>{@link dmx.fun.CheckedFunction}  — function that may throw checked exceptions</li>
 *   <li>{@link dmx.fun.CheckedConsumer}  — consumer that may throw checked exceptions</li>
 *   <li>{@link dmx.fun.Tuple3}       — immutable triple of three values</li>
 *   <li>{@link dmx.fun.Tuple4}       — immutable quadruple of four values</li>
 *   <li>{@link dmx.fun.TriFunction}  — function accepting three arguments</li>
 *   <li>{@link dmx.fun.QuadFunction} — function accepting four arguments</li>
 *   <li>{@link dmx.fun.Lazy}         — lazily evaluated, memoized value</li>
 *   <li>{@link dmx.fun.NonEmptyList} — list guaranteed to contain at least one element</li>
 *   <li>{@link dmx.fun.Either}       — neutral disjoint union of two value types</li>
 * </ul>
 *
 * <p>The entire module is {@code @NullMarked}: all API types are non-null by default.
 */
module dmx.fun {
    requires org.jspecify;

    exports dmx.fun;
}
