package rocks.inspectit.ocelot.events;

import org.springframework.context.ApplicationEvent;

/**
 * This event is fired when the source branch for the agent mappings file itself has changed.
 */
public class AgentMappingsSourceBranchChangedEvent extends ApplicationEvent {

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public AgentMappingsSourceBranchChangedEvent(Object source) {
        super(source);
    }
}
