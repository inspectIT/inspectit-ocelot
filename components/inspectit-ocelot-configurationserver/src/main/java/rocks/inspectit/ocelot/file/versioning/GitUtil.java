package rocks.inspectit.ocelot.file.versioning;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class GitUtil {

    /**
     * Creates a {@code .gitignore} file in the working directory to exclude the {@code users.db} file
     * from versioning.
     *
     * @param workingDirectory the configured working directory
     */
    public static void createGitIgnore(Path workingDirectory) throws IOException {
        Path gitIgnore = workingDirectory.resolve(".gitignore").normalize();

        if (Files.notExists(gitIgnore)) {
            log.info("Creating file: {}", gitIgnore);
            Files.writeString(gitIgnore, "users.db\n");
        }
    }
}
