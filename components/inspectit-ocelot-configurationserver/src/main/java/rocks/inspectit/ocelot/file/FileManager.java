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
import rocks.inspectit.ocelot.file.versioning.ConfigurationPromotion;
import rocks.inspectit.ocelot.file.versioning.VersioningManager;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Encapsulates access to the file system storing the source config files managed by this server.
 */
@Component
@Slf4j
public class FileManager {

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

        WorkingDirectoryAccessor workingDirectoryAccessorImpl = new WorkingDirectoryAccessor(workingDirectory, eventPublisher);

        Supplier<Authentication> authenticationSupplier = () -> SecurityContextHolder.getContext().getAuthentication();
        versioningManager = new VersioningManager(workingDirectory, authenticationSupplier, eventPublisher);
        versioningManager.initialize();

        this.workingDirectoryAccessor = new AutoCommitWorkingDirectoryProxy(workingDirectoryAccessorImpl, versioningManager);
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

    public WorkspaceDiff getWorkspaceDiff(boolean includeContent) {
        try {
            return versioningManager.getWorkspaceDiff(includeContent);
        } catch (IOException | GitAPIException e) {
            log.error("error", e);
            return null;
        }
    }

    public void promoteConfiguration(ConfigurationPromotion promotion){
        try {
            versioningManager.promoteConfiguration(promotion);
        } catch (GitAPIException | IOException e) {
            log.error("error", e);
            throw new RuntimeException();
        }
    }
}
