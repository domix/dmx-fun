package dmx.fun.quarkus;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Internal meta interceptor binding shared by {@link TransactionalResult} and
 * {@link TransactionalTry}.
 *
 * <p>Per CDI §2.7.1.1, an interceptor binding type may transitively declare another
 * interceptor binding type. Both {@link TransactionalResult} and {@link TransactionalTry}
 * carry this annotation, so {@link TransactionalDmxInterceptor} — which is bound to
 * {@code @DmxTransactionalBinding} — activates for methods annotated with either one.
 */
@InterceptorBinding
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface DmxTransactionalBinding {}
