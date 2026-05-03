/**
 * Quarkus integration for dmx-fun types.
 *
 * <p>Provides CDI-based programmatic transaction support ({@link dmx.fun.quarkus.TxResult},
 * {@link dmx.fun.quarkus.TxTry}) and declarative support via interceptor bindings
 * ({@link dmx.fun.quarkus.TransactionalResult}, {@link dmx.fun.quarkus.TransactionalTry})
 * so applications can use {@code Result} and {@code Try} idiomatically without giving up
 * JTA-managed transactions via Narayana.
 *
 * <p>Quarkus is declared as {@code compileOnly} — consumers bring their own Quarkus
 * dependency. Tested against: 3.11.x, 3.21.x, 3.31.x, 3.35.x.
 */
module dmx.fun.quarkus {
    requires dmx.fun;
    requires static jakarta.cdi;
    requires static jakarta.transaction;
    requires static jakarta.interceptor;
    requires org.jspecify;

    exports dmx.fun.quarkus;
}
