package dmx.fun;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InteropTest {

    @Test
    void option_toResult_and_back() {
        Option<Integer> some = Option.some(10);
        Option<Integer> none = Option.none();

        Result<Integer, String> r1 = some.toResult("missing");
        Result<Integer, String> r2 = none.toResult("missing");

        assertThat(r1.isOk()).isTrue();
        assertThat(r1.get()).isEqualTo(10);

        assertThat(r2.isError()).isTrue();
        assertThat(r2.getError()).isEqualTo("missing");

        assertThat(Option.fromResult(r1)).isEqualTo(some);
        assertThat(Option.fromResult(r2)).isEqualTo(Option.none());
    }

    @Test
    void result_toTry_shouldMapErrToThrowable() {
        Result<Integer, String> err = Result.err("boom");
        Try<Integer> t = err.toTry(RuntimeException::new);

        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause().getMessage()).isEqualTo("boom");
    }

    @Test
    void try_toResult_shouldMapErrToThrowable() {
        Try<Integer> boom = Result.ok(2)
            .toTry(o -> new RuntimeException("boom"));
        assertThat(boom.isSuccess()).isTrue();
    }

    @Test
    void result_fromTry() {
        var success = Result.fromTry(Try.success(10));
        assertThat(success).isEqualTo(Result.ok(10));
    }

    @Test
    void result_fromOption() {
        Result<Integer, String> someValue = Result.fromOption(Option.some(10), "missing");
        Result<Object, String> missingOption = Result.fromOption(Option.none(), "missing");
        assertThat(someValue).isEqualTo(Result.ok(10));
        assertThat(missingOption).isEqualTo(Result.err("missing"));
    }

    @Test
    void try_toOption_and_back() {
        Try<Integer> s = Try.success(10);
        Try<Integer> f = Try.failure(new IllegalStateException("x"));

        assertThat(s.toOption()).isEqualTo(Option.some(10));
        assertThat(f.toOption()).isEqualTo(Option.none());

        Try<Integer> back1 = Try.fromOption(Option.some(10), () -> new RuntimeException("missing"));
        Try<Integer> back2 = Try.fromOption(Option.none(), () -> new RuntimeException("missing"));

        assertThat(back1.isSuccess()).isTrue();
        assertThat(back1.get()).isEqualTo(10);

        assertThat(back2.isFailure()).isTrue();
        assertThat(back2.getCause().getMessage()).isEqualTo("missing");
    }

    @Test
    void option_toTry_shouldBeLazy_onSome() {
        AtomicBoolean called = new AtomicBoolean(false);

        Try<Integer> t = Option.some(10).toTry(() -> {
            called.set(true);
            return new RuntimeException("nope");
        });

        assertThat(t.isSuccess()).isTrue();
        assertThat(t.get()).isEqualTo(10);
        assertThat(called.get()).as("exception supplier must not be called for Some").isFalse();
    }

    @Test
    void option_fromTry() {
        var success = Option.fromTry(Try.success(10));
        assertThat(success).isEqualTo(Option.some(10));
        var failure = Option.fromTry(Try.failure(new IllegalStateException("boom")));
        assertThat(failure).isEqualTo(Option.none());
    }

    @Test
    void option_toResult() {
        var success = Option.some(10).toResult("missing");
        assertThat(success).isEqualTo(Result.ok(10));
        var failure = Option.<Integer>none().toResult("missing");
        assertThat(failure).isEqualTo(Result.err("missing"));
    }

    @Test
    void option_toTry() {
        var success = Option.some(10).toTry(() -> new RuntimeException("boom"));
        assertThat(success.isSuccess()).isTrue();
        assertThat(success.get()).isEqualTo(10);
        var failure = Option.<Integer>none().toTry(() -> new RuntimeException("boom"));
        assertThat(failure.isFailure()).isTrue();
    }

    // ---------- Optional interop ----------

    @Test
    void result_fromOptional_present_returnsOk() {
        Result<Integer, NoSuchElementException> r = Result.fromOptional(Optional.of(42));
        assertThat(r.isOk()).isTrue();
        assertThat(r.get()).isEqualTo(42);
    }

    @Test
    void result_fromOptional_empty_returnsErrWithNoSuchElementException() {
        Result<Integer, NoSuchElementException> r = Result.fromOptional(Optional.empty());
        assertThat(r.isError()).isTrue();
        assertThat(r.getError()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void result_fromOptional_null_throwsNPE() {
        assertThatThrownBy(() -> Result.fromOptional(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void try_fromOptional_present_returnsSuccess() {
        Try<Integer> t = Try.fromOptional(Optional.of(99), () -> new RuntimeException("missing"));
        assertThat(t.isSuccess()).isTrue();
        assertThat(t.get()).isEqualTo(99);
    }

    @Test
    void try_fromOptional_empty_returnsFailureFromSupplier() {
        RuntimeException ex = new RuntimeException("missing");
        Try<Integer> t = Try.fromOptional(Optional.empty(), () -> ex);
        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause()).isEqualTo(ex);
    }

    @Test
    void try_fromOptional_supplierNotCalledWhenPresent() {
        AtomicBoolean called = new AtomicBoolean(false);
        Try<Integer> t = Try.fromOptional(Optional.of(1), () -> {
            called.set(true);
            return new RuntimeException("should not be called");
        });
        assertThat(t.isSuccess()).isTrue();
        assertThat(called.get()).as("supplier must not be called when Optional is present").isFalse();
    }

    @Test
    void try_fromOptional_null_throwsNPE() {
        assertThatThrownBy(() -> Try.fromOptional(null, () -> new RuntimeException("x")))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void try_fromOptional_nullSupplier_throwsNPE() {
        assertThatThrownBy(() -> Try.fromOptional(Optional.empty(), null))
            .isInstanceOf(NullPointerException.class);
    }

    // ---------- Validated interop ----------

    @Nested
    class ValidatedInterop {

        @Test
        void validated_toResult_valid() {
            Validated<String, Integer> v = Validated.valid(42);
            Result<Integer, String> r = v.toResult();
            assertThat(r.isOk()).isTrue();
            assertThat(r.get()).isEqualTo(42);
        }

        @Test
        void validated_toResult_invalid() {
            Validated<String, Integer> v = Validated.invalid("oops");
            Result<Integer, String> r = v.toResult();
            assertThat(r.isError()).isTrue();
            assertThat(r.getError()).isEqualTo("oops");
        }

        @Test
        void validated_fromResult_ok() {
            Result<Integer, String> r = Result.ok(7);
            Validated<String, Integer> v = Validated.fromResult(r);
            assertThat(v.isValid()).isTrue();
            assertThat(v.get()).isEqualTo(7);
        }

        @Test
        void validated_fromResult_err() {
            Result<Integer, String> r = Result.err("bad");
            Validated<String, Integer> v = Validated.fromResult(r);
            assertThat(v.isInvalid()).isTrue();
            assertThat(v.getError()).isEqualTo("bad");
        }

        @Test
        void validated_toOption_valid() {
            Validated<String, Integer> v = Validated.valid(5);
            assertThat(v.toOption()).isEqualTo(Option.some(5));
        }

        @Test
        void validated_toOption_invalid() {
            Validated<String, Integer> v = Validated.invalid("err");
            assertThat(v.toOption()).isEqualTo(Option.none());
        }

        @Test
        void validated_fromOption_some() {
            Validated<String, Integer> v = Validated.fromOption(Option.some(9), "missing");
            assertThat(v.isValid()).isTrue();
            assertThat(v.get()).isEqualTo(9);
        }

        @Test
        void validated_fromOption_none() {
            Validated<String, Integer> v = Validated.fromOption(Option.none(), "missing");
            assertThat(v.isInvalid()).isTrue();
            assertThat(v.getError()).isEqualTo("missing");
        }

        @Test
        void validated_toTry_valid() {
            Validated<String, Integer> v = Validated.valid(3);
            Try<Integer> t = v.toTry(RuntimeException::new);
            assertThat(t.isSuccess()).isTrue();
            assertThat(t.get()).isEqualTo(3);
        }

        @Test
        void validated_toTry_invalid() {
            Validated<String, Integer> v = Validated.invalid("fail");
            Try<Integer> t = v.toTry(RuntimeException::new);
            assertThat(t.isFailure()).isTrue();
            assertThat(t.getCause().getMessage()).isEqualTo("fail");
        }

        @Test
        void validated_fromTry_success() {
            Validated<String, Integer> v = Validated.fromTry(Try.success(10), Throwable::getMessage);
            assertThat(v.isValid()).isTrue();
            assertThat(v.get()).isEqualTo(10);
        }

        @Test
        void validated_fromTry_failure() {
            Validated<String, Integer> v = Validated.fromTry(
                Try.failure(new RuntimeException("boom")), Throwable::getMessage);
            assertThat(v.isInvalid()).isTrue();
            assertThat(v.getError()).isEqualTo("boom");
        }
    }
}
