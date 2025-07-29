package rocks.inspectit.ocelot.file.versioning;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import rocks.inspectit.ocelot.file.FileManager;

/**
 * Allows to work with additional git features.
 * <p>
 * The {@code .gitignore} file will be created once during start-up by the {@link FileManager} if non-existing.
 * The file will not be modified by the configuration-server itself after that.
 */
@Slf4j
public class GitUtil {

    public static final String GIT_IGNORE_FILE_NAME = ".gitignore";

    /**
     * Creates a {@code .gitignore} file in the working directory to exclude the {@code users.db} file
     * from versioning.
     *
     * @param workingDirectory the configured working directory
     */
    public static void createGitIgnore(Path workingDirectory) {
        Path gitIgnore = workingDirectory.resolve(GIT_IGNORE_FILE_NAME).normalize();

        if (Files.notExists(gitIgnore)) {
            log.info("Creating file: {}", gitIgnore);
            String content = "users.db" + System.lineSeparator();
            try {
                Files.writeString(gitIgnore, content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Could not create .gitignore: {}", e.getMessage());
            }
        }
    }
}
