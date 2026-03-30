/**
 * Functional programming constructions for Java.
 *
 * <p>Provides the core monadic types:
 * <ul>
 *   <li>{@link codes.domix.fun.Option} — represents an optional value</li>
 *   <li>{@link codes.domix.fun.Result} — represents a value or an error</li>
 *   <li>{@link codes.domix.fun.Try}    — represents a computation that may throw</li>
 *   <li>{@link codes.domix.fun.Tuple2}     — an immutable pair of two values</li>
 *   <li>{@link codes.domix.fun.Validated} — error-accumulating validation type</li>
 *   <li>{@link codes.domix.fun.CheckedSupplier} — supplier that may throw checked exceptions</li>
 *   <li>{@link codes.domix.fun.CheckedRunnable}  — runnable that may throw checked exceptions</li>
 *   <li>{@link codes.domix.fun.CheckedFunction}  — function that may throw checked exceptions</li>
 *   <li>{@link codes.domix.fun.CheckedConsumer}  — consumer that may throw checked exceptions</li>
 *   <li>{@link codes.domix.fun.Tuple3}       — immutable triple of three values</li>
 *   <li>{@link codes.domix.fun.Tuple4}       — immutable quadruple of four values</li>
 *   <li>{@link codes.domix.fun.TriFunction}  — function accepting three arguments</li>
 *   <li>{@link codes.domix.fun.QuadFunction} — function accepting four arguments</li>
 *   <li>{@link codes.domix.fun.Lazy}         — lazily evaluated, memoized value</li>
 * </ul>
 *
 * <p>The entire module is {@code @NullMarked}: all API types are non-null by default.
 */
module codes.domix.fun {
    requires org.jspecify;

    exports codes.domix.fun;
}
