/**
 * Resilience4J adapter for dmx-fun.
 *
 * <p>Wraps {@code Retry}, {@code CircuitBreaker}, {@code RateLimiter}, and {@code Bulkhead}
 * so that executions return {@link dmx.fun.Try} or {@link dmx.fun.Result} instead of
 * throwing exceptions.
 */
module dmx.fun.resilience4j {
    requires dmx.fun;
    requires static io.github.resilience4j.core;
    requires static io.github.resilience4j.retry;
    requires static io.github.resilience4j.circuitbreaker;
    requires static io.github.resilience4j.ratelimiter;
    requires static io.github.resilience4j.bulkhead;
    requires org.jspecify;

    exports dmx.fun.resilience4j;
}
