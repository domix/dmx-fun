package dmx.fun.quarkus;

import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Minimal {@link InvocationContext} stub for unit-testing interceptors without a CDI container.
 */
class StubInvocationContext implements InvocationContext {

    private final Object target;
    private final Method method;
    private final Callable<?> action;

    StubInvocationContext(Object target, Method method, Callable<?> action) {
        this.target = target;
        this.method = method;
        this.action = action;
    }

    @Override public Object getTarget()  { return target; }
    @Override public Method getMethod()  { return method; }
    @Override public Object proceed() throws Exception { return action.call(); }

    @Override public Object getTimer()                       { throw new UnsupportedOperationException(); }
    @Override public Constructor<?> getConstructor()         { throw new UnsupportedOperationException(); }
    @Override public Object[] getParameters()                { return new Object[0]; }
    @Override public void setParameters(Object[] params)     {}
    @Override public Map<String, Object> getContextData()    { return Map.of(); }
    @Override public Set<Annotation> getInterceptorBindings(){ return Set.of(); }
}
