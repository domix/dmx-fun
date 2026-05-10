package dmx.fun;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EitherTest {

    // -------------------------------------------------------------------------
    // Factory: left / right
    // -------------------------------------------------------------------------

    @Test
    void left_shouldCreateLeftInstance() {
        var e = Either.left("error");
        assertThat(e.isLeft()).isTrue();
        assertThat(e.isRight()).isFalse();
    }

    @Test
    void right_shouldCreateRightInstance() {
        var e = Either.right(42);
        assertThat(e.isRight()).isTrue();
        assertThat(e.isLeft()).isFalse();
    }

    @Test
    void left_shouldThrowNPE_whenValueIsNull() {
        assertThatThrownBy(() -> Either.left(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Left value");
    }

    @Test
    void right_shouldThrowNPE_whenValueIsNull() {
        assertThatThrownBy(() -> Either.right(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Right value");
    }

    // -------------------------------------------------------------------------
    // getLeft / getRight
    // -------------------------------------------------------------------------

    @Test
    void getLeft_shouldReturnValue_whenLeft() {
        assertThat(Either.left("error").getLeft()).isEqualTo("error");
    }

    @Test
    void getRight_shouldReturnValue_whenRight() {
        assertThat(Either.right(42).getRight()).isEqualTo(42);
    }

    @Test
    void getLeft_shouldThrow_whenRight() {
        var e = Either.right(42);
        assertThatThrownBy(e::getLeft)
            .isInstanceOf(java.util.NoSuchElementException.class)
            .hasMessageContaining("Right");
    }

    @Test
    void getRight_shouldThrow_whenLeft() {
        var e = Either.left("error");
        assertThatThrownBy(e::getRight)
            .isInstanceOf(java.util.NoSuchElementException.class)
            .hasMessageContaining("Left");
    }

    // -------------------------------------------------------------------------
    // getRightOrElse / getRightOrElseGet / getLeftOrElse / getLeftOrElseGet
    // -------------------------------------------------------------------------

    @Test
    void getRightOrElse_returnsRight_whenRight() {
        var value = Either
            .right(42)
            .getRightOrElse(0);
        assertThat(value)
            .isEqualTo(42);
    }

    @Test
    void getRightOrElse_returnsFallback_whenLeft() {
        var value = Either
            .left("err")
            .getRightOrElse(0);
        assertThat(value)
            .isEqualTo(0);
    }

    @Test
    void getRightOrElse_throwsNPE_whenFallbackIsNull() {
        assertThatThrownBy(() -> Either.<String, Integer>left("err").getRightOrElse(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("fallback");
    }

    @Test
    void getRightOrElseGet_returnsRight_whenRight() {
        assertThat(Either.<String, Integer>right(42).getRightOrElseGet(() -> 0)).isEqualTo(42);
    }

    @Test
    void getRightOrElseGet_callsSupplier_whenLeft() {
        assertThat(Either.<String, Integer>left("err").getRightOrElseGet(() -> 99)).isEqualTo(99);
    }

    @Test
    void getRightOrElseGet_doesNotCallSupplier_whenRight() {
        var called = new ArrayList<String>();
        Either.<String, Integer>right(1).getRightOrElseGet(() -> {
            called.add("called");
            return 0;
        });
        assertThat(called).isEmpty();
    }

    @Test
    void getRightOrElseGet_throwsNPE_whenSupplierIsNull() {
        assertThatThrownBy(() -> Either.<String, Integer>left("err").getRightOrElseGet(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("supplier");
    }

    @Test
    void getRightOrElseGet_throwsNPE_whenSupplierReturnsNull() {
        assertThatThrownBy(() -> Either.<String, Integer>left("err").getRightOrElseGet(() -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("supplier returned null");
    }

    @Test
    void getLeftOrElse_returnsLeft_whenLeft() {
        assertThat(Either.<String, Integer>left("err").getLeftOrElse("default")).isEqualTo("err");
    }

    @Test
    void getLeftOrElse_returnsFallback_whenRight() {
        assertThat(Either.<String, Integer>right(1).getLeftOrElse("default")).isEqualTo("default");
    }

    @Test
    void getLeftOrElse_throwsNPE_whenFallbackIsNull() {
        assertThatThrownBy(() -> Either.<String, Integer>right(1).getLeftOrElse(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("fallback");
    }

    @Test
    void getLeftOrElseGet_returnsLeft_whenLeft() {
        assertThat(Either.<String, Integer>left("err").getLeftOrElseGet(() -> "default")).isEqualTo("err");
    }

    @Test
    void getLeftOrElseGet_callsSupplier_whenRight() {
        assertThat(Either.<String, Integer>right(1).getLeftOrElseGet(() -> "fallback")).isEqualTo("fallback");
    }

    @Test
    void getLeftOrElseGet_doesNotCallSupplier_whenLeft() {
        var called = new ArrayList<String>();
        Either.<String, Integer>left("x").getLeftOrElseGet(() -> {
            called.add("called");
            return "y";
        });
        assertThat(called).isEmpty();
    }

    @Test
    void getLeftOrElseGet_throwsNPE_whenSupplierIsNull() {
        assertThatThrownBy(() -> Either.<String, Integer>right(1).getLeftOrElseGet(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("supplier");
    }

    @Test
    void getLeftOrElseGet_throwsNPE_whenSupplierReturnsNull() {
        assertThatThrownBy(() -> Either.<String, Integer>right(1).getLeftOrElseGet(() -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("supplier returned null");
    }

    // -------------------------------------------------------------------------
    // fold
    // -------------------------------------------------------------------------

    @Test
    void fold_shouldApplyOnLeft_whenLeft() {
        var e = Either.left("error");
        var result = e.fold("LEFT:%s"::formatted, "RIGHT:%s"::formatted);
        assertThat(result).isEqualTo("LEFT:error");
    }

    @Test
    void fold_shouldApplyOnRight_whenRight() {
        var e = Either.right(42);
        var result = e.fold("LEFT:%s"::formatted, "RIGHT:%d"::formatted);
        assertThat(result).isEqualTo("RIGHT:42");
    }

    @Test
    void fold_shouldThrowNPE_whenOnLeftIsNull() {
        var e = Either.left("x");
        assertThatThrownBy(() -> e.fold(null, Object::toString))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fold_shouldThrowNPE_whenOnRightIsNull() {
        var e = Either.right(1);
        assertThatThrownBy(() -> e.fold(l -> l, null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // map
    // -------------------------------------------------------------------------

    @Test
    void map_shouldTransformRight_whenRight() {
        var e = Either.right(5)
            .map(v -> v * 2);
        assertThat(e.isRight()).isTrue();
        assertThat(e.getRight()).isEqualTo(10);
    }

    @Test
    void map_shouldLeaveLeft_unchanged() {
        var e = Either.<String, Integer>left("err")
            .map(v -> v * 2);
        assertThat(e.isLeft()).isTrue();
        assertThat(e.getLeft()).isEqualTo("err");
    }

    @Test
    void map_shouldThrowNPE_whenMapperIsNull() {
        var e = Either.right(1);
        assertThatThrownBy(() -> e.map(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    @Test
    void map_shouldThrowNPE_whenMapperReturnsNull() {
        var e = Either.right(1);
        assertThatThrownBy(() -> e.map(_ -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper returned null");
    }

    // -------------------------------------------------------------------------
    // mapLeft
    // -------------------------------------------------------------------------

    @Test
    void mapLeft_shouldTransformLeft_whenLeft() {
        var e = Either.left("error")
            .mapLeft(String::toUpperCase);
        assertThat(e.isLeft()).isTrue();
        assertThat(e.getLeft()).isEqualTo("ERROR");
    }

    @Test
    void mapLeft_shouldLeaveRight_unchanged() {
        var e = Either.<String, Integer>right(7)
            .mapLeft(String::toUpperCase);
        assertThat(e.isRight()).isTrue();
        assertThat(e.getRight()).isEqualTo(7);
    }

    @Test
    void mapLeft_shouldThrowNPE_whenMapperIsNull() {
        var e = Either.left("x");
        assertThatThrownBy(() -> e.mapLeft(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    @Test
    void mapLeft_shouldThrowNPE_whenMapperReturnsNull() {
        var e = Either.left("x");
        assertThatThrownBy(() -> e.mapLeft(_ -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper returned null");
    }

    // -------------------------------------------------------------------------
    // flatMap
    // -------------------------------------------------------------------------

    @Test
    void flatMap_shouldChain_whenRight() {
        var result = Either.right(5)
            .flatMap(v -> v > 0 ? Either.right(v * 10) : Either.left("negative"));
        assertThat(result.isRight()).isTrue();
        assertThat(result.getRight()).isEqualTo(50);
    }

    @Test
    void flatMap_shouldShortCircuit_whenLeft() {
        var called = new ArrayList<String>();
        var result = Either.left("err")
            .flatMap(v -> {
                called.add("called");
                return Either.right(v);
            });
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEqualTo("err");
        assertThat(called).isEmpty();
    }

    @Test
    void flatMap_shouldReturnLeft_whenMapperReturnsLeft() {
        var result = Either.right(5)
            .flatMap(_ -> Either.left("too small"));
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEqualTo("too small");
    }

    @Test
    void flatMap_shouldThrowNPE_whenMapperIsNull() {
        var e = Either.right(1);
        assertThatThrownBy(() -> e.flatMap(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void flatMap_shouldThrowNPE_whenMapperReturnsNull() {
        var e = Either.right(1);
        assertThatThrownBy(() -> e.flatMap(v -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper returned null");
    }

    // -------------------------------------------------------------------------
    // flatMapLeft
    // -------------------------------------------------------------------------

    @Test
    void flatMapLeft_shouldChain_whenLeft() {
        var result = Either.<String, Integer>left("error")
            .flatMapLeft(s -> s.isEmpty() ? Either.right(0) : Either.left(s.toUpperCase()));
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEqualTo("ERROR");
    }

    @Test
    void flatMapLeft_shouldShortCircuit_whenRight() {
        var called = new ArrayList<String>();
        var result = Either.<String, Integer>right(42)
            .flatMapLeft(s -> {
                called.add("called");
                return Either.left(s);
            });
        assertThat(result.isRight()).isTrue();
        assertThat(result.getRight()).isEqualTo(42);
        assertThat(called).isEmpty();
    }

    @Test
    void flatMapLeft_shouldReturnRight_whenMapperReturnsRight() {
        var result = Either.<String, Integer>left("skip")
            .flatMapLeft(_ -> Either.right(99));
        assertThat(result.isRight()).isTrue();
        assertThat(result.getRight()).isEqualTo(99);
    }

    @Test
    void flatMapLeft_shouldThrowNPE_whenMapperIsNull() {
        var e = Either.left("x");
        assertThatThrownBy(() -> e.flatMapLeft(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    @Test
    void flatMapLeft_shouldThrowNPE_whenMapperReturnsNull() {
        var e = Either.left("x");
        assertThatThrownBy(() -> e.flatMapLeft(_ -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper returned null");
    }

    // -------------------------------------------------------------------------
    // swap
    // -------------------------------------------------------------------------

    @Test
    void swap_shouldTurnLeftIntoRight() {
        var swapped = Either.left("hello").swap();
        assertThat(swapped.isRight()).isTrue();
        assertThat(swapped.getRight()).isEqualTo("hello");
    }

    @Test
    void swap_shouldTurnRightIntoLeft() {
        var swapped = Either.right("world").swap();
        assertThat(swapped.isLeft()).isTrue();
        assertThat(swapped.getLeft()).isEqualTo("world");
    }

    @Test
    void swap_shouldBeInvolution() {
        var original = Either.right(99);
        assertThat(
            original
                .swap()
                .swap()
                .getRight()
        ).isEqualTo(99);
    }

    // -------------------------------------------------------------------------
    // peek / peekLeft
    // -------------------------------------------------------------------------

    @Test
    void peek_shouldExecuteAction_whenRight() {
        var seen = new ArrayList<Integer>();
        var e = Either.<String, Integer>right(42)
            .peek(seen::add);
        assertThat(seen).containsExactly(42);
        assertThat(e.getRight()).isEqualTo(42); // unchanged
    }

    @Test
    void peek_shouldNotExecuteAction_whenLeft() {
        var seen = new ArrayList<Integer>();
        Either.<String, Integer>left("err").peek(seen::add);
        assertThat(seen).isEmpty();
    }

    @Test
    void peek_shouldThrowNPE_whenActionIsNull() {
        var e = Either.right(1);
        assertThatThrownBy(() -> e.peek(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void peekLeft_shouldExecuteAction_whenLeft() {
        var seen = new ArrayList<String>();
        var e = Either.left("err")
            .peekLeft(seen::add);
        assertThat(seen).containsExactly("err");
        assertThat(e.getLeft()).isEqualTo("err"); // unchanged
    }

    @Test
    void peekLeft_shouldNotExecuteAction_whenRight() {
        var seen = new ArrayList<String>();
        Either.<String, Integer>right(1)
            .peekLeft(seen::add);
        assertThat(seen).isEmpty();
    }

    @Test
    void peekLeft_shouldThrowNPE_whenActionIsNull() {
        var e = Either.left("x");
        assertThatThrownBy(() -> e.peekLeft(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // toOption
    // -------------------------------------------------------------------------

    @Test
    void toOption_shouldReturnSome_whenRight() {
        var opt = Either
            .right(7)
            .toOption();
        assertThat(opt.isDefined()).isTrue();
        assertThat(opt.get()).isEqualTo(7);
    }

    @Test
    void toOption_shouldReturnNone_whenLeft() {
        var opt = Either.left("err")
            .toOption();
        assertThat(opt.isEmpty()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Either.toResult
    // -------------------------------------------------------------------------

    @Test
    void toResult_shouldMapRightToOk() {
        var result = Either
            .right(42)
            .toResult();
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo(42);
    }

    @Test
    void toResult_shouldMapLeftToErr() {
        var result = Either
            .left("fail")
            .toResult();
        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).isEqualTo("fail");
    }

    // -------------------------------------------------------------------------
    // Either.toValidated
    // -------------------------------------------------------------------------

    @Test
    void toValidated_shouldMapRightToValid() {
        var v = Either
            .right(99)
            .toValidated();
        assertThat(v.isValid()).isTrue();
        assertThat(v.get()).isEqualTo(99);
    }

    @Test
    void toValidated_shouldMapLeftToInvalid() {
        var v = Either
            .left("err")
            .toValidated();
        assertThat(v.isInvalid()).isTrue();
        assertThat(v.getError()).isEqualTo("err");
    }

    // -------------------------------------------------------------------------
    // Result.toEither conversion
    // -------------------------------------------------------------------------

    @Test
    void result_toEither_shouldMapOkToRight() {
        var ok = Result.<Integer, String>ok(42);
        var e = ok.toEither();
        assertThat(e.isRight()).isTrue();
        assertThat(e.getRight()).isEqualTo(42);
    }

    @Test
    void result_toEither_shouldMapErrToLeft() {
        var err = Result.<Integer, String>err("fail");
        var e = err.toEither();
        assertThat(e.isLeft()).isTrue();
        assertThat(e.getLeft()).isEqualTo("fail");
    }

    // -------------------------------------------------------------------------
    // toOptional
    // -------------------------------------------------------------------------

    @Test
    void toOptional_shouldReturnPresent_whenRight() {
        Optional<Integer> opt = Either.<String, Integer>right(42).toOptional();
        assertThat(opt).isPresent().hasValue(42);
    }

    @Test
    void toOptional_shouldReturnEmpty_whenLeft() {
        Optional<Integer> opt = Either.<String, Integer>left("err").toOptional();
        assertThat(opt).isEmpty();
    }

    // -------------------------------------------------------------------------
    // stream
    // -------------------------------------------------------------------------

    @Test
    void stream_shouldReturnSingleElementStream_whenRight() {
        List<Integer> collected = Either.<String, Integer>right(7).stream().toList();
        assertThat(collected).containsExactly(7);
    }

    @Test
    void stream_shouldReturnEmptyStream_whenLeft() {
        List<Integer> collected = Either.<String, Integer>left("err").stream().toList();
        assertThat(collected).isEmpty();
    }

    @Test
    void stream_shouldIntegrateIntoStreamPipeline() {
        List<Either<String, Integer>> eithers = List.of(
            Either.right(1), Either.left("err"), Either.right(3));
        List<Integer> rights = eithers.stream()
            .flatMap(Either::stream)
            .toList();
        assertThat(rights).containsExactly(1, 3);
    }

    // -------------------------------------------------------------------------
    // streamLeft
    // -------------------------------------------------------------------------

    @Test
    void streamLeft_shouldReturnSingleElementStream_whenLeft() {
        List<String> collected = Either.<String, Integer>left("err").streamLeft().toList();
        assertThat(collected).containsExactly("err");
    }

    @Test
    void streamLeft_shouldReturnEmptyStream_whenRight() {
        List<String> collected = Either.<String, Integer>right(7).streamLeft().toList();
        assertThat(collected).isEmpty();
    }

    @Test
    void streamLeft_shouldIntegrateIntoStreamPipeline() {
        List<Either<String, Integer>> eithers = List.of(
            Either.left("a"), Either.right(1), Either.left("b"));
        List<String> lefts = eithers.stream()
            .flatMap(Either::streamLeft)
            .toList();
        assertThat(lefts).containsExactly("a", "b");
    }

    // -------------------------------------------------------------------------
    // toTry
    // -------------------------------------------------------------------------

    @Test
    void toTry_shouldReturnSuccess_whenRight() {
        Try<Integer> t = Either.<String, Integer>right(42)
            .toTry(IllegalArgumentException::new);
        assertThat(t.isSuccess()).isTrue();
        assertThat(t.get()).isEqualTo(42);
    }

    @Test
    void toTry_shouldReturnFailure_whenLeft() {
        Try<Integer> t = Either.<String, Integer>left("bad")
            .toTry(IllegalArgumentException::new);
        assertThat(t.isFailure()).isTrue();
        assertThat(t.getCause())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("bad");
    }

    @Test
    void toTry_shouldThrowNPE_whenLeftMapperIsNull() {
        var e = Either.<String, Integer>left("x");
        assertThatThrownBy(() -> e.toTry(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("leftMapper");
    }

    @Test
    void toTry_shouldThrowNPE_whenLeftMapperReturnsNull() {
        var e = Either.<String, Integer>left("x");
        assertThatThrownBy(() -> e.toTry(_ -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("leftMapper returned null");
    }

    // -------------------------------------------------------------------------
    // match
    // -------------------------------------------------------------------------

    @Test
    void match_shouldExecuteOnLeft_whenLeft() {
        var seen = new ArrayList<String>();
        Either.<String, Integer>left("err").match(seen::add, v -> seen.add("right:" + v));
        assertThat(seen).containsExactly("err");
    }

    @Test
    void match_shouldExecuteOnRight_whenRight() {
        var seen = new ArrayList<String>();
        Either.<String, Integer>right(42).match(seen::add, v -> seen.add("right:" + v));
        assertThat(seen).containsExactly("right:42");
    }

    @Test
    void match_shouldThrowNPE_whenOnLeftIsNull() {
        var e = Either.<String, Integer>left("x");
        assertThatThrownBy(() -> e.match(null, v -> {}))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("onLeft");
    }

    @Test
    void match_shouldThrowNPE_whenOnRightIsNull() {
        var e = Either.<String, Integer>right(1);
        assertThatThrownBy(() -> e.match(l -> {}, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("onRight");
    }

    // -------------------------------------------------------------------------
    // Neutral semantics demo: neither side is an error
    // -------------------------------------------------------------------------

    @Test
    void eitherShouldModel_neutralBranching_withoutErrorSemantics() {
        var adminId = Either.left("admin-001");
        var userId  = Either.right(42);

        var labelAdmin = adminId.fold("Admin: %s"::formatted, "User #%s"::formatted);
        var labelUser  = userId.fold("Admin: %s"::formatted, "User #%d"::formatted);

        assertThat(labelAdmin).isEqualTo("Admin: admin-001");
        assertThat(labelUser).isEqualTo("User #42");
    }
}
