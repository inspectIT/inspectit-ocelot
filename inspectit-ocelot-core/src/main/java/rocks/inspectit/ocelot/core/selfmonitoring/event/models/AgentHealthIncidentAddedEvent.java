package rocks.inspectit.ocelot.core.selfmonitoring.event.models;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthIncident;
import rocks.inspectit.ocelot.core.utils.AgentHealthIncidentBuffer;

import java.util.List;

/**
 * Fired by {@link AgentHealthIncidentBuffer} whenever a new incident has been added.
 */
public class AgentHealthIncidentAddedEvent extends ApplicationEvent {

    @Getter
    private final List<AgentHealthIncident> currentIncidents;

    public AgentHealthIncidentAddedEvent(Object source, List<AgentHealthIncident> currentIncidents) {
        super(source);
        this.currentIncidents = currentIncidents;
    }
}
