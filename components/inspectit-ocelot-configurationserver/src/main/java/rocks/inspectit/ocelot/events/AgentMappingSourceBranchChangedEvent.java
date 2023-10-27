package rocks.inspectit.ocelot.events;

import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.versioning.Branch;

/**
 * This event is fired when the source branch for the agent mappings file itself has changed.
 */
public class AgentMappingSourceBranchChangedEvent extends ApplicationEvent {

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public AgentMappingSourceBranchChangedEvent(Object source) {
        super(source);

    }
}
