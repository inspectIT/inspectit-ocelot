package rocks.inspectit.ocelot.file;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FileManager {

    @VisibleForTesting
    static final Charset ENCODING = StandardCharsets.UTF_8;

    @Value("${inspectit.workingDirectory:workdir}")
    @VisibleForTesting
    String workingDir;

    /**
     * The path under which the file system accessible by this component lies.
     * This is the absolute, normalized path represented by {@link #workingDir}.
     */
    private Path root;

    @PostConstruct
    @VisibleForTesting
    void init() throws IOException {
        root = Paths.get(workingDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /**
     * Returns all files recursively contained under the given path.
     *
     * @param path the path of the directory
     * @return the directory contents
     * @throws IOException If the input was not valid or something in the fileystem went wrong
     */
    public synchronized Collection<FileInfo> getFilesInDirectory(String path) throws IOException {
        Path dir;
        if (StringUtils.isEmpty(path)) {
            dir = root;
        } else {
            assertPathWithinWorkDir(path);
            dir = root.resolve(path);
        }
        return Files.walk(dir)
                .filter(f -> f != dir)
                .map((f) ->
                        FileInfo.builder()
                                .path(getRelativePath(f))
                                .type(Files.isDirectory(f) ? FileInfo.Type.DIRECTORY : FileInfo.Type.FILE)
                                .build()
                ).collect(Collectors.toSet());

    }

    /**
     * Creates a new directory with the given path
     *
     * @param path the path of the directory to create
     * @throws IOException if the directory already exists or could not be created for any reason
     */
    public synchronized void createNewDirectory(String path) throws IOException {
        assertPathNotEmpty(path);
        assertPathWithinWorkDir(path);
        Path dir = root.resolve(path);

        Path curr = dir;
        while (curr != null) {
            if (Files.exists(curr) && !Files.isDirectory(curr)) {
                throw new AccessDeniedException(curr + " is a file!");
            }
            curr = curr.getParent();
        }

        Files.createDirectories(dir.getParent());
        Files.createDirectory(dir);
    }

    /**
     * Deletes the given directory including all contents.
     *
     * @param path the path of the directory
     * @throws IOException if the directory could not be deleted
     */
    public synchronized void deleteDirectory(String path) throws IOException {
        assertPathNotEmpty(path);
        assertPathWithinWorkDir(path);
        Path dir = root.resolve(path);
        if (Files.isDirectory(dir)) {
            List<Path> files = getContentsDepthFirst(dir);
            for (Path f : files) {
                Files.delete(f);
            }
        } else {
            throw new NotDirectoryException(path);
        }
    }

    /**
     * Reads the given file content into a string
     *
     * @param path the path of the file
     * @return the files content
     * @throws IOException if the file could not be read
     */
    public synchronized String readFile(String path) throws IOException {
        assertPathNotEmpty(path);
        assertPathWithinWorkDir(path);
        Path file = root.resolve(path);
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new AccessDeniedException(path + " is a directory!");
        }
        return new String(Files.readAllBytes(file), ENCODING);
    }

    /**
     * Creates or replaces the file under the given path with the given content.
     * If required, parent directories are automatically created.
     *
     * @param path    the path of the file
     * @param content the content of the file
     * @throws IOException if the file could not be written
     */
    public synchronized void createOrReplaceFile(String path, String content) throws IOException {
        assertPathNotEmpty(path);
        assertPathWithinWorkDir(path);
        Path file = root.resolve(path);
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new AccessDeniedException(path + " is a directory!");
        }
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(ENCODING));
    }

    /**
     * Deletes the given file. Does not delete directories.
     *
     * @param path the path of the file to delete
     * @throws IOException if the file could not be deleted.
     */
    public synchronized void deleteFile(String path) throws IOException {
        assertPathNotEmpty(path);
        assertPathWithinWorkDir(path);
        Path file = root.resolve(path);
        if (Files.isRegularFile(file)) {
            Files.delete(file);
        } else {
            throw new AccessDeniedException(path);
        }
    }

    /**
     * Moves and / or renames the given file or directories.
     * Directories are moved including their contents.
     *
     * @param source      the source file or directory path
     * @param destination the target file or directory path
     * @throws IOException if the given file or directory could not be renamed / moved
     */
    public synchronized void move(String source, String destination) throws IOException {
        assertPathNotEmpty(source);
        assertPathNotEmpty(destination);
        assertPathWithinWorkDir(source);
        assertPathWithinWorkDir(destination);
        Path src = root.resolve(source);
        Path dest = root.resolve(destination);
        Files.createDirectories(dest.getParent());
        Files.move(src, dest);
    }

    private List<Path> getContentsDepthFirst(Path dir) throws IOException {
        List<Path> files = Files.walk(dir).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(files);
        return files;
    }

    private String getRelativePath(Path f) {
        return root.relativize(f.normalize())
                .toString()
                .replace(f.getFileSystem().getSeparator(), "/");
    }

    /**
     * Ensures that the given path does not point to the root directory.
     *
     * @param path the path to test
     * @throws AccessDeniedException thrown if the path points to the root
     */
    private void assertPathNotEmpty(String path) throws AccessDeniedException {
        if (StringUtils.isEmpty(path)) {
            throw new AccessDeniedException("/");
        }
        if (root.resolve(path).toAbsolutePath().normalize().equals(root.toAbsolutePath())) {
            throw new AccessDeniedException("/");
        }
    }

    /**
     * Ensures that the given path does not point to a file outside of the root directory.
     *
     * @param path the path to test
     * @throws AccessDeniedException thrown if the path points to a file outside of the root
     */
    private void assertPathWithinWorkDir(String path) throws IOException {
        String subPath = root.resolve(path).toAbsolutePath().normalize().toString();
        String rootPath = root.toString();
        if (!subPath.startsWith(rootPath)) {
            throw new AccessDeniedException(path);
        }
    }
}
