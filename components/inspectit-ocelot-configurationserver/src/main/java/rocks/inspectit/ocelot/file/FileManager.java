package rocks.inspectit.ocelot.file;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.dirmanagers.GitDirManager;
import rocks.inspectit.ocelot.file.dirmanagers.WorkingDirManager;

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
    static final String FILES_SUBFOLDER = "files/configuration";

    @VisibleForTesting
    static final Charset ENCODING = StandardCharsets.UTF_8;

    @VisibleForTesting
    @Autowired
    InspectitServerSettings config;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private GitDirManager gitDirManager;

    @Autowired
    private WorkingDirManager workingDirManager;

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
     * Returns all file names found in the files folder, either from the last commit or the working directory
     *
     * @param fromWorkingDir if true, the entire file tree within this directory is returned. Otherwise the last committed
     *                       file tree is returned
     * @return the given file names as a List of type String
     */
    public synchronized List<String> listFiles(boolean fromWorkingDir) {
        if (fromWorkingDir) {
            try {
                return workingDirManager.listFiles("");
            } catch (IOException e) {
                log.error("An error occurred while listing files in working directory");
                return null;
            }
        } else {
            try {
                return gitDirManager.listFiles();
            } catch (IOException e) {
                log.error("An error occurred while listing files from git directory");
                return null;
            }
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
        fireFileChangeEvent();
        commitAllChanges();
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
        fireFileChangeEvent();
    }

    /**
     * Reads the given file's content as it is found in the working directory as string
     *
     * @param filePath the path of the file
     * @return the files content
     * @throws IOException if the file could not be read
     */
    public synchronized String readFile(String filePath) {
        return readFile(filePath, false);
    }

    /**
     * Reads the given file's content as string. Can either read the file as it is currently found in the working directory
     * or last committed version of the file.
     *
     * @param fromWorkingDir if true, the file as it is currently stored in the working directory is returned. Otherwise,
     *                       the file as it was last committed is returned.
     * @param filePath       the path of the file
     * @return the files content
     * @throws IOException if the file could not be read
     */
    public synchronized String readFile(String filePath, boolean fromWorkingDir) {
        if (fromWorkingDir) {
            try {
                return workingDirManager.readFile(filePath);
            } catch (IOException e) {
                log.error("Could not read file: {}", filePath);
                return null;
            }
        } else {
            try {
                return gitDirManager.readFile(filePath);
            } catch (IOException e) {
                log.error("Could not read file: {}", filePath);
                return null;
            }
        }
    }


    public synchronized String readAgentMapping() {
        return readAgentMapping(false);
    }

    /**
     * Reads the agent
     *
     * @param fromWorkingDir if true, the file as it is currently stored in the working directory is returned. Otherwise,
     *                       the file as it was last committed is returned.
     * @return the files content
     * @throws IOException if the file could not be read
     */
    public synchronized String readAgentMapping(boolean fromWorkingDir) {
        if (fromWorkingDir) {
            try {
                return workingDirManager.readAgentMappingFile();
            } catch (IOException e) {
                log.error("Could not read AgentMapping from disk");
                return null;
            }
        } else {
            try {
                return gitDirManager.readAgentMappingFile();
            } catch (IOException e) {
                log.error("Could not read AgentMapping from last git commit");
                return null;
            }
        }
    }

    /**
     * Creates or replaces the file under the given path with the given content.
     * If required, parent directories are automatically created.
     *
     * @param path    the path of the file
     * @param content the content of the file
     * @throws IOException if the file could not be written
     */
    public synchronized void createOrReplaceFile(String path, String content) throws IOException, GitAPIException {
        assertValidSubPath(path);
        Path file = filesRoot.resolve(path);
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new AccessDeniedException(path + " is a directory!");
        }
        FileUtils.forceMkdir(file.getParent().toFile());
        Files.write(file, content.getBytes(ENCODING));
        fireFileChangeEvent();
        commitAllChanges();
    }

    /**
     * Creates or replaces the file under the given path with the given content.
     * If required, parent directories are automatically created.
     *
     * @param path    the path of the file
     * @param content the content of the file
     * @throws IOException if the file could not be written
     */
    public synchronized void writeFile(String path, String content) throws IOException, GitAPIException {
        workingDirManager.writeFile(path, content);
        commitAllChanges();
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
            fireFileChangeEvent();
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
    public synchronized void move(String source, String destination) throws IOException, GitAPIException {
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
        fireFileChangeEvent();
        commitAllChanges();
    }

    private void fireFileChangeEvent() {
        eventPublisher.publishEvent(new FileChangedEvent(this));
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

    /**
     * Commits all current changes to the local git repo.
     *
     * @return Returns true if the commit was successful, returns false if any errors occurred during the process
     */
    public boolean commitAllChanges() {
        try {
            return gitDirManager.commitAllChanges();
        } catch (GitAPIException e) {
            e.printStackTrace();
            log.error("An error occurred while committing files to git directory");
        }
        return false;
    }


}
