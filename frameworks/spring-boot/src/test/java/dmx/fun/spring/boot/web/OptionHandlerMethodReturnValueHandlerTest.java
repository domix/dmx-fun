package dmx.fun.spring.boot.web;

import dmx.fun.Option;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OptionHandlerMethodReturnValueHandlerTest {

    // Fixture methods used for MethodParameter reflection
    static Option<String> optionMethod() { return Option.none(); }
    static String stringMethod() { return ""; }

    private MethodParameter paramFor(String name) throws Exception {
        return new MethodParameter(
            OptionHandlerMethodReturnValueHandlerTest.class.getDeclaredMethod(name), -1);
    }

    // ── supportsReturnType ────────────────────────────────────────────────────

    @Test
    void supportsOption() throws Exception {
        var handler = new OptionHandlerMethodReturnValueHandler(new RecordingDelegate());
        assertThat(handler.supportsReturnType(paramFor("optionMethod"))).isTrue();
    }

    @Test
    void doesNotSupportNonOption() throws Exception {
        var handler = new OptionHandlerMethodReturnValueHandler(new RecordingDelegate());
        assertThat(handler.supportsReturnType(paramFor("stringMethod"))).isFalse();
    }

    // ── handleReturnValue — None ──────────────────────────────────────────────

    @Test
    void none_sets404AndMarksRequestHandled() throws Exception {
        var delegate = new RecordingDelegate();
        var handler = new OptionHandlerMethodReturnValueHandler(delegate);
        var mavContainer = new ModelAndViewContainer();
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var webRequest = new ServletWebRequest(request, response);

        handler.handleReturnValue(Option.none(), paramFor("optionMethod"), mavContainer, webRequest);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(mavContainer.isRequestHandled()).isTrue();
        assertThat(delegate.calls).isEmpty();
    }

    // ── handleReturnValue — Some ──────────────────────────────────────────────

    @Test
    void some_delegatesUnwrappedValue() throws Exception {
        var delegate = new RecordingDelegate();
        var handler = new OptionHandlerMethodReturnValueHandler(delegate);
        var mavContainer = new ModelAndViewContainer();
        var webRequest = new ServletWebRequest(
            new MockHttpServletRequest(), new MockHttpServletResponse());
        var param = paramFor("optionMethod");

        handler.handleReturnValue(Option.some("hello"), param, mavContainer, webRequest);

        assertThat(delegate.calls).hasSize(1);
        assertThat(delegate.calls.get(0)).isEqualTo("hello");
    }

    @Test
    void some_doesNotSet404() throws Exception {
        var delegate = new RecordingDelegate();
        var handler = new OptionHandlerMethodReturnValueHandler(delegate);
        var response = new MockHttpServletResponse();
        var webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);

        int initialStatus = response.getStatus();
        handler.handleReturnValue(Option.some("x"), paramFor("optionMethod"),
            new ModelAndViewContainer(), webRequest);

        assertThat(response.getStatus()).isEqualTo(initialStatus);
    }

    // ── Stub delegate ─────────────────────────────────────────────────────────

    static class RecordingDelegate implements HandlerMethodReturnValueHandler {
        final List<Object> calls = new ArrayList<>();

        @Override
        public boolean supportsReturnType(MethodParameter returnType) { return true; }

        @Override
        public void handleReturnValue(Object returnValue, MethodParameter returnType,
                ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
            calls.add(returnValue);
            mavContainer.setRequestHandled(true);
        }
    }
}
