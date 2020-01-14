package rocks.inspectit.ocelot.file;

import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.file.manager.file.AbstractFileManager;

/**
 * Event fired by the {@link AbstractFileManager} when any file-mutating operation was executed.
 */
public class FileChangedEvent extends ApplicationEvent {

    public FileChangedEvent(Object source) {
        super(source);
    }
}
