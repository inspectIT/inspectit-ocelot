package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.file.FileChangedEvent;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public abstract class AbstractWorkingDirectoryAccessor extends AbstractFileAccessor {

    private ApplicationEventPublisher eventPublisher;

    public AbstractWorkingDirectoryAccessor(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    private void fireFileChangeEvent() {
        FileChangedEvent event = new FileChangedEvent(this);
        eventPublisher.publishEvent(event);
    }

    public boolean writeConfigurationFile(String file, String content) {
        verifyPath(file);
        //TODO sanitize
        //TODO check location, sub path
        //TODO remove leading slash

        try {
            writeFile(CONFIGURATION_FILES_SUBFOLDER + File.separator + file, content);
            fireFileChangeEvent();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteConfigurationFile(String file) {
        verifyPath(file);

        //TODO sanitize
        //TODO check location, sub path
        //TODO remove leading slash
        try {
            deleteFile(CONFIGURATION_FILES_SUBFOLDER + File.separator + file);
            fireFileChangeEvent();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean moveConfigurationFile(String sourcePath, String targetPath) {
        verifyPath(sourcePath);
        verifyPath(targetPath);

        //TODO sanitize
        //TODO check location, sub path
        //TODO remove leading slash

        try {
            String source = CONFIGURATION_FILES_SUBFOLDER + File.separator + sourcePath;
            String target = CONFIGURATION_FILES_SUBFOLDER + File.separator + targetPath;

            moveFile(source, target);
            fireFileChangeEvent();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean writeAgentMappings(String content) {
        try {
            writeFile(AGENT_MAPPINGS_FILE_NAME, content);
            fireFileChangeEvent();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected abstract void writeFile(String path, String content) throws IOException;

    protected abstract void moveFile(String sourcePath, String targetPath) throws IOException;

    protected abstract void deleteFile(String path) throws IOException;
}
