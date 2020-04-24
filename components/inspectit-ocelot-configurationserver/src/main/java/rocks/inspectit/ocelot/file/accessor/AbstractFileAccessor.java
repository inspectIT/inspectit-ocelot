package rocks.inspectit.ocelot.file.accessor;

import rocks.inspectit.ocelot.file.FileInfo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public abstract class AbstractFileAccessor {

    public static final Charset FILE_ENCODING = StandardCharsets.UTF_8;

    public static final String AGENT_MAPPINGS_FILE_NAME = "agent_mappings.yaml";

    public static final String CONFIGURATION_FILES_SUBFOLDER = "files";

    public Optional<String> readConfigurationFile(String file) {
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, file);

        Optional<byte[]> fileContent = readFile(targetPath);

        return fileContent.map(String::new);
    }

    public Optional<List<FileInfo>> listConfigurationFiles(String path) {
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, path);

        List<FileInfo> files = listFiles(targetPath);

        return Optional.ofNullable(files);
    }

    public Optional<String> readAgentMappings() {
        Optional<byte[]> fileContent = readFile(AGENT_MAPPINGS_FILE_NAME);

        return fileContent.map(bytes -> new String(bytes, FILE_ENCODING));
    }

    protected abstract String verifyPath(String relativeBasePath, String path);

    protected abstract Optional<byte[]> readFile(String path);

    protected abstract List<FileInfo> listFiles(String path);
}
