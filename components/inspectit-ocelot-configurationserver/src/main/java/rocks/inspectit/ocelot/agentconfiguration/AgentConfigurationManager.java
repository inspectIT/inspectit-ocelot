package rocks.inspectit.ocelot.agentconfiguration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.FileChangedEvent;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.mappings.AgentMappingManager;
import rocks.inspectit.ocelot.mappings.AgentMappingsChangedEvent;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Manager responsible for serving the agent configuration based on the set of {@link AgentMapping}s.
 */
@Component
@Slf4j
public class AgentConfigurationManager {

    /**
     * Used as maker in {@link #attributesToConfigurationCache} to mark attribute-maps for which no mapping matches.
     */
    private static final AgentConfiguration NO_MATCHING_MAPPING = new AgentConfiguration(null, "");

    @Autowired
    @VisibleForTesting
    InspectitServerSettings config;

    @Autowired
    private AgentMappingManager mappingManager;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private FileManager fileManager;

    /**
     * Cache mapping attribute-maps to configurations.
     * This is a loading cache which has all mappings with their configurations in-memory.
     * If the mappings or any configuration file change, this cache is replaced with a new one.
     */
    private LoadingCache<Map<String, String>, AgentConfiguration> attributesToConfigurationCache;

    /**
     * Active task used for reloading the configuration asynchronously.
     */
    private AgentConfigurationReloadTask reloadTask;

    @PostConstruct
    @VisibleForTesting
    void init() {
        replaceConfigurations(Collections.emptyList());
        triggerConfigurationReloading();
    }

    @EventListener({FileChangedEvent.class, AgentMappingsChangedEvent.class})
    private synchronized void triggerConfigurationReloading() {
        if (reloadTask != null) {
            reloadTask.cancel();
        }
        reloadTask = new AgentConfigurationReloadTask(mappingManager.getAgentMappings(), fileManager, this::replaceConfigurations);
        executorService.submit(reloadTask);
    }

    /**
     * Fetches the configuration as yaml string given a set of attributes describing the target agent.
     *
     * @param agentAttributes the attributes of the agent for which the configuration shall be queried
     * @return the configuration for this agent or null if the attributes match no mapping
     */
    public AgentConfiguration getConfiguration(Map<String, String> agentAttributes) {
        AgentConfiguration config = attributesToConfigurationCache.getUnchecked(agentAttributes);
        return config == NO_MATCHING_MAPPING ? null : config;
    }

    /**
     * Replaces {@link #attributesToConfigurationCache} with a new cache which is backed by the given list of configurations.
     * The order of the list is used as priority, e.g. configurations coming first have a higher priority.
     *
     * @param newConfigurations the new ordered list of configurations
     */
    private synchronized void replaceConfigurations(List<AgentConfiguration> newConfigurations) {
        attributesToConfigurationCache = CacheBuilder.newBuilder()
                .maximumSize(config.getMaxAgents())
                .build(new CacheLoader<Map<String, String>, AgentConfiguration>() {
                    @Override
                    public AgentConfiguration load(Map<String, String> agentAttributes) {
                        return newConfigurations.stream()
                                .filter(config -> config.getMapping().matchesAttributes(agentAttributes))
                                .findFirst()
                                .orElse(NO_MATCHING_MAPPING);
                    }
                });
    }

}
