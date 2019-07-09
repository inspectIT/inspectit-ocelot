package rocks.inspectit.ocelot.file;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired by the {@link FileManager} when any file-mutating operation was executed.
 */
public class FileChangedEvent extends ApplicationEvent {

    public FileChangedEvent(Object source) {
        super(source);
    }
}
