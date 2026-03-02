package codes.domix.fun;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the JPMS module descriptor for {@code codes.domix.fun}.
 *
 * <p>Uses {@link ModuleFinder} to inspect the compiled {@code module-info.class}
 * directly from the build output, so these tests work regardless of whether
 * the test suite itself runs on the module path or the classpath.
 */
class ModuleTest {

    private static final String MODULE_NAME = "codes.domix.fun";

    private static ModuleDescriptor loadDescriptor() {
        // Gradle's test working directory is the subproject root (lib/)
        Path classes = Path.of("build/classes/java/main");
        ModuleFinder finder = ModuleFinder.of(classes);
        Optional<ModuleReference> ref = finder.find(MODULE_NAME);
        assertTrue(ref.isPresent(), "Module '" + MODULE_NAME + "' not found in " + classes.toAbsolutePath());
        return ref.get().descriptor();
    }

    @Test
    void module_hasCorrectName() {
        assertEquals(MODULE_NAME, loadDescriptor().name());
    }

    @Test
    void module_exportsPublicPackage() {
        Set<String> exported = loadDescriptor().exports().stream()
            .map(ModuleDescriptor.Exports::source)
            .collect(Collectors.toSet());

        assertTrue(exported.contains("codes.domix.fun"),
            "Expected 'codes.domix.fun' to be exported, but exports were: " + exported);
    }

    @Test
    void module_requiresJSpecify() {
        Set<String> required = loadDescriptor().requires().stream()
            .map(ModuleDescriptor.Requires::name)
            .collect(Collectors.toSet());

        assertTrue(required.contains("org.jspecify"),
            "Expected 'org.jspecify' to be required, but requires were: " + required);
    }

    @Test
    void module_doesNotExportInternalPackages() {
        Set<String> exported = loadDescriptor().exports().stream()
            .map(ModuleDescriptor.Exports::source)
            .collect(Collectors.toSet());

        assertEquals(Set.of("codes.domix.fun"), exported,
            "Module exports more packages than expected");
    }
}
