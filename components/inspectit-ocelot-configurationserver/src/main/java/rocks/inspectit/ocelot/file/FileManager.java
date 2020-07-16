package rocks.inspectit.ocelot.file;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.accessor.git.CachingRevisionAccess;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AutoCommitWorkingDirectoryProxy;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.WorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.versioning.VersioningManager;
import rocks.inspectit.ocelot.file.versioning.model.ConfigurationPromotion;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Encapsulates access to the file system storing the source config files managed by this server.
 */
@Component
@Slf4j
public class FileManager {

    private final ReadWriteLock workingDirectoryLock = new ReentrantReadWriteLock();

    /**
     * The accessor used to access the working directory.
     */
    private AbstractWorkingDirectoryAccessor workingDirectoryAccessor;

    /**
     * The manager used for Git interactions.
     */
    private VersioningManager versioningManager;

    /**
     * Caches the current live-revision to make sure the same instance is returned if no new changes were committed.
     */
    private CachingRevisionAccess cachedLiveRevision;

    /**
     * Caches the current workspace-revision to make sure the same instance is returned if no new changes were committed.
     */
    private CachingRevisionAccess cachedWorkspaceRevision;

    @Autowired
    public FileManager(InspectitServerSettings settings, ApplicationEventPublisher eventPublisher, Executor executor) throws GitAPIException {
        Path workingDirectory = Paths.get(settings.getWorkingDirectory()).toAbsolutePath().normalize();

        // We use an asynchronous event publishing mechanism to make sure that Event-Listeners do not accidentally get hold of locks
        // which are owned by the thread which fires the event.
        ApplicationEventPublisher asyncPublisher = (event) -> executor.execute(() -> eventPublisher.publishEvent(event));

        WorkingDirectoryAccessor workingDirectoryAccessorImpl = new WorkingDirectoryAccessor(workingDirectoryLock.readLock(), workingDirectoryLock
                .writeLock(), workingDirectory);

        Supplier<Authentication> authenticationSupplier = () -> SecurityContextHolder.getContext().getAuthentication();
        versioningManager = new VersioningManager(workingDirectory, authenticationSupplier, asyncPublisher);
        versioningManager.initialize();

        workingDirectoryAccessor = new AutoCommitWorkingDirectoryProxy(workingDirectoryLock.writeLock(), workingDirectoryAccessorImpl, versioningManager);
    }

    /**
     * Returns the file accessor to access the files in the working directory.
     *
     * @return an implementation of {@link AbstractWorkingDirectoryAccessor}
     */
    public AbstractWorkingDirectoryAccessor getWorkingDirectory() {
        return workingDirectoryAccessor;
    }

    /**
     * Returns access to the latest commit of the live branch.
     *
     * @return accessor to access the current live branch
     */
    public RevisionAccess getLiveRevision() {
        CachingRevisionAccess currentRev = versioningManager.getLiveRevision();
        if (cachedLiveRevision == null || !currentRev.getRevisionId().equals(cachedLiveRevision.getRevisionId())) {
            cachedLiveRevision = currentRev;
        }
        return cachedLiveRevision;
    }

    /**
     * Returns a versioned view on the current state of the working directory.
     * When no writes happen, the file contents will be the same as for {@link #getWorkingDirectory()}.
     * However, the instance returned by this method is immutable.
     * The returned revision will not be affected by subsequent writes to the working directory.
     *
     * @return accessor to access the current workspace branch
     */
    public RevisionAccess getWorkspaceRevision() {
        CachingRevisionAccess currentRev = versioningManager.getWorkspaceRevision();
        if (cachedWorkspaceRevision == null
                || !currentRev.getRevisionId().equals(cachedWorkspaceRevision.getRevisionId())) {
            cachedWorkspaceRevision = currentRev;
        }
        return cachedWorkspaceRevision;
    }

    /**
     * Returns the diff between the current live branch and the current workspace branch.
     *
     * @param includeContent whether the file difference (old and new content) is included
     *
     * @return the diff between the live and workspace branch
     */
    public WorkspaceDiff getWorkspaceDiff(boolean includeContent) throws IOException, GitAPIException {
        return versioningManager.getWorkspaceDiff(includeContent);
    }

    /**
     * Executes a file promotion according to the specified {@link ConfigurationPromotion} definition.
     *
     * @param promotion          the definition what to promote
     * @param allowSelfPromotion if true, the current user will be allowed to promote his own changes.
     */
    public void promoteConfiguration(ConfigurationPromotion promotion, boolean allowSelfPromotion) throws GitAPIException {
        workingDirectoryLock.writeLock().lock();
        try {
            versioningManager.promoteConfiguration(promotion, allowSelfPromotion);
        } finally {
            workingDirectoryLock.writeLock().unlock();
        }
    }
}
