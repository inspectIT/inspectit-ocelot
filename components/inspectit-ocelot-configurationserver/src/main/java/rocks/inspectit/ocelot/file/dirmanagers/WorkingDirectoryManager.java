package rocks.inspectit.ocelot.file.dirmanagers;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.file.FileInfo;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class manages all I/O actions on the working directory.
 */
@Component
public class WorkingDirectoryManager extends DirectoryManager {

    /**
     * The subfolder of the working directory in which all files managed by this class are stored.
     */
    private static final String FILES_SUBFOLDER = "files";

    /**
     * The agent mapping subfolder resolved as a path object.
     */
    @VisibleForTesting
    Path workingDirRoot;

    @PostConstruct
    public void init() {
        workingDirRoot = Paths.get(config.getWorkingDirectory()).resolve(FILES_SUBFOLDER).toAbsolutePath().normalize();
    }

    /**
     * Returns path of a given path object relative to the working directory as String.
     *
     * @param f The path one wants to get the relative path of.
     * @return The relative path as string.
     */
    private String getRelativePath(Path f) {
        Path rootPath = workingDirRoot;
        return rootPath.relativize(f.normalize())
                .toString()
                .replace(f.getFileSystem().getSeparator(), "/");
    }

    /**
     * Reads the given file's content as a string.
     * Reads all files in the files folder.
     *
     * @param path the path to the file that should be read
     * @return the files content.
     * @throws IOException if the file could not be read.
     */
    public synchronized String readFile(String path) throws IOException {
        Path file = workingDirRoot.resolve(path);
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new AccessDeniedException(path + " is a directory!");
        }
        return new String(Files.readAllBytes(file), ENCODING);
    }

    /**
     * Creates or replaces the file under the given path with the given content.
     * If required, parent directories are automatically created.
     *
     * @param path    the path of the file.
     * @param content the content of the file.
     *                working directory.
     * @throws IOException if the file could not be written.
     */
    public synchronized void writeFile(String path, String content) throws IOException {
        Path file = workingDirRoot.resolve(path);
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new AccessDeniedException(path + " is a directory!");
        }
        FileUtils.forceMkdir(file.getParent().toFile());
        Files.write(file, content.getBytes(ENCODING));
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
        Path src = workingDirRoot.resolve(source);
        Path dest = workingDirRoot.resolve(destination);

        FileUtils.forceMkdir(dest.getParent().toFile());

        if (Files.isDirectory(src)) {
            FileUtils.moveDirectory(src.toFile(), dest.toFile());
        } else {
            FileUtils.moveFile(src.toFile(), dest.toFile());
        }
    }

    /**
     * Returns all files recursively contained under the given path.
     *
     * @param path      the path of the directory
     * @param recursive if true, the entire file tree within this directory is returned. Otherwise only the direct children.
     * @return the directory contents
     * @throws IOException If the input was not valid or something in the filesystem went wrong
     */
    public synchronized List<FileInfo> listFiles(String path, boolean recursive) throws IOException {
        Path dir;
        if (StringUtils.isEmpty(path)) {
            dir = workingDirRoot;
        } else {
            dir = workingDirRoot.resolve(path);
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<FileInfo> result = new ArrayList<>();
            for (Path child : files.collect(Collectors.toList())) {
                boolean isDirectory = Files.isDirectory(child);
                FileInfo.FileInfoBuilder builder = FileInfo.builder()
                        .name(child.getFileName().toString())
                        .type(isDirectory ? FileInfo.Type.DIRECTORY : FileInfo.Type.FILE);
                if (isDirectory && recursive) {
                    builder.children(listFiles(getRelativePath(child), true));
                }
                result.add(builder.build());
            }
            return result;
        }
    }
}
