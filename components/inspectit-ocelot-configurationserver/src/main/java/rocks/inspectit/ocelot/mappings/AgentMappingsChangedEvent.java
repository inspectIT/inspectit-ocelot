package rocks.inspectit.ocelot.mappings;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when the mappings managed by the {@link AgentMappingManager} have changed in any way.
 */
public class AgentMappingsChangedEvent extends ApplicationEvent {

    public AgentMappingsChangedEvent(Object source) {
        super(source);
    }
}
