package rocks.inspectit.ocelot.file;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AutoCommitWorkingDirectoryProxy;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.WorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.versioning.VersioningManager;
import rocks.inspectit.ocelot.file.versioning.model.ConfigurationPromotion;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Autowired
    public FileManager(InspectitServerSettings settings, ApplicationEventPublisher eventPublisher) throws GitAPIException {
        Path workingDirectory = Paths.get(settings.getWorkingDirectory()).toAbsolutePath().normalize();

        WorkingDirectoryAccessor workingDirectoryAccessorImpl = new WorkingDirectoryAccessor(workingDirectoryLock, workingDirectory, eventPublisher);

        Supplier<Authentication> authenticationSupplier = () -> SecurityContextHolder.getContext().getAuthentication();
        versioningManager = new VersioningManager(workingDirectory, authenticationSupplier, eventPublisher);
        versioningManager.initialize();

        this.workingDirectoryAccessor = new AutoCommitWorkingDirectoryProxy(workingDirectoryLock, workingDirectoryAccessorImpl, versioningManager);
    }

    /**
     * Returns the file accessor to access the files in the working directory.
     *
     * @return an implementation of {@link AbstractWorkingDirectoryAccessor}
     */
    public AbstractWorkingDirectoryAccessor getWorkingDirectory() {
        return workingDirectoryAccessor;
    }

    public AbstractFileAccessor getLiveRevision() {
        return versioningManager.getLiveRevision();
    }

    public WorkspaceDiff getWorkspaceDiff(boolean includeContent) throws IOException, GitAPIException {
        return versioningManager.getWorkspaceDiff(includeContent);
    }

    public void promoteConfiguration(ConfigurationPromotion promotion) throws GitAPIException {
        workingDirectoryLock.writeLock().lock();

        try {
            versioningManager.promoteConfiguration(promotion);
        } finally {
            workingDirectoryLock.writeLock().unlock();
        }
    }
}
