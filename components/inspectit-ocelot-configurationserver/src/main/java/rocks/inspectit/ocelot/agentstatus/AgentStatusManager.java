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
     * Cache storing the most recent status for each agent or client. The key represents the agent id or a set of attributes
     * in case the configuration was not fetched by an agent (in case no agent id is specified).
     * This cache is limited in size and has an expiration based on {@link #config}.
     */
    private Cache<Object, AgentStatus> attributesToAgentStatusCache;

    /**
     * Clears the connection history.
     */
    @PostConstruct
    public void reset() {
        attributesToAgentStatusCache = CacheBuilder.newBuilder()
                .maximumSize(config.getMaxAgents())
                .expireAfterWrite(config.getAgentEvictionDelay().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Called to update the history when an agent just fetched a configuration.
     *
     * @param agentAttributes     the attributes sent by the agent when fetching the configuration
     * @param headers             the headers sent by the client fetching the configuration
     * @param resultConfiguration the configuration sent to the agent, can be null if no matching mapping exists.
     */
    public void notifyAgentConfigurationFetched(Map<String, String> agentAttributes, Map<String, String> headers, AgentConfiguration resultConfiguration) {
        AgentMetaInformation metaInformation = AgentMetaInformation.of(headers);

        AgentStatus agentStatus = AgentStatus.builder()
                .metaInformation(metaInformation)
                .attributes(agentAttributes)
                .lastConfigFetch(new Date())
                .mappingName(resultConfiguration == null ? null : resultConfiguration.getMapping().getName())
                .sourceBranch(resultConfiguration == null ? null : resultConfiguration.getMapping()
                        .getSourceBranch()
                        .getBranchName())
                .build();

        Object statusKey;
        if (metaInformation != null) {
            statusKey = metaInformation.getAgentId();
        } else {
            statusKey = agentAttributes;
        }

        attributesToAgentStatusCache.put(statusKey, agentStatus);
    }

    /**
     * @return a collection of all agent statuses since {@link #reset()} was called.
     */
    public Collection<AgentStatus> getAgentStatuses() {
        return attributesToAgentStatusCache.asMap().values();
    }
}
