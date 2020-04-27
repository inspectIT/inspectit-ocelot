package rocks.inspectit.ocelot.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.WorkingDirectoryAccessor;

/**
 * Encapsulates access to the file system storing the source config files managed by this server.
 */
@Component
@Slf4j
public class FileManager {

    private WorkingDirectoryAccessor workingDirectoryAccessor;

    @Autowired
    public FileManager(WorkingDirectoryAccessor workingDirectoryAccessor) {
        this.workingDirectoryAccessor = workingDirectoryAccessor;
    }

    public AbstractWorkingDirectoryAccessor getWorkingDirectory() {
        return workingDirectoryAccessor;
    }
}
