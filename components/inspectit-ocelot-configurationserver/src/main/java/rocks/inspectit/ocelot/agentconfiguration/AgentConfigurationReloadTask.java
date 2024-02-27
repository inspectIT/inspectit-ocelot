package rocks.inspectit.ocelot.agentconfiguration;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.mappings.AgentMappingSerializer;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;
import rocks.inspectit.ocelot.utils.CancellableTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A task for asynchronously loading the configurations based on a given list of mappings.
 */
@Slf4j
class AgentConfigurationReloadTask extends CancellableTask<List<AgentConfiguration>> {

    private FileManager fileManager;

    private AgentMappingSerializer mappingsSerializer;

    /**
     * Creates a new reload task, but does NOT start it.
     * The loading process is done in {@link #run()}.
     *
     * @param mappingsSerializer the serializer responsible for extracting the mappings from the current revision
     * @param fileManager        the FileManager used to read the configuration files
     * @param onLoadCallback     invoked when the loading has finished successfully. Will not be invoked if the loading failed or was canceled.
     */
    public AgentConfigurationReloadTask(AgentMappingSerializer mappingsSerializer, FileManager fileManager, Consumer<List<AgentConfiguration>> onLoadCallback) {
        super(onLoadCallback);
        this.mappingsSerializer = mappingsSerializer;
        this.fileManager = fileManager;
    }

    /**
     * Performs the actual loading, should only be invoked once.
     */
    @Override
    public void run() {
        log.info("Starting configuration reloading...");
        RevisionAccess fileAccess = mappingsSerializer.getRevisionAccess();

        if (!fileAccess.agentMappingsExist()) {
            log.error("No agent mappings file was found on the current branch! Please add '{}' to the current branch.",
                    AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            onTaskSuccess(Collections.emptyList());
            return;
        }
        List<AgentMapping> mappingsToLoad = mappingsSerializer.readAgentMappings(fileAccess);
        List<AgentConfiguration> newConfigurations = new ArrayList<>();
        for (AgentMapping mapping : mappingsToLoad) {
            if(mapping == null) {
                log.debug("Could not load null agent mapping");
                continue;
            }
            try {
                if (isCanceled()) {
                    log.debug("Configuration reloading canceled");
                    return;
                }

                AbstractFileAccessor fileAccessor = getFileAccessorForMapping(mapping);
                if(fileAccessor == null) {
                    log.debug("No file accessor provided for mapping {}. Cannot read files", mapping);
                    continue;
                }

                AgentConfiguration agentConfiguration = AgentConfiguration.create(mapping, fileAccessor);
                newConfigurations.add(agentConfiguration);
            } catch (Exception e) {
                log.error("Could not load agent mapping '{}'.", mapping.name(), e);
            }
        }

        onTaskSuccess(newConfigurations);
    }

    /**
     * Returns the file accessor with regard to the source branch of the agent mapping.
     *
     * @param mapping the agent mapping with source branch
     *
     * @return the file accessor for the mapping source branch
     */
    private AbstractFileAccessor getFileAccessorForMapping(AgentMapping mapping) {
        return switch (mapping.sourceBranch()) {
            case LIVE -> fileManager.getLiveRevision();
            case WORKSPACE -> fileManager.getWorkspaceRevision();
        };
    }
}
