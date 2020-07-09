package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.versioning.VersioningManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Delegation proxy for the {@link WorkingDirectoryAccessor} which directly stages and commits changes.
 */
@Slf4j
public class AutoCommitWorkingDirectoryProxy extends AbstractWorkingDirectoryAccessor {

    /**
     * Lock which is used when writing to the working directory.
     */
    private Lock writeLock;

    /**
     * The accessor which actually does the working directory access.
     */
    private WorkingDirectoryAccessor workingDirectoryAccessor;

    /**
     * The version manager to use.
     */
    private VersioningManager versioningManager;

    public AutoCommitWorkingDirectoryProxy(Lock writeLock, WorkingDirectoryAccessor workingDirectoryAccessor, VersioningManager versioningManager) {
        this.writeLock = writeLock;
        this.workingDirectoryAccessor = workingDirectoryAccessor;
        this.versioningManager = versioningManager;
    }

    /**
     * Stages and commits the current working directory files.
     */
    private void commit() {
        try {
            versioningManager.commitAllChanges("Commit configuration file and agent mapping changes");
        } catch (GitAPIException e) {
            log.error("File modification was successful but staging and committing of the change failed!", e);
        }
    }

    /**
     * Brings the working directory into a clean state. Currently, existing changes will be committed and marked
     * as external changes.
     */
    private void clean() {
        try {
            versioningManager.commitAsExternalChange();
        } catch (GitAPIException e) {
            log.error("Could not clean the working directory.");
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void writeFile(String path, String content) throws IOException {
        writeLock.lock();
        try {
            clean();
            workingDirectoryAccessor.writeFile(path, content);
            commit();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    protected void createDirectory(String path) throws IOException {
        writeLock.lock();
        try {
            clean();
            workingDirectoryAccessor.createDirectory(path);
            commit();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    protected void move(String sourcePath, String targetPath) throws IOException {
        writeLock.lock();
        try {
            clean();
            workingDirectoryAccessor.move(sourcePath, targetPath);
            commit();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    protected void delete(String path) throws IOException {
        writeLock.lock();
        try {
            clean();
            workingDirectoryAccessor.delete(path);
            commit();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    protected String verifyPath(String relativeBasePath, String path) {
        return workingDirectoryAccessor.verifyPath(relativeBasePath, path);
    }

    @Override
    protected byte[] readFile(String path) throws IOException {
        return workingDirectoryAccessor.readFile(path);
    }

    @Override
    protected List<FileInfo> listFiles(String path) {
        return workingDirectoryAccessor.listFiles(path);
    }

    @Override
    protected boolean exists(String path) {
        return workingDirectoryAccessor.exists(path);
    }

    @Override
    protected boolean isDirectory(String path) {
        return workingDirectoryAccessor.isDirectory(path);
    }
}
