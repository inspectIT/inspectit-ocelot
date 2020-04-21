package rocks.inspectit.ocelot.file.accessor;

import rocks.inspectit.ocelot.file.FileInfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public abstract class AbstractFileAccessor {

    public static final Charset FILE_ENCODING = StandardCharsets.UTF_8;

    public static final String AGENT_MAPPINGS_FILE_NAME = "agent_mappings.yaml";

    public static final String CONFIGURATION_FILES_SUBFOLDER = "files";

    public Optional<String> readConfigurationFile(String file) {
        verifyPath(file);
        //TODO sanitize
        //TODO check location, sub path
        //TODO remove leading slash
        Optional<byte[]> fileContent = readFile(CONFIGURATION_FILES_SUBFOLDER + File.separator + file);

        return fileContent.map(String::new);
    }

    public Optional<List<FileInfo>> listConfigurationFiles(String path) {
        verifyPath(path);
        //TODO sanitize
        //TODO check location, sub path
        //TODO remove leading slash
        List<FileInfo> files = listFiles(CONFIGURATION_FILES_SUBFOLDER + File.separator + path);

        return Optional.ofNullable(files);
    }

    public Optional<String> readAgentMappings() {
        Optional<byte[]> fileContent = readFile(AGENT_MAPPINGS_FILE_NAME);

        return fileContent.map(bytes -> new String(bytes, FILE_ENCODING));
    }

    protected void verifyPath(String path) {
        failIfDirectoryTraversal(path);
    }

    public void failIfDirectoryTraversal(String relativePath) {
        File file = new File(relativePath);

        if (file.isAbsolute()) {
            throw new DirectoryTraversalException("Directory traversal attempt - absolute path not allowed.");
        }

        String pathUsingCanonical;
        String pathUsingAbsolute;
        try {
            pathUsingCanonical = file.getCanonicalPath();
            pathUsingAbsolute = file.getAbsolutePath();
        } catch (IOException e) {
            throw new DirectoryTraversalException("Directory traversal is prohibited.", e);
        }

        // Require the absolute path and canonicalized path match.
        // This is done to avoid directory traversal
        // attacks, e.g. "1/../2/"
        if (!pathUsingCanonical.equals(pathUsingAbsolute)) {
            throw new DirectoryTraversalException("Directory traversal is prohibited.");
        }
    }

    protected abstract Optional<byte[]> readFile(String path);

    protected abstract List<FileInfo> listFiles(String path);
}
