package rocks.inspectit.ocelot.file.manager;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.FileChangedEvent;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.dirmanagers.GitDirectoryManager;
import rocks.inspectit.ocelot.file.dirmanagers.WorkingDirectoryManager;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Encapsulates access to the file system storing the source config files managed by this server.
 */
@Slf4j
public abstract class AbstractFileManager {

    /**
     * The subfolder within the working directory which acts as
     * filesRoot for the files and directories managed by this class.
     */
    public static final String FILES_DIRECTORY = "files";

    /**
     * The subfolder within the working directory in which all configuration files are stored.
     */
    public static final String CONFIG_DIRECTORY = "configuration";

    @VisibleForTesting
    static final Charset ENCODING = StandardCharsets.UTF_8;

    @VisibleForTesting
    @Autowired
    InspectitServerSettings config;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private GitDirectoryManager gitDirectoryManager;

    @Autowired
    private WorkingDirectoryManager workingDirectoryManager;

    protected abstract String resolvePath(String path);

    @PostConstruct
    @VisibleForTesting
    void init() throws IOException {
        Path rootPath = Paths.get(config.getWorkingDirectory()).resolve(resolvePath(".")).toAbsolutePath().normalize();
        Files.createDirectories(rootPath);
    }

    /**
     * Lists all files found in the configuration folder.
     *
     * @param path       the path to the subfolder of which the files should be listed of. Use "" to list all files in
     *                   files
     * @param recursive  if true, all subfolders of the given path are searched.
     * @param versioning if true, the directories of the last committed version are searched.
     *                   if false, the directories as they are on the drive are searched.
     * @return A list of files found in the given path.
     */
    public List<FileInfo> listFiles(String path, boolean recursive, boolean versioning) throws IOException {
        if (versioning) {
            return gitDirectoryManager.listFiles(path, recursive);
        } else {
            String resolvedPath = resolvePath(path);
            return workingDirectoryManager.listFiles(resolvedPath, recursive);
        }
    }

    /**
     * @param path the path to check
     * @return true if the given path denotes a directory
     * @throws AccessDeniedException if access is forbidden
     */
    public boolean isDirectory(String path) throws AccessDeniedException {
        return workingDirectoryManager.isDirectory(resolvePath(path));
    }

    /**
     * @param path the path to check
     * @return true if the given path denotes an existing file or directory
     * @throws AccessDeniedException if access is forbidden
     */
    public boolean exists(String path) throws AccessDeniedException {
        return workingDirectoryManager.exists(resolvePath(path));
    }

    /**
     * Creates a new directory with the given path
     *
     * @param path the path of the directory to create
     * @throws IOException if the directory already exists or could not be created for any reason
     */
    public synchronized void createDirectory(String path) throws IOException, GitAPIException {
        workingDirectoryManager.createDirectory(resolvePath(path));
        commitAllChanges();
        fireFileChangeEvent();
    }

    /**
     * Deletes the given directory including all contents.
     *
     * @param path the path of the directory
     * @throws IOException if the directory could not be deleted
     */
    public synchronized void deleteDirectory(String path) throws IOException, GitAPIException {
        workingDirectoryManager.deleteDirectory(resolvePath(path));
        commitAllChanges();
        fireFileChangeEvent();
    }

    /**
     * Reads the given file's content as a string.
     *
     * @param path the path of the file.
     * @return the files content.
     */
    public String readFile(String path, boolean versioning) {
        try {
            if (!versioning) {
                String resolvedPath = resolvePath(path);
                return workingDirectoryManager.readFile(resolvedPath);
            } else {
                return gitDirectoryManager.readFile(path);
            }
        } catch (IOException e) {
            log.error("Could not read file: {}", path);
            return null;
        }
    }

    /**
     * Creates or replaces the file under the given path with the given content.
     * If required, parent directories are automatically created.
     * Writes all files in the 'files' folder
     *
     * @param path    the path of the file
     * @param content the content of the file
     * @throws IOException if the file could not be written
     */
    public synchronized void writeFile(String path, String content) throws IOException, GitAPIException {
        workingDirectoryManager.writeFile(resolvePath(path), content);
        commitAllChanges();
        fireFileChangeEvent();
    }

    /**
     * Deletes the given file. Does not delete directories.
     *
     * @param path the path of the file to delete
     * @throws IOException if the file could not be deleted.
     */
    public synchronized void deleteFile(String path) throws IOException, GitAPIException {
        workingDirectoryManager.deleteFile(resolvePath(path));
        fireFileChangeEvent();
        commitAllChanges();
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
        workingDirectoryManager.move(resolvePath(source), resolvePath(destination));
        commitAllChanges();
    }

    private void fireFileChangeEvent() {
        eventPublisher.publishEvent(new FileChangedEvent(this));
    }

    /**
     * Commits all current changes to the local git repo.
     *
     * @return Returns true if the commit was successful, returns false if any errors occurred during the process
     */
    public void commitAllChanges() throws GitAPIException {
        gitDirectoryManager.commitAllChanges();
    }
}
