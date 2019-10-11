package rocks.inspectit.ocelot.file.dirmanagers;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class manages all I/O actions on the working directory.
 */
@Component
public class WorkingDirectoryManager extends DirectoryManager {

    /**
     * Returns all files recursively which are found in the given path.
     *
     * @param path the path of the directory.
     * @return the directory contents.
     * @throws IOException If the input was not valid or something in the filesystem went wrong.
     */
    public synchronized List<String> listFiles(String path) throws IOException {
        return listFiles(path, false);
    }

    /**
     * Returns all files recursively which are found in the given path.
     *
     * @param path               the path of the directory.
     * @param onlyConfigurations if this is true, only the files found in the configuration folder are returned.
     * @return the directory contents.
     * @throws IOException If the input was not valid or something in the filesystem went wrong.
     */
    public synchronized List<String> listFiles(String path, boolean onlyConfigurations) throws IOException {
        Path dir = setDir(path, onlyConfigurations);
        if (path.equals(".git")) {
            return Arrays.asList();
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<String> result = new ArrayList<>();
            for (Path child : files.collect(Collectors.toList())) {
                if (Files.isDirectory(child)) {
                    result.addAll(listFiles(getRelativePath(child, onlyConfigurations), onlyConfigurations));
                } else {
                    String fileName = child.getFileName().toString();
                    if (!fileName.equals(".git")) {
                        if (onlyConfigurations) {
                            if (path.startsWith("files/configurations")) {
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

    /**
     * Returns the relative path of a given path object.
     *
     * @param f             The path one wants to get the relative path of.
     * @param configuration If true, the path will be resolved relative from the configuration folder. Otherwise it will
     *                      be resolved relative from the working directory of the server.
     * @return The relative path as string.
     */
    private String getRelativePath(Path f, boolean configuration) {
        Path rootPath = workingDirRoot;
        if (configuration) {
            rootPath = configurationRoot;
        }
        return rootPath.relativize(f.normalize())
                .toString()
                .replace(f.getFileSystem().getSeparator(), "/");
    }

    /**
     * Returns the path object to a given path string relative to the working directory.
     *
     * @param path          the path as string.
     * @param configuration If true, the path will be resolved relative from the configuration folder. Otherwise it will
     *                      be resolved relative from the working directory of the server.
     * @return A path object which resembles the given path in the chosen directory.
     */
    private Path setDir(String path, boolean configuration) throws AccessDeniedException {
        Path rootPath = workingDirRoot;
        if (configuration) {
            rootPath = configurationRoot;
        }
        if (StringUtils.isEmpty(path)) {
            return rootPath;
        } else {
            assertPathWithinFilesRoot(path);
            return rootPath.resolve(path);
        }
    }

    /**
     * Appends a given path to a fileName.
     * e.g. path = 'test/hello'; name = 'world'. String returned: 'test/hello/world'.
     *
     * @param path     the path one wants to add to a filename.
     * @param fileName the filename one wants to add a path to.
     * @return A String build from the given path and the filename.
     */
    private String appendPathToFileName(String path, String fileName) {
        StringBuilder builder = new StringBuilder();
        if (!path.equals("")) {
            builder.append(path).append("/").append(fileName);
            return builder.toString();
        }
        return fileName;

    }

    /**
     * Reads the given file content into a string.
     * Reads files in the files folder.
     *
     * @param path the path of the file.
     * @return the files content.
     * @throws IOException if the file could not be read.
     */
    public synchronized String readFile(String path) throws IOException {
        assertValidSubPath(path);
        Path file = configurationRoot.resolve(path);
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new AccessDeniedException(path + " is a directory!");
        }
        return new String(Files.readAllBytes(file), ENCODING);
    }

    /**
     * Reads the given AgentMappings content into a string.
     *
     * @return the files content.
     * @throws IOException if the file could not be read.
     */
    public synchronized String readAgentMappingFile() throws IOException {
        String path = AGENT_MAPPINGS_FILE;
        assertValidSubPath(path);
        Path file = workingDirRoot.resolve(path);
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new AccessDeniedException(path + " is a directory!");
        }
        return new String(Files.readAllBytes(file), ENCODING);
    }

    /**
     * Ensures that the given path is a subpath of {@link #configurationRoot}.
     *
     * @param path the pat hto check.
     * @throws AccessDeniedException if the file is not a subpath of the filesRoot.
     */
    private void assertValidSubPath(String path) throws AccessDeniedException {
        assertPathNotEmpty(path);
        assertPathWithinFilesRoot(path);
    }

    /**
     * Ensures that the given path does not point to the filesRoot directory.
     *
     * @param path the path to test.
     * @throws AccessDeniedException thrown if the path points to the filesRoot.
     */
    private void assertPathNotEmpty(String path) throws AccessDeniedException {
        if (StringUtils.isEmpty(path)) {
            throw new AccessDeniedException("/");
        }
        if (configurationRoot.resolve(path).toAbsolutePath().normalize().equals(configurationRoot.toAbsolutePath())) {
            throw new AccessDeniedException("/");
        }
    }

    /**
     * Ensures that the given path does not point to a file outside of the filesRoot directory.
     * This method succeeds if the given path represents the filesRoot.
     *
     * @param path the path to test.
     * @throws AccessDeniedException thrown if the path points to a file outside of the filesRoot.
     */
    private void assertPathWithinFilesRoot(String path) throws AccessDeniedException {
        String subPath = configurationRoot.resolve(path).toAbsolutePath().normalize().toString();
        String rootPath = configurationRoot.toString();
        if (!subPath.startsWith(rootPath)) {
            throw new AccessDeniedException(path);
        }
    }

    /**
     * Creates or replaces the file in the given path with the given content.
     * If required, parent directories are automatically created.
     * Default top level folder is the working directory of the server.
     *
     * @param path    the path of the file.
     * @param content the content of the file.
     * @throws IOException if the file could not be written.
     */
    public synchronized void writeFile(String path, String content) throws IOException {
        writeFiles(path, content, false);
    }

    /**
     * Creates or replaces the AgentMappingsFile with the given content.
     *
     * @param content the content of the file.
     * @throws IOException if the file could not be written.
     */
    public synchronized void writeAgentMappingFile(String content) throws IOException {
        writeFiles(AGENT_MAPPINGS_FILE, content, false);

    }

    /**
     * Creates or replaces the file under the given path with the given content.
     * If required, parent directories are automatically created.
     *
     * @param path               the path of the file.
     * @param content            the content of the file.
     * @param onlyConfigurations if true, the files path is resolved from the configuration folder. Otherwise it is resolved form the
     *                           working directory.
     * @throws IOException if the file could not be written.
     */
    private synchronized void writeFiles(String path, String content, boolean onlyConfigurations) throws IOException {
        Path rootPath = workingDirRoot;
        if (onlyConfigurations) {
            rootPath = configurationRoot;
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
