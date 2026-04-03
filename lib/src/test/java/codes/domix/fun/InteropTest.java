package codes.domix.fun;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        var success = Option.some(10).toResult("missing");
        assertEquals(Result.ok(10), success);
        var failure = Option.<Integer>none().toResult("missing");
        assertEquals(Result.err("missing"), failure);
    }

    @Test
    void option_toTry() {
        var success = Option.some(10).toTry(() -> new RuntimeException("boom"));
        assertTrue(success.isSuccess());
        assertEquals(10, success.get());
        var failure = Option.<Integer>none().toTry(() -> new RuntimeException("boom"));
        assertTrue(failure.isFailure());
    }

    // ---------- Optional interop ----------

    @Test
    void result_fromOptional_present_returnsOk() {
        Result<Integer, NoSuchElementException> r = Result.fromOptional(Optional.of(42));
        assertTrue(r.isOk());
        assertEquals(42, r.get());
    }

    @Test
    void result_fromOptional_empty_returnsErrWithNoSuchElementException() {
        Result<Integer, NoSuchElementException> r = Result.fromOptional(Optional.empty());
        assertTrue(r.isError());
        assertInstanceOf(NoSuchElementException.class, r.getError());
    }

    @Test
    void result_fromOptional_null_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Result.fromOptional(null));
    }

    @Test
    void try_fromOptional_present_returnsSuccess() {
        Try<Integer> t = Try.fromOptional(Optional.of(99), () -> new RuntimeException("missing"));
        assertTrue(t.isSuccess());
        assertEquals(99, t.get());
    }

    @Test
    void try_fromOptional_empty_returnsFailureFromSupplier() {
        RuntimeException ex = new RuntimeException("missing");
        Try<Integer> t = Try.fromOptional(Optional.empty(), () -> ex);
        assertTrue(t.isFailure());
        assertEquals(ex, t.getCause());
    }

    @Test
    void try_fromOptional_supplierNotCalledWhenPresent() {
        AtomicBoolean called = new AtomicBoolean(false);
        Try<Integer> t = Try.fromOptional(Optional.of(1), () -> {
            called.set(true);
            return new RuntimeException("should not be called");
        });
        assertTrue(t.isSuccess());
        assertFalse(called.get(), "supplier must not be called when Optional is present");
    }

    @Test
    void try_fromOptional_null_throwsNPE() {
        assertThrows(NullPointerException.class,
            () -> Try.fromOptional(null, () -> new RuntimeException("x")));
    }

    @Test
    void try_fromOptional_nullSupplier_throwsNPE() {
        assertThrows(NullPointerException.class,
            () -> Try.fromOptional(Optional.empty(), null));
    }

    // ---------- Validated interop ----------

    @Nested
    class ValidatedInterop {

        @Test
        void validated_toResult_valid() {
            Validated<String, Integer> v = Validated.valid(42);
            Result<Integer, String> r = v.toResult();
            assertTrue(r.isOk());
            assertEquals(42, r.get());
        }

        @Test
        void validated_toResult_invalid() {
            Validated<String, Integer> v = Validated.invalid("oops");
            Result<Integer, String> r = v.toResult();
            assertTrue(r.isError());
            assertEquals("oops", r.getError());
        }

        @Test
        void validated_fromResult_ok() {
            Result<Integer, String> r = Result.ok(7);
            Validated<String, Integer> v = Validated.fromResult(r);
            assertTrue(v.isValid());
            assertEquals(7, v.get());
        }

        @Test
        void validated_fromResult_err() {
            Result<Integer, String> r = Result.err("bad");
            Validated<String, Integer> v = Validated.fromResult(r);
            assertTrue(v.isInvalid());
            assertEquals("bad", v.getError());
        }

        @Test
        void validated_toOption_valid() {
            Validated<String, Integer> v = Validated.valid(5);
            assertEquals(Option.some(5), v.toOption());
        }

        @Test
        void validated_toOption_invalid() {
            Validated<String, Integer> v = Validated.invalid("err");
            assertEquals(Option.none(), v.toOption());
        }

        @Test
        void validated_fromOption_some() {
            Validated<String, Integer> v = Validated.fromOption(Option.some(9), "missing");
            assertTrue(v.isValid());
            assertEquals(9, v.get());
        }

        @Test
        void validated_fromOption_none() {
            Validated<String, Integer> v = Validated.fromOption(Option.none(), "missing");
            assertTrue(v.isInvalid());
            assertEquals("missing", v.getError());
        }

        @Test
        void validated_toTry_valid() {
            Validated<String, Integer> v = Validated.valid(3);
            Try<Integer> t = v.toTry(RuntimeException::new);
            assertTrue(t.isSuccess());
            assertEquals(3, t.get());
        }

        @Test
        void validated_toTry_invalid() {
            Validated<String, Integer> v = Validated.invalid("fail");
            Try<Integer> t = v.toTry(RuntimeException::new);
            assertTrue(t.isFailure());
            assertEquals("fail", t.getCause().getMessage());
        }

        @Test
        void validated_fromTry_success() {
            Validated<String, Integer> v = Validated.fromTry(Try.success(10), Throwable::getMessage);
            assertTrue(v.isValid());
            assertEquals(10, v.get());
        }

        @Test
        void validated_fromTry_failure() {
            Validated<String, Integer> v = Validated.fromTry(
                Try.failure(new RuntimeException("boom")), Throwable::getMessage);
            assertTrue(v.isInvalid());
            assertEquals("boom", v.getError());
        }

        @Test
        @Disabled("just sample code")
        void foo() {
            // shows how the Option type can be used to represent a nullable value (invalid)
            var user2 = new Foo.User2();
            String name2 = Option.some(user2)
                .flatMap(Foo.User2::getProfile)
                .map(Foo.Profile::getDisplayName)
                .getOrElse("no name");
            System.out.printf("name 2: %s%n", name2);
        }
    }
}
