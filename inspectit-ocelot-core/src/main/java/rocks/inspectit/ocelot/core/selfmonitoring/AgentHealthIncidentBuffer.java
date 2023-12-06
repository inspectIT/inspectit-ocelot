package rocks.inspectit.ocelot.core.selfmonitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthIncident;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthIncidentAddedEvent;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Buffer with queued AgentHealthIncidents.
 * New incidents will be inserted at the beginning of the queue.
 * As soon as incidents are put into a full queue, old incidents will be removed to create space
 */
@Component
public class AgentHealthIncidentBuffer {

    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private InspectitEnvironment env;

    private final ConcurrentLinkedQueue<AgentHealthIncident> buffer = new ConcurrentLinkedQueue<>();

    /**
     * Add new incident to the buffer.
     * If the buffer is full, remove the latest incident at first.
     * The buffer size will be read from the current inspectIT configuration.
     * @param incident new incident
     */
    public void put(AgentHealthIncident incident) {
        int bufferSize = env.getCurrentConfig().getSelfMonitoring().getAgentHealth().getIncidentBufferSize();
        while(buffer.size() >= bufferSize) buffer.poll();

        buffer.offer(incident);
        ctx.publishEvent(new AgentHealthIncidentAddedEvent(this, asList()));
    }

    /**
     * Creates a list from the internal queue.
     * The list will be reversed, since the queue inserts new elements at the tail
     * @return List of agent health incidents
     */
    public List<AgentHealthIncident> asList() {
        List<AgentHealthIncident> incidentList = new LinkedList<>(buffer);
        Collections.reverse(incidentList);
        return incidentList;
    }

    public void clear() {
        buffer.clear();
    }
}
