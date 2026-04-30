package dmx.fun.jakarta.jaxb;

import dmx.fun.Either;
import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Tuple2;
import dmx.fun.Tuple3;
import dmx.fun.Tuple4;
import dmx.fun.Validated;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonbAdaptersTest {

    // -----------------------------------------------------------------------
    // DmxFunJsonbAdapters.all()
    // -----------------------------------------------------------------------

    @Test
    void allReturnsNineAdapters() {
        assertThat(DmxFunJsonbAdapters.all()).hasSize(9);
    }

    // -----------------------------------------------------------------------
    // Option
    // -----------------------------------------------------------------------

    @Nested
    class OptionTests {

        private final OptionJsonbAdapter adapter = new OptionJsonbAdapter();

        @Test
        void someAdaptsToMapWithValue() throws Exception {
            var map = adapter.adaptToJson(Option.some("alice"));
            assertThat(map).containsEntry("value", "alice");
        }

        @Test
        void noneAdaptsToEmptyMap() throws Exception {
            var map = adapter.adaptToJson(Option.none());
            assertThat(map).isEmpty();
        }

        @Test
        void mapWithValueAdaptsFromJsonAsSome() throws Exception {
            var result = adapter.adaptFromJson(Map.of("value", "bob"));
            assertThat(result).isSome();
            assertThat(result.get()).isEqualTo("bob");
        }

        @Test
        void emptyMapAdaptsFromJsonAsNone() throws Exception {
            var result = adapter.adaptFromJson(Map.of());
            assertThat(result).isNone();
        }
    }

    // -----------------------------------------------------------------------
    // Result
    // -----------------------------------------------------------------------

    @Nested
    class ResultTests {

        private final ResultJsonbAdapter adapter = new ResultJsonbAdapter();

        @Test
        void okAdaptsToMapWithOkKey() throws Exception {
            var map = adapter.adaptToJson(Result.ok(42));
            assertThat(map).containsEntry("ok", 42);
        }

        @Test
        void errAdaptsToMapWithErrKey() throws Exception {
            var map = adapter.adaptToJson(Result.err("oops"));
            assertThat(map).containsEntry("err", "oops");
        }

        @Test
        void mapWithOkKeyAdaptsFromJsonAsOk() throws Exception {
            var result = adapter.adaptFromJson(Map.of("ok", "val"));
            assertThat(result).isOk();
        }

        @Test
        void mapWithErrKeyAdaptsFromJsonAsErr() throws Exception {
            var result = adapter.adaptFromJson(Map.of("err", "fail"));
            assertThat(result).isErr();
        }

        @Test
        void unknownKeyThrows() {
            assertThatThrownBy(() -> adapter.adaptFromJson(Map.of("x", "y")))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // Try
    // -----------------------------------------------------------------------

    @Nested
    class TryTests {

        private final TryJsonbAdapter adapter = new TryJsonbAdapter();

        @Test
        void successAdaptsToMapWithValueKey() throws Exception {
            var map = adapter.adaptToJson(Try.success("done"));
            assertThat(map).containsEntry("value", "done");
        }

        @Test
        void failureAdaptsToMapWithErrorKey() throws Exception {
            var map = adapter.adaptToJson(Try.failure(new RuntimeException("boom")));
            assertThat(map).containsEntry("error", "boom");
        }

        @Test
        void failureWithNullMessageFallsBackToClassName() throws Exception {
            var map = adapter.adaptToJson(Try.failure(new NullPointerException()));
            assertThat(map).containsEntry("error", "NullPointerException");
        }

        @Test
        void mapWithValueKeyAdaptsFromJsonAsSuccess() throws Exception {
            var result = adapter.adaptFromJson(Map.of("value", "ok"));
            assertThat(result).isSuccess();
        }

        @Test
        void mapWithErrorKeyAdaptsFromJsonAsFailure() throws Exception {
            var result = adapter.adaptFromJson(Map.of("error", "oops"));
            assertThat(result).isFailure();
        }
    }

    // -----------------------------------------------------------------------
    // Either
    // -----------------------------------------------------------------------

    @Nested
    class EitherTests {

        private final EitherJsonbAdapter adapter = new EitherJsonbAdapter();

        @Test
        void rightAdaptsToMapWithRightKey() throws Exception {
            var map = adapter.adaptToJson(Either.right("yes"));
            assertThat(map).containsEntry("right", "yes");
        }

        @Test
        void leftAdaptsToMapWithLeftKey() throws Exception {
            var map = adapter.adaptToJson(Either.left("no"));
            assertThat(map).containsEntry("left", "no");
        }

        @Test
        void mapWithRightKeyAdaptsFromJsonAsRight() throws Exception {
            var result = adapter.adaptFromJson(Map.of("right", "ok"));
            assertThat(result).isRight();
        }

        @Test
        void mapWithLeftKeyAdaptsFromJsonAsLeft() throws Exception {
            var result = adapter.adaptFromJson(Map.of("left", "err"));
            assertThat(result).isLeft();
        }

        @Test
        void unknownKeyThrows() {
            assertThatThrownBy(() -> adapter.adaptFromJson(Map.of("x", "y")))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // Validated
    // -----------------------------------------------------------------------

    @Nested
    class ValidatedTests {

        private final ValidatedJsonbAdapter adapter = new ValidatedJsonbAdapter();

        @Test
        void validAdaptsToMapWithValidKey() throws Exception {
            var map = adapter.adaptToJson(Validated.valid("ok"));
            assertThat(map).containsEntry("valid", "ok");
        }

        @Test
        void invalidAdaptsToMapWithInvalidKey() throws Exception {
            var map = adapter.adaptToJson(Validated.invalid("bad"));
            assertThat(map).containsEntry("invalid", "bad");
        }

        @Test
        void mapWithValidKeyAdaptsFromJsonAsValid() throws Exception {
            var result = adapter.adaptFromJson(Map.of("valid", "a"));
            assertThat(result).isValid();
        }

        @Test
        void mapWithInvalidKeyAdaptsFromJsonAsInvalid() throws Exception {
            var result = adapter.adaptFromJson(Map.of("invalid", "e"));
            assertThat(result).isInvalid();
        }

        @Test
        void unknownKeyThrows() {
            assertThatThrownBy(() -> adapter.adaptFromJson(Map.of("x", "y")))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // Tuple2
    // -----------------------------------------------------------------------

    @Nested
    class Tuple2Tests {

        private final Tuple2JsonbAdapter adapter = new Tuple2JsonbAdapter();

        @Test
        void adaptsToMapWithTwoKeys() throws Exception {
            var map = adapter.adaptToJson(Tuple2.of("a", 1));
            assertThat(map).containsEntry("_1", "a").containsEntry("_2", 1);
        }

        @Test
        void adaptsFromMapPreservingValues() throws Exception {
            var t = adapter.adaptFromJson(Map.of("_1", "x", "_2", "y"));
            assertThat(t._1()).isEqualTo("x");
            assertThat(t._2()).isEqualTo("y");
        }
    }

    // -----------------------------------------------------------------------
    // Tuple3
    // -----------------------------------------------------------------------

    @Nested
    class Tuple3Tests {

        private final Tuple3JsonbAdapter adapter = new Tuple3JsonbAdapter();

        @Test
        void adaptsToMapWithThreeKeys() throws Exception {
            var map = adapter.adaptToJson(Tuple3.of("a", "b", "c"));
            assertThat(map).containsEntry("_1", "a").containsEntry("_2", "b").containsEntry("_3", "c");
        }

        @Test
        void adaptsFromMapPreservingValues() throws Exception {
            var t = adapter.adaptFromJson(Map.of("_1", 1, "_2", 2, "_3", 3));
            assertThat(t._1()).isEqualTo(1);
            assertThat(t._2()).isEqualTo(2);
            assertThat(t._3()).isEqualTo(3);
        }
    }

    // -----------------------------------------------------------------------
    // Tuple4
    // -----------------------------------------------------------------------

    @Nested
    class Tuple4Tests {

        private final Tuple4JsonbAdapter adapter = new Tuple4JsonbAdapter();

        @Test
        void adaptsToMapWithFourKeys() throws Exception {
            var map = adapter.adaptToJson(Tuple4.of("a", "b", "c", "d"));
            assertThat(map).containsEntry("_1", "a").containsEntry("_2", "b")
                .containsEntry("_3", "c").containsEntry("_4", "d");
        }

        @Test
        void adaptsFromMapPreservingValues() throws Exception {
            var t = adapter.adaptFromJson(Map.of("_1", 1, "_2", 2, "_3", 3, "_4", 4));
            assertThat(t._1()).isEqualTo(1);
            assertThat(t._4()).isEqualTo(4);
        }
    }

    // -----------------------------------------------------------------------
    // NonEmptyList
    // -----------------------------------------------------------------------

    @Nested
    class NonEmptyListTests {

        private final NonEmptyListJsonbAdapter adapter = new NonEmptyListJsonbAdapter();

        @Test
        void adaptsToListPreservingOrder() throws Exception {
            var nel = NonEmptyList.of("a", List.of("b", "c"));
            var list = adapter.adaptToJson(nel);
            assertThat(list).containsExactly("a", "b", "c");
        }

        @Test
        void singletonAdaptsToSingleElementList() throws Exception {
            var list = adapter.adaptToJson(NonEmptyList.of("x", List.of()));
            assertThat(list).containsExactly("x");
        }

        @Test
        void adaptsFromListWithMultipleElements() throws Exception {
            var nel = adapter.adaptFromJson(List.of("x", "y", "z"));
            assertThat(nel.head()).isEqualTo("x");
            assertThat(nel.tail().toArray()).containsExactly("y", "z");
        }

        @Test
        void adaptsFromSingletonList() throws Exception {
            var nel = adapter.adaptFromJson(List.of("only"));
            assertThat(nel.head()).isEqualTo("only");
            assertThat(nel.tail()).isEmpty();
        }

        @Test
        void emptyListThrows() {
            assertThatThrownBy(() -> adapter.adaptFromJson(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
        }
    }
}
