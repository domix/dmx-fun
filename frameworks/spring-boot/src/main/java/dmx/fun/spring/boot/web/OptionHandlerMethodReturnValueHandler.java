package dmx.fun.spring.boot.web;

import dmx.fun.Option;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Spring MVC {@link HandlerMethodReturnValueHandler} that converts {@link Option} return
 * values into HTTP responses automatically:
 *
 * <ul>
 *   <li>{@link Option#some(Object)} → HTTP 200 with the unwrapped value serialized by the
 *       delegate {@link HandlerMethodReturnValueHandler} (typically
 *       {@code RequestResponseBodyMethodProcessor}).</li>
 *   <li>{@link Option#none()} → HTTP 404 with an empty body.</li>
 * </ul>
 *
 * <p>This handler is registered automatically by {@link DmxFunWebMvcAutoConfiguration}
 * before the built-in body processor, so controller methods in {@code @RestController}
 * classes can declare {@code Option<T>} as their return type and receive the correct
 * HTTP semantics without any extra boilerplate.
 *
 * <p>Example:
 * <pre>{@code
 * @GetMapping("/{id}")
 * public Option<Item> findById(@PathVariable("id") Long id) {
 *     return service.findById(id);   // some → 200, none → 404
 * }
 * }</pre>
 */
@NullMarked
public final class OptionHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

    private final HandlerMethodReturnValueHandler delegate;

    /**
     * Creates a handler that unwraps {@link Option} return values and delegates
     * serialization of the unwrapped value to the given handler.
     *
     * @param delegate the handler that processes the unwrapped value — typically a
     *                 {@code RequestResponseBodyMethodProcessor} that writes the body
     *                 using the configured {@code HttpMessageConverter}s
     */
    public OptionHandlerMethodReturnValueHandler(HandlerMethodReturnValueHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return Option.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public void handleReturnValue(
            @Nullable Object returnValue,
            MethodParameter returnType,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest) throws Exception {

        var option = (Option<?>) returnValue;

        if (option != null && option.isDefined()) {
            delegate.handleReturnValue(option.get(), returnType, mavContainer, webRequest);
        } else {
            mavContainer.setRequestHandled(true);
            HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
            if (response != null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }
}
