package dmx.fun.jakarta.jaxb;

import dmx.fun.Either;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlAdaptersTest {

    // -----------------------------------------------------------------------
    // DmxFunXmlAdapters.all()
    // -----------------------------------------------------------------------

    @Test
    void allReturnsFourAdapters() {
        assertThat(DmxFunXmlAdapters.all()).hasSize(4);
    }

    // -----------------------------------------------------------------------
    // Option
    // -----------------------------------------------------------------------

    @Nested
    class OptionXmlAdapterTests {

        private final OptionXmlAdapter adapter = new OptionXmlAdapter();

        @Test
        void someMarshalsToElementWithValue() throws Exception {
            var element = adapter.marshal(Option.some("alice"));
            assertThat(element).isNotNull();
            assertThat(element.value).isEqualTo("alice");
        }

        @Test
        void noneMarshalsToNull() throws Exception {
            var element = adapter.marshal(Option.none());
            assertThat(element).isNull();
        }

        @Test
        void nullMarshalsToNull() throws Exception {
            var element = adapter.marshal(null);
            assertThat(element).isNull();
        }

        @Test
        void elementWithValueUnmarshalsAsSome() throws Exception {
            var element = new OptionXmlAdapter.OptionElement();
            element.value = "bob";
            var result = adapter.unmarshal(element);
            assertThat(result).isSome();
            assertThat(result.get()).isEqualTo("bob");
        }

        @Test
        void elementWithNullValueUnmarshalsAsNone() throws Exception {
            var element = new OptionXmlAdapter.OptionElement();
            var result = adapter.unmarshal(element);
            assertThat(result).isNone();
        }

        @Test
        void nullElementUnmarshalsAsNone() throws Exception {
            var result = adapter.unmarshal(null);
            assertThat(result).isNone();
        }
    }

    // -----------------------------------------------------------------------
    // Result
    // -----------------------------------------------------------------------

    @Nested
    class ResultXmlAdapterTests {

        private final ResultXmlAdapter adapter = new ResultXmlAdapter();

        @Test
        void okMarshalsToElementWithOkChild() throws Exception {
            var element = adapter.marshal(Result.ok(42));
            assertThat(element).isNotNull();
            assertThat(element.ok).isEqualTo("42");
            assertThat(element.err).isNull();
        }

        @Test
        void errMarshalsToElementWithErrChild() throws Exception {
            var element = adapter.marshal(Result.err("oops"));
            assertThat(element).isNotNull();
            assertThat(element.ok).isNull();
            assertThat(element.err).isEqualTo("oops");
        }

        @Test
        void nullMarshalsToNull() throws Exception {
            var element = adapter.marshal(null);
            assertThat(element).isNull();
        }

        @Test
        void elementWithOkUnmarshalsAsOk() throws Exception {
            var element = new ResultXmlAdapter.ResultElement();
            element.ok = "val";
            var result = adapter.unmarshal(element);
            assertThat(result).isOk();
        }

        @Test
        void elementWithErrUnmarshalsAsErr() throws Exception {
            var element = new ResultXmlAdapter.ResultElement();
            element.err = "fail";
            var result = adapter.unmarshal(element);
            assertThat(result).isErr();
        }

        @Test
        void nullElementThrows() {
            assertThatThrownBy(() -> adapter.unmarshal(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void elementWithBothChildrenThrows() {
            var element = new ResultXmlAdapter.ResultElement();
            element.ok = "v";
            element.err = "e";
            assertThatThrownBy(() -> adapter.unmarshal(element))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ambiguous");
        }

        @Test
        void elementWithNeitherChildThrows() {
            assertThatThrownBy(() -> adapter.unmarshal(new ResultXmlAdapter.ResultElement()))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // Try
    // -----------------------------------------------------------------------

    @Nested
    class TryXmlAdapterTests {

        private final TryXmlAdapter adapter = new TryXmlAdapter();

        @Test
        void successMarshalsToElementWithValueChild() throws Exception {
            var element = adapter.marshal(Try.success("done"));
            assertThat(element).isNotNull();
            assertThat(element.value).isEqualTo("done");
            assertThat(element.error).isNull();
        }

        @Test
        void failureMarshalsToElementWithErrorChild() throws Exception {
            var element = adapter.marshal(Try.failure(new RuntimeException("boom")));
            assertThat(element).isNotNull();
            assertThat(element.value).isNull();
            assertThat(element.error).isEqualTo("boom");
        }

        @Test
        void nullMarshalsToNull() throws Exception {
            var element = adapter.marshal(null);
            assertThat(element).isNull();
        }

        @Test
        void elementWithValueUnmarshalsAsSuccess() throws Exception {
            var element = new TryXmlAdapter.TryElement();
            element.value = "ok";
            var result = adapter.unmarshal(element);
            assertThat(result).isSuccess();
        }

        @Test
        void elementWithErrorUnmarshalsAsFailure() throws Exception {
            var element = new TryXmlAdapter.TryElement();
            element.error = "oops";
            var result = adapter.unmarshal(element);
            assertThat(result).isFailure();
        }

        @Test
        void nullElementUnmarshalsAsFailure() throws Exception {
            var result = adapter.unmarshal(null);
            assertThat(result).isFailure();
        }

        @Test
        void elementWithBothChildrenUnmarshalsAsFailure() throws Exception {
            var element = new TryXmlAdapter.TryElement();
            element.value = "v";
            element.error = "e";
            var result = adapter.unmarshal(element);
            assertThat(result).isFailure();
            assertThat(result.getCause().getMessage()).contains("Ambiguous");
        }
    }

    // -----------------------------------------------------------------------
    // Either
    // -----------------------------------------------------------------------

    @Nested
    class EitherXmlAdapterTests {

        private final EitherXmlAdapter adapter = new EitherXmlAdapter();

        @Test
        void rightMarshalsToElementWithRightChild() throws Exception {
            var element = adapter.marshal(Either.right("yes"));
            assertThat(element).isNotNull();
            assertThat(element.right).isEqualTo("yes");
            assertThat(element.left).isNull();
        }

        @Test
        void leftMarshalsToElementWithLeftChild() throws Exception {
            var element = adapter.marshal(Either.left("no"));
            assertThat(element).isNotNull();
            assertThat(element.left).isEqualTo("no");
            assertThat(element.right).isNull();
        }

        @Test
        void nullMarshalsToNull() throws Exception {
            var element = adapter.marshal(null);
            assertThat(element).isNull();
        }

        @Test
        void elementWithRightUnmarshalsAsRight() throws Exception {
            var element = new EitherXmlAdapter.EitherElement();
            element.right = "r";
            var result = adapter.unmarshal(element);
            assertThat(result).isRight();
        }

        @Test
        void elementWithLeftUnmarshalsAsLeft() throws Exception {
            var element = new EitherXmlAdapter.EitherElement();
            element.left = "l";
            var result = adapter.unmarshal(element);
            assertThat(result).isLeft();
        }

        @Test
        void nullElementThrows() {
            assertThatThrownBy(() -> adapter.unmarshal(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void elementWithBothChildrenThrows() {
            var element = new EitherXmlAdapter.EitherElement();
            element.left = "l";
            element.right = "r";
            assertThatThrownBy(() -> adapter.unmarshal(element))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ambiguous");
        }

        @Test
        void elementWithNeitherChildThrows() {
            assertThatThrownBy(() -> adapter.unmarshal(new EitherXmlAdapter.EitherElement()))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
