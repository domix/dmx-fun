package dmx.fun;

import java.util.ArrayList;
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
            .isInstanceOf(NullPointerException.class);
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
            .isInstanceOf(NullPointerException.class);
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
