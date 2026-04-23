package dmx.fun.spring.boot.web;

import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.assertj.core.api.Assertions.assertThat;

class ResultHandlerMethodReturnValueHandlerTest {

    static Result<String, String> resultMethod() { return Result.ok(""); }
    static Validated<String, String> validatedMethod() { return Validated.valid(""); }
    static Try<String> tryMethod() { return Try.success(""); }
    static String stringMethod() { return ""; }

    private MethodParameter paramFor(String name) throws Exception {
        return new MethodParameter(
            ResultHandlerMethodReturnValueHandlerTest.class.getDeclaredMethod(name), -1);
    }

    @Test
    void supportsResultValidatedAndTry() throws Exception {
        var handler = new ResultHandlerMethodReturnValueHandler(new RecordingDelegate(true));

        assertThat(handler.supportsReturnType(paramFor("resultMethod"))).isTrue();
        assertThat(handler.supportsReturnType(paramFor("validatedMethod"))).isTrue();
        assertThat(handler.supportsReturnType(paramFor("tryMethod"))).isTrue();
    }

    @Test
    void doesNotSupportWrapperTypesWhenDelegateDoesNotSupport() throws Exception {
        var handler = new ResultHandlerMethodReturnValueHandler(new RecordingDelegate(false));

        assertThat(handler.supportsReturnType(paramFor("resultMethod"))).isFalse();
        assertThat(handler.supportsReturnType(paramFor("validatedMethod"))).isFalse();
        assertThat(handler.supportsReturnType(paramFor("tryMethod"))).isFalse();
    }

    @Test
    void doesNotSupportOtherTypes() throws Exception {
        var handler = new ResultHandlerMethodReturnValueHandler(new RecordingDelegate(false));
        assertThat(handler.supportsReturnType(paramFor("stringMethod"))).isFalse();
    }

    @Test
    void resultOk_delegatesValueWithout500() throws Exception {
        var delegate = new RecordingDelegate(true);
        var handler = new ResultHandlerMethodReturnValueHandler(delegate);
        var response = new MockHttpServletResponse();
        var webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);
        int initialStatus = response.getStatus();

        handler.handleReturnValue(Result.ok("ok"), paramFor("resultMethod"),
            new ModelAndViewContainer(), webRequest);

        assertThat(delegate.calls).containsExactly("ok");
        assertThat(response.getStatus()).isEqualTo(initialStatus);
    }

    @Test
    void resultErr_sets500AndDelegatesError() throws Exception {
        var delegate = new RecordingDelegate(true);
        var handler = new ResultHandlerMethodReturnValueHandler(delegate);
        var response = new MockHttpServletResponse();
        var webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);

        handler.handleReturnValue(Result.err("boom"), paramFor("resultMethod"),
            new ModelAndViewContainer(), webRequest);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(delegate.calls).containsExactly("boom");
    }

    @Test
    void validatedValid_delegatesValueWithout500() throws Exception {
        var delegate = new RecordingDelegate(true);
        var handler = new ResultHandlerMethodReturnValueHandler(delegate);
        var response = new MockHttpServletResponse();
        var webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);
        int initialStatus = response.getStatus();

        handler.handleReturnValue(Validated.valid("valid"), paramFor("validatedMethod"),
            new ModelAndViewContainer(), webRequest);

        assertThat(delegate.calls).containsExactly("valid");
        assertThat(response.getStatus()).isEqualTo(initialStatus);
    }

    @Test
    void validatedInvalid_sets500AndDelegatesError() throws Exception {
        var delegate = new RecordingDelegate(true);
        var handler = new ResultHandlerMethodReturnValueHandler(delegate);
        var response = new MockHttpServletResponse();
        var webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);

        handler.handleReturnValue(Validated.invalid("invalid"), paramFor("validatedMethod"),
            new ModelAndViewContainer(), webRequest);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(delegate.calls).containsExactly("invalid");
    }

    @Test
    void trySuccess_delegatesValueWithout500() throws Exception {
        var delegate = new RecordingDelegate(true);
        var handler = new ResultHandlerMethodReturnValueHandler(delegate);
        var response = new MockHttpServletResponse();
        var webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);
        int initialStatus = response.getStatus();

        handler.handleReturnValue(Try.success("ok"), paramFor("tryMethod"),
            new ModelAndViewContainer(), webRequest);

        assertThat(delegate.calls).containsExactly("ok");
        assertThat(response.getStatus()).isEqualTo(initialStatus);
    }

    @Test
    void tryFailure_sets500AndDelegatesCause() throws Exception {
        var delegate = new RecordingDelegate(true);
        var handler = new ResultHandlerMethodReturnValueHandler(delegate);
        var response = new MockHttpServletResponse();
        var webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);
        var ex = new IllegalStateException("boom");

        handler.handleReturnValue(Try.failure(ex), paramFor("tryMethod"),
            new ModelAndViewContainer(), webRequest);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(delegate.calls).containsExactly(ex);
    }

    @Test
    void supportsOtherTypesWhenDelegateSupportsThem() throws Exception {
        var handler = new ResultHandlerMethodReturnValueHandler(new RecordingDelegate(true));
        assertThat(handler.supportsReturnType(paramFor("stringMethod"))).isFalse();
    }

    static class RecordingDelegate implements HandlerMethodReturnValueHandler {
        final List<Object> calls = new ArrayList<>();
        private final boolean supportsReturnType;

        RecordingDelegate(boolean supportsReturnType) {
            this.supportsReturnType = supportsReturnType;
        }

        @Override
        public boolean supportsReturnType(MethodParameter returnType) { return supportsReturnType; }

        @Override
        public void handleReturnValue(Object returnValue, MethodParameter returnType,
                ModelAndViewContainer mavContainer, org.springframework.web.context.request.NativeWebRequest webRequest) {
            calls.add(returnValue);
            mavContainer.setRequestHandled(true);
        }
    }
}
