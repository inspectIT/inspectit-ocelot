package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Abstract class for reading and modifying files in the server's working directory.
 */
@Slf4j
public abstract class AbstractWorkingDirectoryAccessor extends AbstractFileAccessor {

    /**
     * Writes the given content to the specified file. The file will be overwritten, if it already exists.
     *
     * @param path    the target file
     * @param content the content to write
     * @throws IOException in case the file can not be written
     */
    protected abstract void writeFile(String path, String content) throws IOException;

    /**
     * Creates the specified directory.
     *
     * @param path the directory to create
     * @throws IOException in case the file can not be written
     */
    protected abstract void createDirectory(String path) throws IOException;

    /**
     * Moves the specified source path to the specified target - can be a file or directory.
     *
     * @param sourcePath the source path
     * @param targetPath the target path
     * @throws IOException in case the file or directory can not be moved
     */
    protected abstract void move(String sourcePath, String targetPath) throws IOException;

    /**
     * Deletes the specified path - can be a file or directory.
     *
     * @param path the path to delete
     * @throws IOException in case the file or directory can not be deleted
     */
    protected abstract void delete(String path) throws IOException;

    /**
     * Creating a new configuration directory.
     *
     * @param directory the directory to create
     * @throws IOException in case the directory can not be created
     */
    public void createConfigurationDirectory(String directory) throws IOException {
        log.debug("Creating configuration directory: {}", directory);
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, directory);

        createDirectory(targetPath);
    }

    /**
     * Writes the given content to the specified configuration file. The file will be overwritten, if it already exists.
     * If the file does not exist, it will be created. In case a directory exists at the target location, the operation
     * will result in an exception.
     *
     * @param file    the file to write
     * @param content the content to write
     * @throws IOException in case the file can not be written
     */
    public void writeConfigurationFile(String file, String content) throws IOException {
        log.debug("Writing configuration file: {}", file);
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, file);

        writeFile(targetPath, content);
    }

    /**
     * Deletes the specified configuration file or directory.
     *
     * @param path the configuration to delete
     * @throws IOException in case the file or directory can not be deleted
     */
    public void deleteConfiguration(String path) throws IOException {
        log.debug("Deleting configuration: {}", path);
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, path);

        if (Paths.get(path).normalize().toString().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete base directory: " + path);
        }

        delete(targetPath);
    }

    /**
     * Moves the specified source to the target location. The source can be a file or directory.
     *
     * @param source the source item to move
     * @param target the target item
     * @throws IOException in case the file or directory can not be moved
     */
    public void moveConfiguration(String source, String target) throws IOException {
        log.debug("Moving configuration: {} -> {}", source, target);
        String sourcePath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, source);
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, target);

        move(sourcePath, targetPath);
    }

    /**
     * Writes the given content to the agent mappings file. The current file will be overwritten.
     *
     * @param content the content to write
     * @throws IOException in case the agent mappings can not be written
     */
    public void writeAgentMappings(String content) throws IOException {
        log.debug("Writing agent mappings");
        writeFile(AGENT_MAPPINGS_FILE_NAME, content);
    }
}
