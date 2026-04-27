package dmx.fun.spring.boot.web;

import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Spring MVC {@link HandlerMethodReturnValueHandler} for {@link Result}, {@link Validated},
 * and {@link Try} return values.
 *
 * <p>Maps successful outcomes to HTTP 200 and error outcomes to HTTP 500 while delegating
 * body serialization to a standard body processor.
 */
@NullMarked
public final class ResultHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

    private final HandlerMethodReturnValueHandler delegate;

    /**
     * Creates a handler that unwraps {@link dmx.fun.Result}, {@link dmx.fun.Validated}, and
     * {@link dmx.fun.Try} return values and delegates serialization of the unwrapped value (or
     * error) to the given handler.
     *
     * @param delegate the handler that writes the response body — typically a
     *                 {@code RequestResponseBodyMethodProcessor}
     */
    public ResultHandlerMethodReturnValueHandler(HandlerMethodReturnValueHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        Class<?> type = returnType.getParameterType();
        boolean wrapperType = Result.class.isAssignableFrom(type)
            || Validated.class.isAssignableFrom(type)
            || Try.class.isAssignableFrom(type);
        return wrapperType && delegate.supportsReturnType(returnType);
    }

    @Override
    public void handleReturnValue(
            @Nullable Object returnValue,
            MethodParameter returnType,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest) throws Exception {

        if (returnValue instanceof Result<?, ?> result) {
            if (result.isOk()) {
                delegate.handleReturnValue(result.get(), returnType, mavContainer, webRequest);
            } else {
                set500(webRequest);
                delegate.handleReturnValue(result.getError(), returnType, mavContainer, webRequest);
            }
            return;
        }

        if (returnValue instanceof Validated<?, ?> validated) {
            if (validated.isValid()) {
                delegate.handleReturnValue(validated.get(), returnType, mavContainer, webRequest);
            } else {
                set500(webRequest);
                delegate.handleReturnValue(validated.getError(), returnType, mavContainer, webRequest);
            }
            return;
        }

        if (returnValue instanceof Try<?> tried) {
            if (tried.isSuccess()) {
                delegate.handleReturnValue(tried.get(), returnType, mavContainer, webRequest);
            } else {
                set500(webRequest);
                delegate.handleReturnValue(tried.getCause(), returnType, mavContainer, webRequest);
            }
            return;
        }

        set500(webRequest);
        mavContainer.setRequestHandled(true);
    }

    private static void set500(NativeWebRequest webRequest) {
        var response = webRequest.getNativeResponse(HttpServletResponse.class);
        if (response != null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
