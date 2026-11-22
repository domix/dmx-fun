package dmx.fun.tutorial.chapter08;

import dmx.fun.NonEmptyList;
import dmx.fun.Resource;
import dmx.fun.Try;
import dmx.fun.tutorial.domain.Enrollment;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes a roster to a file, one student per line.
 *
 * <p>{@code Resource} pairs an acquire step with a release step and runs a body in between.
 * Whatever the body does, succeed or throw, the writer is closed, and the outcome comes back as
 * a {@code Try}: this is the {@code try-with-resources} idea as a value you can map, combine
 * and return, instead of a statement you have to be inside of.
 */
public final class RosterExport {

    private RosterExport() {}

    public static Try<Path> write(Path target, String courseId, NonEmptyList<Enrollment> roster) {
        return Resource.<BufferedWriter>fromAutoCloseable(() -> Files.newBufferedWriter(target))
            .use(writer -> {
                writer.write("# roster for " + courseId);
                writer.newLine();
                for (Enrollment e : roster.toList()) {
                    writer.write(e.studentId());
                    writer.newLine();
                }
                return target;
            });
    }
}
