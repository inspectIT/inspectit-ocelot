package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.versioning.VersioningManager;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Delegation proxy for the {@link WorkingDirectoryAccessor} which directly stages and commits changes.
 */
@Slf4j
public class AutoCommitWorkingDirectoryProxy extends AbstractWorkingDirectoryAccessor {

    private WorkingDirectoryAccessor workingDirectoryAccessor;

    private VersioningManager versioningManager;

    public AutoCommitWorkingDirectoryProxy(WorkingDirectoryAccessor workingDirectoryAccessor, VersioningManager versioningManager) {
        this.workingDirectoryAccessor = workingDirectoryAccessor;
        this.versioningManager = versioningManager;
    }

    /**
     * Stages and commits the current working directory files.
     */
    private void commit() {
        try {
            versioningManager.stageAndCommit();
        } catch (GitAPIException e) {
            log.error("File modification was successful but staging and committing of the change failed!", e);
        }
    }

    private void clean() {
        try {
            versioningManager.resetConfigurationFiles();
        } catch (GitAPIException e) {
            //TODO
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void writeFile(String path, String content) throws IOException {
        clean();
        workingDirectoryAccessor.writeFile(path, content);
        commit();
    }

    @Override
    protected void createDirectory(String path) throws IOException {
        clean();
        workingDirectoryAccessor.createDirectory(path);
        commit();
    }

    @Override
    protected void move(String sourcePath, String targetPath) throws IOException {
        clean();
        workingDirectoryAccessor.move(sourcePath, targetPath);
        commit();
    }

    @Override
    protected void delete(String path) throws IOException {
        clean();
        workingDirectoryAccessor.delete(path);
        commit();
    }

    @Override
    protected String verifyPath(String relativeBasePath, String path) {
        return workingDirectoryAccessor.verifyPath(relativeBasePath, path);
    }

    @Override
    protected Optional<byte[]> readFile(String path) {
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
