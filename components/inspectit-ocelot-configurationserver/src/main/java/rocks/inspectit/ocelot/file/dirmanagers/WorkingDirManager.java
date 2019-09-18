package rocks.inspectit.ocelot.file.dirmanagers;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.FileChangedEvent;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class WorkingDirManager {

    /**
     * The name of the agent mappings Yaml file used to read and persist mappings.
     */
    private static final String AGENT_MAPPINGS_FILE = "agent_mappings.yaml";

    /**
     * The subfolder within the working directory which acts as
     * filesRoot for the files and directories managed by this class.
     */
    @VisibleForTesting
    static final String FILES_SUBFOLDER = "files/configuration";

    @VisibleForTesting
    static final String AGENT_MAPPING_SUBFOLDER = "files";

    @VisibleForTesting
    static final Charset ENCODING = StandardCharsets.UTF_8;

    @Autowired
    InspectitServerSettings config;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private Path filesRoot;

    private Path agentMappingRoot;

    @PostConstruct
    @VisibleForTesting
    void init() throws IOException {
        filesRoot = Paths.get(config.getWorkingDirectory()).resolve(FILES_SUBFOLDER).toAbsolutePath().normalize();
        Files.createDirectories(filesRoot);
        agentMappingRoot = Paths.get(config.getWorkingDirectory()).resolve(AGENT_MAPPING_SUBFOLDER).toAbsolutePath().normalize();
        Files.createDirectories(agentMappingRoot);
    }

    private void fireFileChangeEvent() {
        eventPublisher.publishEvent(new FileChangedEvent(this));
    }


    /**
     * Returns all files recursively which are found in the given path.
     *
     * @param path the path of the directory
     * @return the directory contents
     * @throws IOException If the input was not valid or something in the filesystem went wrong
     */
    public synchronized List<String> listFiles(String path) throws IOException {
        return listFiles(path, false);
    }


    /**
     * Returns all files recursively which are found in the given path.
     *
     * @param path        the path of the directory
     * @param specialFile if this is true, also the top level folder is searched. Otherwise only the files folder is searched
     * @return the directory contents
     * @throws IOException If the input was not valid or something in the filesystem went wrong
     */
    private synchronized List<String> listFiles(String path, boolean specialFile) throws IOException {
        Path dir = setDir(path, specialFile);
        if (path.equals(".git")) {
            return Arrays.asList();
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<String> result = new ArrayList<>();
            for (Path child : files.collect(Collectors.toList())) {
                if (Files.isDirectory(child)) {
                    result.addAll(listFiles(getRelativePath(child, specialFile), specialFile));
                } else {
                    String fileName = child.getFileName().toString();
                    if (!fileName.equals(".git")) {
                        if (specialFile) {
                            if (!path.startsWith("files")) {
                                result.add(appendPathToFileName(path, fileName));
                            }
                        } else {
                            result.add(appendPathToFileName(path, fileName));
                        }
                    }
                }
            }
            return result;
        }
    }

    private String getRelativePath(Path f, boolean specialDir) {
        Path rootPath = filesRoot;
        if (specialDir) {
            rootPath = agentMappingRoot;
        }
        return rootPath.relativize(f.normalize())
                .toString()
                .replace(f.getFileSystem().getSeparator(), "/");
    }

    private Path setDir(String path, boolean topLevelDir) throws AccessDeniedException {
        Path rootPath = filesRoot;
        if (topLevelDir) {
            rootPath = agentMappingRoot;
        }
        if (StringUtils.isEmpty(path)) {
            return rootPath;
        } else {
            assertPathWithinFilesRoot(path);
            return rootPath.resolve(path);
        }
    }

    private String appendPathToFileName(String path, String fileName) {
        StringBuilder builder = new StringBuilder();
        if (!path.equals("")) {
            builder.append(path).append("/").append(fileName);
            return builder.toString();
        }
        return fileName;

    }

    /**
     * Reads the given file content into a string
     * Reads files in the files folder
     *
     * @param path the path of the file
     * @return the files content
     * @throws IOException if the file could not be read
     */
    public synchronized String readFile(String path) throws IOException {
        assertValidSubPath(path);
        Path file = filesRoot.resolve(path);
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new AccessDeniedException(path + " is a directory!");
        }
        return new String(Files.readAllBytes(file), ENCODING);
    }

    /**
     * Reads the given AgentMappings content into a string
     *
     * @return the files content
     * @throws IOException if the file could not be read
     */
    public synchronized String readAgentMappingFile() throws IOException {
        String path = AGENT_MAPPINGS_FILE;
        assertValidSubPath(path);
        Path file = agentMappingRoot.resolve(path);
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new AccessDeniedException(path + " is a directory!");
        }
        return new String(Files.readAllBytes(file), ENCODING);
    }

    /**
     * Ensures that the given path is a subpath of {@link #filesRoot}
     *
     * @param path the pat hto check
     * @throws AccessDeniedException if the file is not a subpath of the filesRoot
     */
    private void assertValidSubPath(String path) throws AccessDeniedException {
        assertPathNotEmpty(path);
        assertPathWithinFilesRoot(path);
    }

    /**
     * Ensures that the given path does not point to the filesRoot directory.
     *
     * @param path the path to test
     * @throws AccessDeniedException thrown if the path points to the filesRoot
     */
    private void assertPathNotEmpty(String path) throws AccessDeniedException {
        if (StringUtils.isEmpty(path)) {
            throw new AccessDeniedException("/");
        }
        if (filesRoot.resolve(path).toAbsolutePath().normalize().equals(filesRoot.toAbsolutePath())) {
            throw new AccessDeniedException("/");
        }
    }

    /**
     * Ensures that the given path does not point to a file outside of the filesRoot directory.
     * This method succeeds if the given path represents the filesRoot.
     *
     * @param path the path to test
     * @throws AccessDeniedException thrown if the path points to a file outside of the filesRoot
     */
    private void assertPathWithinFilesRoot(String path) throws AccessDeniedException {
        String subPath = filesRoot.resolve(path).toAbsolutePath().normalize().toString();
        String rootPath = filesRoot.toString();
        if (!subPath.startsWith(rootPath)) {
            throw new AccessDeniedException(path);
        }
    }

    /**
     * Creates or replaces the file in the given path with the given content.
     * If required, parent directories are automatically created.
     * Default top level folder is the git/files-folder
     *
     * @param path    the path of the file
     * @param content the content of the file
     * @throws IOException if the file could not be written
     */
    public synchronized void writeFile(String path, String content) throws IOException {
        writeFiles(path, content, false);
    }

    /**
     * Creates or replaces the AgentMappingsFile with the given content.
     *
     * @param content the content of the file
     * @throws IOException if the file could not be written
     */
    public synchronized void writeAgentMappingFile(String content) throws IOException {
        writeFiles(AGENT_MAPPINGS_FILE, content, true);

    }

    /**
     * Creates or replaces the file under the given path with the given content.
     * If required, parent directories are automatically created.
     *
     * @param path       the path of the file
     * @param content    the content of the file
     * @param fromTopDir if true, the files path is resolved from the top level folder. Otherwise it is resolved form the
     *                   files folder
     * @throws IOException if the file could not be written
     */
    private synchronized void writeFiles(String path, String content, boolean fromTopDir) throws IOException {
        Path rootPath = filesRoot;
        if (fromTopDir) {
            rootPath = agentMappingRoot;
        }
        assertValidSubPath(path);
        Path file = rootPath.resolve(path);
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new AccessDeniedException(path + " is a directory!");
        }
        FileUtils.forceMkdir(file.getParent().toFile());
        Files.write(file, content.getBytes(ENCODING));
        fireFileChangeEvent();
    }
}
