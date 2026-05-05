package dmx.fun.quarkus;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the annotation meta-structure that wires the CDI interceptor stack.
 * These checks guard against accidental annotation removal or misplacement.
 */
class AnnotationsTest {

    // ── @DmxTransactionalBinding ──────────────────────────────────────────────

    @Test
    void dmxTransactionalBinding_hasInterceptorBinding() {
        assertThat(DmxTransactionalBinding.class.isAnnotationPresent(InterceptorBinding.class)).isTrue();
    }

    @Test
    void dmxTransactionalBinding_hasRuntimeRetention() {
        var retention = DmxTransactionalBinding.class.getAnnotation(Retention.class);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void dmxTransactionalBinding_isInherited() {
        assertThat(DmxTransactionalBinding.class.isAnnotationPresent(Inherited.class)).isTrue();
    }

    // ── @TransactionalResult ──────────────────────────────────────────────────

    @Test
    void transactionalResult_hasInterceptorBinding() {
        assertThat(TransactionalResult.class.isAnnotationPresent(InterceptorBinding.class)).isTrue();
    }

    @Test
    void transactionalResult_hasDmxTransactionalBinding() {
        assertThat(TransactionalResult.class.isAnnotationPresent(DmxTransactionalBinding.class)).isTrue();
    }

    @Test
    void transactionalResult_hasRuntimeRetention() {
        var retention = TransactionalResult.class.getAnnotation(Retention.class);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void transactionalResult_isInherited() {
        assertThat(TransactionalResult.class.isAnnotationPresent(Inherited.class)).isTrue();
    }

    // ── @TransactionalTry ─────────────────────────────────────────────────────

    @Test
    void transactionalTry_hasInterceptorBinding() {
        assertThat(TransactionalTry.class.isAnnotationPresent(InterceptorBinding.class)).isTrue();
    }

    @Test
    void transactionalTry_hasDmxTransactionalBinding() {
        assertThat(TransactionalTry.class.isAnnotationPresent(DmxTransactionalBinding.class)).isTrue();
    }

    @Test
    void transactionalTry_hasRuntimeRetention() {
        var retention = TransactionalTry.class.getAnnotation(Retention.class);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void transactionalTry_isInherited() {
        assertThat(TransactionalTry.class.isAnnotationPresent(Inherited.class)).isTrue();
    }

    // ── TransactionalDmxInterceptor ───────────────────────────────────────────

    @Test
    void interceptor_hasInterceptorAnnotation() {
        assertThat(TransactionalDmxInterceptor.class.isAnnotationPresent(Interceptor.class)).isTrue();
    }

    @Test
    void interceptor_hasDmxTransactionalBinding() {
        assertThat(TransactionalDmxInterceptor.class.isAnnotationPresent(DmxTransactionalBinding.class)).isTrue();
    }

    @Test
    void interceptor_hasExpectedPriority() {
        var priority = TransactionalDmxInterceptor.class.getAnnotation(Priority.class);
        assertThat(priority).isNotNull();
        assertThat(priority.value()).isEqualTo(Interceptor.Priority.APPLICATION + 100);
    }

    @Test
    void interceptor_constructor_hasInject() throws NoSuchMethodException {
        var ctor = TransactionalDmxInterceptor.class
            .getDeclaredConstructor(jakarta.transaction.TransactionManager.class);
        assertThat(ctor.isAnnotationPresent(Inject.class)).isTrue();
    }

    // ── TxResult & TxTry CDI scope ────────────────────────────────────────────

    @Test
    void txResult_isApplicationScoped() {
        assertThat(TxResult.class.isAnnotationPresent(ApplicationScoped.class)).isTrue();
    }

    @Test
    void txResult_injectConstructor_hasInject() throws NoSuchMethodException {
        var ctor = TxResult.class.getDeclaredConstructor(jakarta.transaction.TransactionManager.class);
        assertThat(ctor.isAnnotationPresent(Inject.class)).isTrue();
    }

    @Test
    void txTry_isApplicationScoped() {
        assertThat(TxTry.class.isAnnotationPresent(ApplicationScoped.class)).isTrue();
    }

    @Test
    void txTry_injectConstructor_hasInject() throws NoSuchMethodException {
        var ctor = TxTry.class.getDeclaredConstructor(jakarta.transaction.TransactionManager.class);
        assertThat(ctor.isAnnotationPresent(Inject.class)).isTrue();
    }
}
