package rocks.inspectit.ocelot.file;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
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
@Component
@Slf4j
public class FileManager {

    /**
     * The subfolder within the working directory which acts as
     * filesRoot for the files and directories managed by this class.
     */
    @VisibleForTesting
    static final String FILES_SUBFOLDER = "files";

    /**
     * The subfolder within the working directory in which all configuration files are stored.
     */
    @VisibleForTesting
    static final String CONFIG_SUBFOLDER = "configuration";

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
     * Lists all files found in the configuration folder.
     *
     * @param path       the path to the subfolder of which the files should be listed of. Use "" to list all files in
     *                   configuration
     * @param recursive  if true, all subfolders of the given path are searched.
     * @param versioning if true, the directories of the last committed version are searched.
     *                   if false, the directories as they are on the drive are searched.
     * @return A list of files found in the given path.
     */
    public List<FileInfo> listConfigurationFiles(String path, boolean recursive, boolean versioning) throws IOException {
        if (versioning) {
            return gitDirectoryManager.listFiles(setToPathInConfig(path), recursive);
        } else {
            return workingDirectoryManager.listFiles(setToPathInConfig(path), recursive);
        }
    }

    /**
     * @param path       the path to the subfolder of which the files should be listed of. Use "" to list all files in
     *                   files
     * @param recursive  if true, all subfolders of the given path are searched.
     * @param versioning if true, the directories of the last committed version are searched.
     *                   if false, the directories as they are on the drive are searched.
     * @return A list of files found in the given path.
     */
    public List<FileInfo> listSpecialFiles(String path, boolean recursive, boolean versioning) throws IOException {
        if (versioning) {
            return gitDirectoryManager.listFiles(path, recursive);
        } else {
            return workingDirectoryManager.listFiles(path, recursive);
        }
    }

    /**
     * @param path the path to check
     * @return true if the given path denotes a directory
     * @throws AccessDeniedException if access is forbidden
     */
    public boolean isDirectory(String path) throws AccessDeniedException {
        return workingDirectoryManager.isDirectory(path);
    }

    /**
     * @param path the path to check
     * @return true if the given path denotes an existing file or directory
     * @throws AccessDeniedException if access is forbidden
     */
    public boolean exists(String path) throws AccessDeniedException {
        return workingDirectoryManager.exists(path);
    }

    /**
     * Creates a new directory with the given path
     *
     * @param path the path of the directory to create
     * @throws IOException if the directory already exists or could not be created for any reason
     */
    public synchronized void createDirectory(String path) throws IOException, GitAPIException {
        workingDirectoryManager.createDirectory(path);
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
        workingDirectoryManager.deleteDirectory(path);
        commitAllChanges();
        fireFileChangeEvent();
    }

    /**
     * Reads the given file's content as a string.
     * Only Reads files in sub directories of or directly from /configuration
     *
     * @param path the path of the file.
     * @return the files content.
     * @throws IOException if the file could not be read.
     */
    public String readConfigurationFile(String path, boolean versioning) throws AccessDeniedException {
        if (!versioning) {
            try {
                return workingDirectoryManager.readFile(setToPathInConfig(path));
            } catch (IOException e) {
                log.error("Could not read file: {}", path);
                return null;
            }
        } else {
            try {
                return gitDirectoryManager.readFile(setToPathInConfig(path));
            } catch (IOException e) {
                log.error("Could not read file: {}", path);
                return null;
            }
        }
    }

    /**
     * Reads the given file's content as a string.
     * Reads all files in the files folder.
     *
     * @param path the path to the file that should be read
     * @return the files content.
     * @throws IOException if the file could not be read.
     */
    public String readSpecialFile(String path, boolean versioning) throws AccessDeniedException {
        if (!versioning) {
            try {
                return workingDirectoryManager.readFile(path);
            } catch (IOException e) {
                log.error("Could not read file: {}", path);
                return null;
            }
        } else {
            try {
                return gitDirectoryManager.readFile(path);
            } catch (IOException e) {
                log.error("Could not read file: {}", path);
                return null;
            }
        }
    }

    /**
     * Creates or replaces the file under the given path with the given content.
     * If required, parent directories are automatically created.
     * Writes all files in the 'configuration' folder.
     *
     * @param path    the path of the file
     * @param content the content of the file
     * @throws IOException if the file could not be written
     */
    public synchronized void writeConfigurationFile(String path, String content) throws IOException, GitAPIException {
        workingDirectoryManager.writeFile(setToPathInConfig(path), content);
        commitAllChanges();
        fireFileChangeEvent();
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
    public synchronized void writeSpecialFile(String path, String content) throws IOException, GitAPIException {
        workingDirectoryManager.writeFile(path, content);
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
        workingDirectoryManager.deleteFile(path);
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
        workingDirectoryManager.move(source, destination);
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
     * Commits all current changes to the local git repo.
     *
     * @return Returns true if the commit was successful, returns false if any errors occurred during the process
     */
    public void commitAllChanges() throws GitAPIException {
        gitDirectoryManager.commitAllChanges();
    }

    /**
     * Creates a new String with the configuration path in front of a given path and returns it.
     *
     * @param path the path which should be added.
     * @return The path with the configuration path added.
     */
    private String setToPathInConfig(String path) {
        StringBuilder builder = new StringBuilder();
        builder.append(CONFIG_SUBFOLDER).append("/").append(path);
        return builder.toString();
    }
}
