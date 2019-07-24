package rocks.inspectit.ocelot.agentstatus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Holds a history of when agents last fetched their configuration.
 * This is useful for detecting which agents are actively connected.
 */
@Component
public class AgentStatusManager {

    @Autowired
    @VisibleForTesting
    InspectitServerSettings config;

    /**
     * Cache mapping attribute-maps to configurations.
     * This is a loading cache which has all mappings with their configurations in-memory.
     * If the mappings or any configuration file change, this cache is replaced with a new one.
     */
    private Cache<Map<String, String>, AgentStatus> attributesToConfigurationCache;

    /**
     * Clears the connection history.
     */
    @PostConstruct
    public void reset() {
        attributesToConfigurationCache = CacheBuilder
                .newBuilder()
                .maximumSize(config.getMaxAgents())
                .build();
    }

    /**
     * Called to update the history when an agent just fetched a configuration.
     *
     * @param agentAttributes     the attributes sent by the agent when fetching the configuration
     * @param resultConfiguration the configuration sent to the agent, can be null if no matching mapping exists.
     */
    public void notifyAgentConfigurationFetched(Map<String, String> agentAttributes, AgentConfiguration resultConfiguration) {
        AgentStatus newStatus = AgentStatus.builder()
                .attributes(agentAttributes)
                .lastConfigFetch(new Date())
                .mappingName(resultConfiguration == null ? null : resultConfiguration.getMapping().getName())
                .build();
        attributesToConfigurationCache.put(agentAttributes, newStatus);
    }

    /**
     * @return a collection of all agent statuses since {@link #reset()} was called.
     */
    public Collection<AgentStatus> getAgentStatuses() {
        return attributesToConfigurationCache.asMap().values();
    }
}
