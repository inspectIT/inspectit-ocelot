package rocks.inspectit.ocelot.file.accessor;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.file.FileInfo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class AbstractFileAccessor {

    public static final Charset FILE_ENCODING = StandardCharsets.UTF_8;

    public static final String AGENT_MAPPINGS_FILE_NAME = "agent_mappings.yaml";

    /**
     * The subfolder within the working directory which acts as
     * filesRoot for the files and directories managed by this class.
     */
    public static final String CONFIGURATION_FILES_SUBFOLDER = "files";

    public Optional<String> readConfigurationFile(String file) {
        log.debug("Reading configuration file: {}", file);
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, file);

        Optional<byte[]> fileContent = readFile(targetPath);

        return fileContent.map(String::new);
    }

    public Optional<List<FileInfo>> listConfigurationFiles(String path) {
        log.debug("Listing configuration files: {}", path);
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, path);

        List<FileInfo> files = listFiles(targetPath);

        return Optional.ofNullable(files);
    }

    public Optional<String> readAgentMappings() {
        log.debug("Reading agent mappings");

        Optional<byte[]> fileContent = readFile(AGENT_MAPPINGS_FILE_NAME);

        return fileContent.map(bytes -> new String(bytes, FILE_ENCODING));
    }

    public boolean configurationFileExists(String path) {
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, path);

        return exists(targetPath);
    }

    public boolean configurationFileIsDirectory(String path) {
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, path);

        return isDirectory(targetPath);
    }

    protected abstract String verifyPath(String relativeBasePath, String path);

    protected abstract Optional<byte[]> readFile(String path);

    protected abstract List<FileInfo> listFiles(String path);

    protected abstract boolean exists(String path);

    protected abstract boolean isDirectory(String path);
}
