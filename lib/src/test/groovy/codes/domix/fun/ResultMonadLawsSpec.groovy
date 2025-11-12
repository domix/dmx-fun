package codes.domix.fun

import spock.lang.Specification

import java.util.function.Function

/**
 * Specification that verifies the Result type adheres to the Monad and Functor laws.
 * <p>
 * This test suite ensures that the {@link Result} type correctly implements the mathematical
 * properties required for a proper monad and functor, which guarantees predictable and composable behavior.
 * <p>
 * <h3>Monad Laws Verified:</h3>
 * <ul>
 *   <li><b>Left Identity:</b> {@code return a >>= f} should equal {@code f a}</li>
 *   <li><b>Right Identity:</b> {@code m >>= return} should equal {@code m}</li>
 *   <li><b>Associativity:</b> {@code (m >>= f) >>= g} should equal {@code m >>= (x -> f x >>= g)}</li>
 * </ul>
 * <p>
 * <h3>Functor Laws Verified:</h3>
 * <ul>
 *   <li><b>Identity:</b> {@code map(id)} should equal {@code id}</li>
 *   <li><b>Composition:</b> {@code map(f . g)} should equal {@code map(f) . map(g)}</li>
 * </ul>
 * <p>
 * These laws ensure that {@code flatMap} and {@code map} operations can be safely refactored,
 * nested, and composed without changing the semantic meaning of the code.
 *
 * @see Result
 */
class ResultMonadLawsSpec extends Specification {

    // f: x -> Ok(x * 2)
    private final Function<Integer, Result<Integer, String>> f =
        (Integer x) -> Result.ok(x * 2)

    // g: x -> (x % 2 == 0 ? Ok(x + 1) : Err("odd"))
    private final Function<Integer, Result<Integer, String>> g =
        (Integer x) -> (x % 2 == 0) ? Result.ok(x + 1) : Result.err("odd")

    // h: x -> (x > 100 ? Err("too-big") : Ok(x - 3))
    private final Function<Integer, Result<Integer, String>> h =
        (Integer x) -> (x > 100) ? Result.err("too-big") : Result.ok(x - 3)

    def "Monad - Left identity: return a >>= f == f a"() {
        given:
            Integer a = 10

        when:
            def left = Result
                .ok(a)
                .flatMap(f)
            def right = f.apply(a)

        then:
            left == right
    }

    def "Monad - Right identity: m >>= return == m (for Ok)"() {
        given:
            Result<Integer, String> m = Result.ok(42)

        when:
            def lhs = m
                .flatMap((Integer x) -> Result.ok(x))  // bind with return

        then:
            lhs == m
    }

    def "Monad - Right identity: m >>= return == m (for Err)"() {
        given:
            Result<Integer, String> m = Result.err("boom")

        when:
            def lhs = m
                .flatMap((Integer x) -> Result.ok(x))

        then:
            lhs == m
    }

    def "Monad - Associativity: (m >>= f) >>= g == m >>= (x -> f x >>= g) (Ok)"() {
        given:
            Result<Integer, String> m = Result.ok(5)

        when:
            def left = m
                .flatMap(f)
                .flatMap(g)
            def right = m
                .flatMap((Integer x) -> f.apply(x)
                    .flatMap(g))

        then:
            left == right
    }

    def "Monad - Associativity: (m >>= f) >>= g == m >>= (x -> f x >>= g) (Err)"() {
        given:
            Result<Integer, String> m = Result.err("e")

        when:
            def left = m
                .flatMap(f).flatMap(g)
            def right = m
                .flatMap((Integer x) -> f.apply(x)
                    .flatMap(g))

        then:
            left == right
    }

    // --- (Optional but useful) Functor Laws on map ---

    def "Functor - Identity: map(id) == id"() {
        given:
            def id = (Integer x) -> x

        expect:
            Result.ok(7)
                .map(id) == Result.ok(7)
            Result.err("x")
                .<Integer> map(id) == Result.err("x")
    }

    def "Functor - Composition: map(f . g) == map(f) . map(g)"() {
        given:
            Function<Integer, Integer> g0 = (Integer x) -> x + 3
            Function<Integer, Integer> f0 = (Integer x) -> x * 2
            Function<Integer, Integer> comp = (Integer x) -> f0.apply(g0.apply(x))

        expect:
            Result.ok(10)
                .map(comp) == Result.ok(10)
                .map(g0)
                .map(f0)
            Result.err("nope")
                .<Integer> map(comp) == Result.err("nope")
    }

    // (Optional extra) Compatibility flatMap-map:
    // map(f) == flatMap(x -> ok(f(x)))
    def "map(f) == flatMap(x -> ok(f(x)))"() {
        given:
            Function<Integer, Integer> f0 = (Integer x) -> x + 1

        expect:
            Result.ok(1)
                .map(f0) == Result.ok(1)
                .flatMap((Integer x) -> Result.ok(f0.apply(x)))
            Result.err("e")
                .<Integer> map(f0) == Result.err("e")
                .flatMap((Integer x) -> Result.ok(f0.apply(x)))
    }

    // (Optional extra) Associativity with third function
    def "Monad - Associativity with third function h"() {
        given:
            Result<Integer, String> m = Result.ok(20)

        when:
            def left = m
                .flatMap(f)
                .flatMap(h)
            def right = m
                .flatMap((Integer x) -> f.apply(x)
                    .flatMap(h))

        then:
            left == right
    }
}
