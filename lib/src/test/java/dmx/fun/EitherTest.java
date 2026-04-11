package dmx.fun;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EitherTest {

    // -------------------------------------------------------------------------
    // Factory: left / right
    // -------------------------------------------------------------------------

    @Test
    void left_shouldCreateLeftInstance() {
        Either<String, Integer> e = Either.left("error");
        assertThat(e.isLeft()).isTrue();
        assertThat(e.isRight()).isFalse();
    }

    @Test
    void right_shouldCreateRightInstance() {
        Either<String, Integer> e = Either.right(42);
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
        assertThat(Either.<String, Integer>left("error").getLeft()).isEqualTo("error");
    }

    @Test
    void getRight_shouldReturnValue_whenRight() {
        assertThat(Either.<String, Integer>right(42).getRight()).isEqualTo(42);
    }

    @Test
    void getLeft_shouldThrow_whenRight() {
        Either<String, Integer> e = Either.right(42);
        assertThatThrownBy(e::getLeft)
            .isInstanceOf(java.util.NoSuchElementException.class)
            .hasMessageContaining("Right");
    }

    @Test
    void getRight_shouldThrow_whenLeft() {
        Either<String, Integer> e = Either.left("error");
        assertThatThrownBy(e::getRight)
            .isInstanceOf(java.util.NoSuchElementException.class)
            .hasMessageContaining("Left");
    }

    // -------------------------------------------------------------------------
    // fold
    // -------------------------------------------------------------------------

    @Test
    void fold_shouldApplyOnLeft_whenLeft() {
        Either<String, Integer> e = Either.left("error");
        String result = e.fold(l -> "LEFT:" + l, r -> "RIGHT:" + r);
        assertThat(result).isEqualTo("LEFT:error");
    }

    @Test
    void fold_shouldApplyOnRight_whenRight() {
        Either<String, Integer> e = Either.right(42);
        String result = e.fold(l -> "LEFT:" + l, r -> "RIGHT:" + r);
        assertThat(result).isEqualTo("RIGHT:42");
    }

    @Test
    void fold_shouldThrowNPE_whenOnLeftIsNull() {
        Either<String, Integer> e = Either.left("x");
        assertThatThrownBy(() -> e.fold(null, r -> r.toString()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fold_shouldThrowNPE_whenOnRightIsNull() {
        Either<String, Integer> e = Either.right(1);
        assertThatThrownBy(() -> e.fold(l -> l, null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // map
    // -------------------------------------------------------------------------

    @Test
    void map_shouldTransformRight_whenRight() {
        Either<String, Integer> e = Either.<String, Integer>right(5).map(v -> v * 2);
        assertThat(e.isRight()).isTrue();
        assertThat(e.getRight()).isEqualTo(10);
    }

    @Test
    void map_shouldLeaveLeft_unchanged() {
        Either<String, Integer> e = Either.<String, Integer>left("err").map(v -> v * 2);
        assertThat(e.isLeft()).isTrue();
        assertThat(e.getLeft()).isEqualTo("err");
    }

    @Test
    void map_shouldThrowNPE_whenMapperIsNull() {
        Either<String, Integer> e = Either.right(1);
        assertThatThrownBy(() -> e.map(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // mapLeft
    // -------------------------------------------------------------------------

    @Test
    void mapLeft_shouldTransformLeft_whenLeft() {
        Either<String, Integer> e = Either.<String, Integer>left("error").mapLeft(String::toUpperCase);
        assertThat(e.isLeft()).isTrue();
        assertThat(e.getLeft()).isEqualTo("ERROR");
    }

    @Test
    void mapLeft_shouldLeaveRight_unchanged() {
        Either<String, Integer> e = Either.<String, Integer>right(7).mapLeft(String::toUpperCase);
        assertThat(e.isRight()).isTrue();
        assertThat(e.getRight()).isEqualTo(7);
    }

    @Test
    void mapLeft_shouldThrowNPE_whenMapperIsNull() {
        Either<String, Integer> e = Either.left("x");
        assertThatThrownBy(() -> e.mapLeft(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // flatMap
    // -------------------------------------------------------------------------

    @Test
    void flatMap_shouldChain_whenRight() {
        Either<String, Integer> result = Either.<String, Integer>right(5)
            .flatMap(v -> v > 0 ? Either.right(v * 10) : Either.left("negative"));
        assertThat(result.isRight()).isTrue();
        assertThat(result.getRight()).isEqualTo(50);
    }

    @Test
    void flatMap_shouldShortCircuit_whenLeft() {
        List<String> called = new ArrayList<>();
        Either<String, Integer> result = Either.<String, Integer>left("err")
            .flatMap(v -> { called.add("called"); return Either.right(v); });
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEqualTo("err");
        assertThat(called).isEmpty();
    }

    @Test
    void flatMap_shouldReturnLeft_whenMapperReturnsLeft() {
        Either<String, Integer> result = Either.<String, Integer>right(5)
            .flatMap(v -> Either.left("too small"));
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEqualTo("too small");
    }

    @Test
    void flatMap_shouldThrowNPE_whenMapperIsNull() {
        Either<String, Integer> e = Either.right(1);
        assertThatThrownBy(() -> e.flatMap(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void flatMap_shouldThrowNPE_whenMapperReturnsNull() {
        Either<String, Integer> e = Either.right(1);
        assertThatThrownBy(() -> e.flatMap(v -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper returned null");
    }

    // -------------------------------------------------------------------------
    // swap
    // -------------------------------------------------------------------------

    @Test
    void swap_shouldTurnLeftIntoRight() {
        Either<Integer, String> swapped = Either.<String, Integer>left("hello").swap();
        assertThat(swapped.isRight()).isTrue();
        assertThat(swapped.getRight()).isEqualTo("hello");
    }

    @Test
    void swap_shouldTurnRightIntoLeft() {
        Either<String, Integer> swapped = Either.<Integer, String>right("world").swap();
        assertThat(swapped.isLeft()).isTrue();
        assertThat(swapped.getLeft()).isEqualTo("world");
    }

    @Test
    void swap_shouldBeInvolution() {
        Either<String, Integer> original = Either.right(99);
        assertThat(original.swap().swap().getRight()).isEqualTo(99);
    }

    // -------------------------------------------------------------------------
    // peek / peekLeft
    // -------------------------------------------------------------------------

    @Test
    void peek_shouldExecuteAction_whenRight() {
        List<Integer> seen = new ArrayList<>();
        Either<String, Integer> e = Either.<String, Integer>right(42).peek(seen::add);
        assertThat(seen).containsExactly(42);
        assertThat(e.getRight()).isEqualTo(42); // unchanged
    }

    @Test
    void peek_shouldNotExecuteAction_whenLeft() {
        List<Integer> seen = new ArrayList<>();
        Either.<String, Integer>left("err").peek(seen::add);
        assertThat(seen).isEmpty();
    }

    @Test
    void peek_shouldThrowNPE_whenActionIsNull() {
        Either<String, Integer> e = Either.right(1);
        assertThatThrownBy(() -> e.peek(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void peekLeft_shouldExecuteAction_whenLeft() {
        List<String> seen = new ArrayList<>();
        Either<String, Integer> e = Either.<String, Integer>left("err").peekLeft(seen::add);
        assertThat(seen).containsExactly("err");
        assertThat(e.getLeft()).isEqualTo("err"); // unchanged
    }

    @Test
    void peekLeft_shouldNotExecuteAction_whenRight() {
        List<String> seen = new ArrayList<>();
        Either.<String, Integer>right(1).peekLeft(seen::add);
        assertThat(seen).isEmpty();
    }

    @Test
    void peekLeft_shouldThrowNPE_whenActionIsNull() {
        Either<String, Integer> e = Either.left("x");
        assertThatThrownBy(() -> e.peekLeft(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // toOption
    // -------------------------------------------------------------------------

    @Test
    void toOption_shouldReturnSome_whenRight() {
        Option<Integer> opt = Either.<String, Integer>right(7).toOption();
        assertThat(opt.isDefined()).isTrue();
        assertThat(opt.get()).isEqualTo(7);
    }

    @Test
    void toOption_shouldReturnNone_whenLeft() {
        Option<Integer> opt = Either.<String, Integer>left("err").toOption();
        assertThat(opt.isEmpty()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Either.toResult
    // -------------------------------------------------------------------------

    @Test
    void toResult_shouldMapRightToOk() {
        Result<Integer, String> result = Either.<String, Integer>right(42).toResult();
        assertThat(result.isOk()).isTrue();
        assertThat(result.get()).isEqualTo(42);
    }

    @Test
    void toResult_shouldMapLeftToErr() {
        Result<Integer, String> result = Either.<String, Integer>left("fail").toResult();
        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).isEqualTo("fail");
    }

    // -------------------------------------------------------------------------
    // Either.toValidated
    // -------------------------------------------------------------------------

    @Test
    void toValidated_shouldMapRightToValid() {
        Validated<String, Integer> v = Either.<String, Integer>right(99).toValidated();
        assertThat(v.isValid()).isTrue();
        assertThat(v.get()).isEqualTo(99);
    }

    @Test
    void toValidated_shouldMapLeftToInvalid() {
        Validated<String, Integer> v = Either.<String, Integer>left("err").toValidated();
        assertThat(v.isInvalid()).isTrue();
        assertThat(v.getError()).isEqualTo("err");
    }

    // -------------------------------------------------------------------------
    // Result.toEither conversion
    // -------------------------------------------------------------------------

    @Test
    void result_toEither_shouldMapOkToRight() {
        Result<Integer, String> ok = Result.ok(42);
        Either<String, Integer> e = ok.toEither();
        assertThat(e.isRight()).isTrue();
        assertThat(e.getRight()).isEqualTo(42);
    }

    @Test
    void result_toEither_shouldMapErrToLeft() {
        Result<Integer, String> err = Result.err("fail");
        Either<String, Integer> e = err.toEither();
        assertThat(e.isLeft()).isTrue();
        assertThat(e.getLeft()).isEqualTo("fail");
    }

    // -------------------------------------------------------------------------
    // Neutral semantics demo: neither side is an error
    // -------------------------------------------------------------------------

    @Test
    void eitherShouldModel_neutralBranching_withoutErrorSemantics() {
        Either<String, Integer> adminId = Either.left("admin-001");
        Either<String, Integer> userId  = Either.right(42);

        String labelAdmin = adminId.fold(a -> "Admin: " + a, u -> "User #" + u);
        String labelUser  = userId.fold(a  -> "Admin: " + a, u -> "User #" + u);

        assertThat(labelAdmin).isEqualTo("Admin: admin-001");
        assertThat(labelUser).isEqualTo("User #42");
    }
}
