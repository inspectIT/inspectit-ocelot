package rocks.inspectit.ocelot.events;

import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;

/**
 * Event fired by the {@link FileManager} when the workspace branch has changed.
 */
public class WorkspaceChangedEvent extends ApplicationEvent {

    private final RevisionAccess workspaceRevision;

    public WorkspaceChangedEvent(Object source, RevisionAccess workspaceRevision) {
        super(source);
        this.workspaceRevision = workspaceRevision;
    }

    /**
     * @return the new workspace revision after the update.
     */
    public RevisionAccess getWorkspaceRevision() {
        return workspaceRevision;
    }
}
