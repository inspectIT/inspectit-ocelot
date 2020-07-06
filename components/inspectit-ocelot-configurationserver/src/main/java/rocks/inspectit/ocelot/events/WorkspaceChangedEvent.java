package rocks.inspectit.ocelot.events;

import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.file.FileManager;

/**
 * Event fired by the {@link FileManager} when the workspace branch has changed.
 */
public class WorkspaceChangedEvent extends ApplicationEvent {

    public WorkspaceChangedEvent(Object source) {
        super(source);
    }
}
