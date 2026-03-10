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
 * </ul>
 *
 * <p>The entire module is {@code @NullMarked}: all API types are non-null by default.
 */
module codes.domix.fun {
    requires org.jspecify;

    exports codes.domix.fun;
}
