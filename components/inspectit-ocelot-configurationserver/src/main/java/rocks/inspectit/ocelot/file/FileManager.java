package rocks.inspectit.ocelot.file;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates access to the file system storing the source config files managed by this server.
 */
@Component
@Slf4j
public class FileManager {

    /**
     * The subfolder within the working directory which acts as
     * filesRoot for the files and directories managed by this class.
     */
    @VisibleForTesting
    static final String FILES_SUBFOLDER = "files";

    @VisibleForTesting
    static final Charset ENCODING = StandardCharsets.UTF_8;

    @VisibleForTesting
    @Autowired
    InspectitServerSettings config;

    /**
     * The path under which the file system accessible by this component lies.
     * This is the absolute, normalized path represented by {@link InspectitServerSettings#getWorkingDirectory()} with {@link #FILES_SUBFOLDER} appended.
     */
    private Path filesRoot;

    @PostConstruct
    @VisibleForTesting
    void init() throws IOException {
        filesRoot = Paths.get(config.getWorkingDirectory()).resolve(FILES_SUBFOLDER).toAbsolutePath().normalize();
        Files.createDirectories(filesRoot);
    }

    /**
     * Returns all files recursively contained under the given path.
     *
     * @param path      the path of the directory
     * @param recursive if true, the entire file tree within this directory is returned. Otherwise only the direct children.
     * @return the directory contents
     * @throws IOException If the input was not valid or something in the filesystem went wrong
     */
    public synchronized List<FileInfo> getFilesInDirectory(String path, boolean recursive) throws IOException {
        Path dir;
        if (StringUtils.isEmpty(path)) {
            dir = filesRoot;
        } else {
            assertPathWithinFilesRoot(path);
            dir = filesRoot.resolve(path);
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<FileInfo> result = new ArrayList<>();
            for (Path child : files.collect(Collectors.toList())) {
                boolean isDirectory = Files.isDirectory(child);
                FileInfo.FileInfoBuilder builder = FileInfo.builder()
                        .name(child.getFileName().toString())
                        .type(isDirectory ? FileInfo.Type.DIRECTORY : FileInfo.Type.FILE);
                if (isDirectory && recursive) {
                    builder.children(getFilesInDirectory(getRelativePath(child), true));
                }
                result.add(builder.build());
            }
            return result;
        }
    }

    /**
     * @param path the path to check
     * @return true if the given path denotes a directory
     * @throws AccessDeniedException if access is forbidden
     */
    public boolean isDirectory(String path) throws AccessDeniedException {
        assertPathWithinFilesRoot(path);
        return Files.isDirectory(filesRoot.resolve(path));
    }

    /**
     * @param path the path to check
     * @return true if the given path denotes an existing file or directory
     * @throws AccessDeniedException if access is forbidden
     */
    public boolean exists(String path) throws AccessDeniedException {
        assertPathWithinFilesRoot(path);
        return Files.exists(filesRoot.resolve(path));
    }

    /**
     * Creates a new directory with the given path
     *
     * @param path the path of the directory to create
     * @throws IOException if the directory already exists or could not be created for any reason
     */
    public synchronized void createDirectory(String path) throws IOException {
        assertValidSubPath(path);
        Path dir = filesRoot.resolve(path);

        FileUtils.forceMkdir(dir.toFile());
    }

    /**
     * Deletes the given directory including all contents.
     *
     * @param path the path of the directory
     * @throws IOException if the directory could not be deleted
     */
    public synchronized void deleteDirectory(String path) throws IOException {
        assertValidSubPath(path);
        Path dir = filesRoot.resolve(path);
        // throw a more meaningful exception instead of the illegal argument exception thrown by
        // FileUtils.deleteDirectory
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new NotDirectoryException(getRelativePath(dir));
        }
        FileUtils.deleteDirectory(dir.toFile());
    }

    /**
     * Reads the given file content into a string
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
     * Creates or replaces the file under the given path with the given content.
     * If required, parent directories are automatically created.
     *
     * @param path    the path of the file
     * @param content the content of the file
     * @throws IOException if the file could not be written
     */
    public synchronized void createOrReplaceFile(String path, String content) throws IOException {
        assertValidSubPath(path);
        Path file = filesRoot.resolve(path);
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new AccessDeniedException(path + " is a directory!");
        }
        FileUtils.forceMkdir(file.getParent().toFile());
        Files.write(file, content.getBytes(ENCODING));
    }

    /**
     * Deletes the given file. Does not delete directories.
     *
     * @param path the path of the file to delete
     * @throws IOException if the file could not be deleted.
     */
    public synchronized void deleteFile(String path) throws IOException {
        assertValidSubPath(path);
        Path file = filesRoot.resolve(path);
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
        assertValidSubPath(source);
        assertValidSubPath(destination);
        Path src = filesRoot.resolve(source);
        Path dest = filesRoot.resolve(destination);

        FileUtils.forceMkdir(dest.getParent().toFile());

        if (Files.isDirectory(src)) {
            FileUtils.moveDirectory(src.toFile(), dest.toFile());
        } else {
            FileUtils.moveFile(src.toFile(), dest.toFile());
        }
    }

    private String getRelativePath(Path f) {
        return filesRoot.relativize(f.normalize())
                .toString()
                .replace(f.getFileSystem().getSeparator(), "/");
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
}
