package dmx.fun.quarkus.deployment;

import dmx.fun.quarkus.TransactionalDmxInterceptor;
import dmx.fun.quarkus.TxResult;
import dmx.fun.quarkus.TxTry;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jspecify.annotations.NullMarked;

/**
 * Quarkus build-time processor for the {@code dmx-fun-quarkus} extension.
 *
 * <p>Registers the extension feature flag and ensures that {@link TxResult},
 * {@link TxTry}, and {@link TransactionalDmxInterceptor} are not removed by
 * Arc's bean removal optimisation, even when no injection point references them
 * directly.
 */
@NullMarked
public class DmxFunQuarkusProcessor {

    private static final String FEATURE = "dmx-fun-quarkus";

    /**
     * Declares the Quarkus feature name shown in the startup log.
     *
     * @return the {@link FeatureBuildItem} for this extension
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Marks {@link TxResult}, {@link TxTry}, and {@link TransactionalDmxInterceptor}
     * as unremovable so Arc retains them regardless of whether a direct injection
     * point exists.
     *
     * @return the {@link AdditionalBeanBuildItem} registering the beans
     */
    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
            .setUnremovable()
            .addBeanClasses(TxResult.class, TxTry.class, TransactionalDmxInterceptor.class)
            .build();
    }
}
