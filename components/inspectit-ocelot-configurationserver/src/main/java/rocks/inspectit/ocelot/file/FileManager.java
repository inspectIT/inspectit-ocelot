package rocks.inspectit.ocelot.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.WorkingDirectoryAccessor;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Encapsulates access to the file system storing the source config files managed by this server.
 */
@Component
@Slf4j
public class FileManager {

    private WorkingDirectoryAccessor workingDirectoryAccessor;

    @Autowired
    public FileManager(InspectitServerSettings settings, ApplicationEventPublisher eventPublisher) {
        Path workingDirectory = Paths.get(settings.getWorkingDirectory()).toAbsolutePath().normalize();

        this.workingDirectoryAccessor = new WorkingDirectoryAccessor(workingDirectory, eventPublisher);
    }

    /**
     * Returns the file accessor to access the files in the working directory.
     *
     * @return an instance of {@link WorkingDirectoryAccessor}
     */
    public AbstractWorkingDirectoryAccessor getWorkingDirectory() {
        return workingDirectoryAccessor;
    }
}
