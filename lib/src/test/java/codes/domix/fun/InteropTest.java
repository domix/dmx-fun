package codes.domix.fun;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class InteropTest {

    @Test
    void option_toResult_and_back() {
        Option<Integer> some = Option.some(10);
        Option<Integer> none = Option.none();

        Result<Integer, String> r1 = some.toResult("missing");
        Result<Integer, String> r2 = none.toResult("missing");

        assertTrue(r1.isOk());
        assertEquals(10, r1.get());

        assertTrue(r2.isError());
        assertEquals("missing", r2.getError());

        assertEquals(some, Option.fromResult(r1));
        assertEquals(Option.none(), Option.fromResult(r2));
    }

    @Test
    void result_toOption_shouldDropNullOkValue() {
        Result<String, String> okNull = Result.ok(null);
        assertEquals(Option.none(), okNull.toOption());
    }

    @Test
    void result_toTry_shouldMapErrToThrowable() {
        Result<Integer, String> err = Result.err("boom");
        Try<Integer> t = err.toTry(RuntimeException::new);

        assertTrue(t.isFailure());
        assertEquals("boom", t.getCause().getMessage());
    }

    @Test
    void try_toOption_and_back() {
        Try<Integer> s = Try.success(10);
        Try<Integer> f = Try.failure(new IllegalStateException("x"));

        assertEquals(Option.some(10), s.toOption());
        assertEquals(Option.none(), f.toOption());

        Try<Integer> back1 = Try.fromOption(Option.some(10), () -> new RuntimeException("missing"));
        Try<Integer> back2 = Try.fromOption(Option.none(), () -> new RuntimeException("missing"));

        assertTrue(back1.isSuccess());
        assertEquals(10, back1.get());

        assertTrue(back2.isFailure());
        assertEquals("missing", back2.getCause().getMessage());
    }

    @Test
    void option_toTry_shouldBeLazy_onSome() {
        AtomicBoolean called = new AtomicBoolean(false);

        Try<Integer> t = Option.some(10).toTry(() -> {
            called.set(true);
            return new RuntimeException("nope");
        });

        assertTrue(t.isSuccess());
        assertEquals(10, t.get());
        assertFalse(called.get(), "exception supplier must not be called for Some");
    }
}
