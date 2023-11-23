package rocks.inspectit.ocelot.core.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthIncident;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Buffer with queued AgentHealthIncidents.
 * New incidents will be inserted at the beginning of the queue.
 * As soon as incidents are put into a full queue, old incidents will be removed to create space
 */
@Component
@RequiredArgsConstructor
public class AgentHealthIncidentBuffer {
    private final ConcurrentLinkedQueue<AgentHealthIncident> buffer = new ConcurrentLinkedQueue<>();

    private final InspectitEnvironment env;

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
}
