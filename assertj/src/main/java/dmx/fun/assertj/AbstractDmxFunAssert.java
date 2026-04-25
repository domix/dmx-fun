package dmx.fun.assertj;

import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;

@NullMarked
abstract class AbstractDmxFunAssert<SELF extends AbstractDmxFunAssert<SELF, ACTUAL>, ACTUAL>
    extends AbstractAssert<SELF, ACTUAL> {

    AbstractDmxFunAssert(ACTUAL actual, Class<?> selfType) {
        super(actual, selfType);
    }

    final AssertionError buildError(String template, Object... args) {
        String message = String.format(template.replace("<%s>", "%s"), args);
        String description = info.descriptionText();
        return new AssertionError(description.isEmpty() ? message : "[" + description + "] " + message);
    }
}
