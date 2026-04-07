package dmx.fun;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link Result} adheres to the Monad and Functor laws.
 *
 * <h3>Monad Laws:</h3>
 * <ul>
 *   <li><b>Left identity:</b>  {@code return(a).flatMap(f)  == f(a)}</li>
 *   <li><b>Right identity:</b> {@code m.flatMap(return)     == m}</li>
 *   <li><b>Associativity:</b>  {@code (m.flatMap(f)).flatMap(g) == m.flatMap(x -> f(x).flatMap(g))}</li>
 * </ul>
 *
 * <h3>Functor Laws:</h3>
 * <ul>
 *   <li><b>Identity:</b>    {@code map(id) == id}</li>
 *   <li><b>Composition:</b> {@code map(f ∘ g) == map(f).map(g)}</li>
 * </ul>
 */
class ResultMonadLawsTest {

    // f: x -> Ok(x * 2)
    private final Function<Integer, Result<Integer, String>> f =
        x -> Result.ok(x * 2);

    // g: x -> (x % 2 == 0 ? Ok(x + 1) : Err("odd"))
    private final Function<Integer, Result<Integer, String>> g =
        x -> (x % 2 == 0) ? Result.ok(x + 1) : Result.err("odd");

    // h: x -> (x > 100 ? Err("too-big") : Ok(x - 3))
    private final Function<Integer, Result<Integer, String>> h =
        x -> (x > 100) ? Result.err("too-big") : Result.ok(x - 3);

    // ---------- Monad — Left identity ----------

    @Test
    void monad_leftIdentity() {
        int a = 10;
        Result<Integer, String> left  = Result.<Integer, String>ok(a).flatMap(f);
        Result<Integer, String> right = f.apply(a);
        assertThat(left).isEqualTo(right);
    }

    // ---------- Monad — Right identity ----------

    @Test
    void monad_rightIdentity_ok() {
        Result<Integer, String> m   = Result.ok(42);
        Result<Integer, String> lhs = m.flatMap(x -> Result.ok(x));
        assertThat(lhs).isEqualTo(m);
    }

    @Test
    void monad_rightIdentity_err() {
        Result<Integer, String> m   = Result.err("boom");
        Result<Integer, String> lhs = m.flatMap(x -> Result.ok(x));
        assertThat(lhs).isEqualTo(m);
    }

    // ---------- Monad — Associativity ----------

    @Test
    void monad_associativity_ok() {
        Result<Integer, String> m     = Result.ok(5);
        Result<Integer, String> left  = m.flatMap(f).flatMap(g);
        Result<Integer, String> right = m.flatMap(x -> f.apply(x).flatMap(g));
        assertThat(left).isEqualTo(right);
    }

    @Test
    void monad_associativity_err() {
        Result<Integer, String> m     = Result.err("e");
        Result<Integer, String> left  = m.flatMap(f).flatMap(g);
        Result<Integer, String> right = m.flatMap(x -> f.apply(x).flatMap(g));
        assertThat(left).isEqualTo(right);
    }

    @Test
    void monad_associativity_withThirdFunction() {
        Result<Integer, String> m     = Result.ok(20);
        Result<Integer, String> left  = m.flatMap(f).flatMap(h);
        Result<Integer, String> right = m.flatMap(x -> f.apply(x).flatMap(h));
        assertThat(left).isEqualTo(right);
    }

    // ---------- Functor — Identity ----------

    @Test
    void functor_identity() {
        Function<Integer, Integer> id = x -> x;
        assertThat(Result.ok(7).map(id)).isEqualTo(Result.ok(7));
        assertThat(Result.<Integer, String>err("x").map(id)).isEqualTo(Result.err("x"));
    }

    // ---------- Functor — Composition ----------

    @Test
    void functor_composition() {
        Function<Integer, Integer> g0   = x -> x + 3;
        Function<Integer, Integer> f0   = x -> x * 2;
        Function<Integer, Integer> comp = x -> f0.apply(g0.apply(x));

        assertThat(Result.<Integer, String>ok(10).map(comp))
            .isEqualTo(Result.<Integer, String>ok(10).map(g0).map(f0));
        assertThat(Result.<Integer, String>err("nope").map(comp))
            .isEqualTo(Result.<Integer, String>err("nope").map(g0).map(f0));
    }

    // ---------- Compatibility: map(f) == flatMap(x -> ok(f(x))) ----------

    @Test
    void map_equalsTo_flatMapWithOk() {
        Function<Integer, Integer> f0 = x -> x + 1;

        assertThat(Result.<Integer, String>ok(1).map(f0))
            .isEqualTo(Result.<Integer, String>ok(1).flatMap(x -> Result.ok(f0.apply(x))));
        assertThat(Result.<Integer, String>err("e").map(f0))
            .isEqualTo(Result.<Integer, String>err("e").flatMap(x -> Result.ok(f0.apply(x))));
    }
}
