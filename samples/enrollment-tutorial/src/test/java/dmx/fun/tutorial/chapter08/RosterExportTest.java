package dmx.fun.tutorial.chapter08;

import dmx.fun.NonEmptyList;
import dmx.fun.Resource;
import dmx.fun.tutorial.domain.Enrollment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 8: the file is written and closed, or not written and still closed. */
class RosterExportTest {

    private static final NonEmptyList<Enrollment> ROSTER =
        NonEmptyList.of(new Enrollment("s1", "c1"), List.of(new Enrollment("s2", "c1")));

    @Test
    void writesOneStudentPerLine(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("roster.txt");

        var result = RosterExport.write(target, "c1", ROSTER);

        assertThat(result).isSuccess().containsValue(target);
        assertThat(Files.readAllLines(target)).containsExactly("# roster for c1", "s1", "s2");
    }

    @Test
    void unwritablePath_isAFailure_notAnException(@TempDir Path dir) {
        Path target = dir.resolve("missing-directory").resolve("roster.txt");

        var result = RosterExport.write(target, "c1", ROSTER);

        assertThat(result).isFailure();
    }

    @Test
    void releaseRuns_evenWhenTheBodyThrows() {
        var released = new AtomicBoolean(false);
        var resource = Resource.of(() -> "handle", _ -> released.set(true));

        var result = resource.use(_ -> { throw new IllegalStateException("boom"); });

        assertThat(result).isFailure();
        assertThat(released).isTrue();
    }
}
