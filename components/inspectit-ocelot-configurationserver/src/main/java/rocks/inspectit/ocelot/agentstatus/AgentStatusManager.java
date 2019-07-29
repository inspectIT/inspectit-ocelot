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
import java.util.concurrent.TimeUnit;

/**
 * Holds a history when agents last fetched their configuration.
 * This is useful for detecting which agents are active.
 */
@Component
public class AgentStatusManager {

    @Autowired
    @VisibleForTesting
    InspectitServerSettings config;

    /**
     * Cache storing the most recent status for each agent based on its attributes.
     * This cache is limited in size and has an expiration based on {@link #config}
     */
    private Cache<Map<String, String>, AgentStatus> attributesToAgentStatusCache;

    /**
     * Clears the connection history.
     */
    @PostConstruct
    public void reset() {
        attributesToAgentStatusCache = CacheBuilder
                .newBuilder()
                .maximumSize(config.getMaxAgents())
                .expireAfterWrite(config.getAgentEvictionDelay().toMillis(), TimeUnit.MILLISECONDS)
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
        attributesToAgentStatusCache.put(agentAttributes, newStatus);
    }

    /**
     * @return a collection of all agent statuses since {@link #reset()} was called.
     */
    public Collection<AgentStatus> getAgentStatuses() {
        return attributesToAgentStatusCache.asMap().values();
    }
}
