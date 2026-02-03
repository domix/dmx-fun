package codes.domix.fun;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Result<String, String> errNull = Result.err(null);
        assertEquals(Option.none(), errNull.toOption());
    }

    @Test
    void result_toTry_shouldMapErrToThrowable() {
        Result<Integer, String> err = Result.err("boom");
        Try<Integer> t = err.toTry(RuntimeException::new);

        assertTrue(t.isFailure());
        assertEquals("boom", t.getCause().getMessage());
    }

    @Test
    void try_toResult_shouldMapErrToThrowable() {
        Try<Integer> boom = Result.ok(2)
            .toTry(o -> new RuntimeException("boom"));
        assertTrue(boom.isSuccess());
    }

    @Test
    void result_fromTry() {
        var success = Result.fromTry(Try.success(10));
        assertEquals(Result.ok(10), success);
    }

    @Test
    void result_fromOption() {
        Result<Integer, String> someValue = Result.fromOption(Option.some(10), "missing");
        Result<Object, String> missingOption = Result.fromOption(Option.none(), "missing");
        assertEquals(Result.ok(10), someValue);
        assertEquals(Result.err("missing"), missingOption);
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

    @Test
    void option_fromTry() {
        var success = Option.fromTry(Try.success(10));
        assertEquals(Option.some(10), success);
        var failure = Option.fromTry(Try.failure(new IllegalStateException("boom")));
        assertEquals(Option.none(), failure);
    }

    @Test
    void option_toResult() {
        var success = Option.toResult(Option.some(10), "missing");
        assertEquals(Result.ok(10), success);
        var failure = Option.toResult(Option.none(), "missing");
        assertEquals(Result.err("missing"), failure);
    }

    @Test
    void option_toTry() {
        var success = Option.toTry(Option.some(10), () -> new RuntimeException("boom"));
        assertTrue(success.isSuccess());
        assertEquals(10, success.get());
        var failure = Option.toTry(Option.none(), () -> new RuntimeException("boom"));
        assertTrue(failure.isFailure());
    }
}
