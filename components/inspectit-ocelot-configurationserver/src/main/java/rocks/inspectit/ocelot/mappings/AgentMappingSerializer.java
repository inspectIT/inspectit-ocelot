package rocks.inspectit.ocelot.mappings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.events.AgentMappingsSourceBranchChangedEvent;
import rocks.inspectit.ocelot.events.WorkspaceChangedEvent;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.versioning.Branch;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME;
import static rocks.inspectit.ocelot.mappings.AgentMappingManager.DEFAULT_MAPPING;

/**
 * Utility for reading and writing the Agent Mappings.
 */
@Component
@Slf4j
public class AgentMappingSerializer {

    private ObjectMapper ymlMapper;

    private CollectionType mappingsListType;

    private FileManager fileManager;

    /**
     * Current agent mappings from workspace, which are cached to avoid long processing time
     */
    private volatile List<AgentMapping> currentMappings;

    private ApplicationEventPublisher publisher;

    /**
     * SourceBranch for the agent mapping file itself. This does not affect the SourceBranch property inside the agent mappings!
     */
    @Getter
    private Branch sourceBranch;

    @VisibleForTesting
    @Autowired
    AgentMappingSerializer(InspectitServerSettings settings, FileManager fileManager, ApplicationEventPublisher publisher) {
        this.fileManager = fileManager;
        this.publisher = publisher;
        String initialBranch = settings.getInitialAgentMappingsSourceBranch().toUpperCase();
        this.sourceBranch = Branch.valueOf(initialBranch);
    }

    /**
     * Post construct for initializing the mapper objects.
     * Additionally, initially reading the agent mappings if the mappings file exists.
     */
    @PostConstruct
    public void postConstruct() throws IOException {
        ymlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        ymlMapper.findAndRegisterModules();

        mappingsListType = ymlMapper.getTypeFactory().constructCollectionType(List.class, AgentMapping.class);

        if (!fileManager.getWorkspaceRevision().agentMappingsExist()) {
            log.info("Generating default agent mappings for workspace branch");
            List<AgentMapping> defaultMappings = Collections.singletonList(DEFAULT_MAPPING);
            writeAgentMappings(defaultMappings);
        }
        else currentMappings = readAgentMappings(fileManager.getWorkspaceRevision());
    }

    /**
     * Read cached agent mappings to avoid long ymlMapper-processing time.
     *
     * @return List of current {@link AgentMapping}s representing the content of the given file
     */
    public List<AgentMapping> readCachedAgentMappings(){
        if(currentMappings != null) return currentMappings;
        else return readAgentMappings(fileManager.getWorkspaceRevision());
    }

    /**
     * Reload cached agent mappings, if any external changes have been detected in the workspace.
     */
    @EventListener(WorkspaceChangedEvent.class)
    public synchronized void reloadCachedAgentMappings() {
        log.info("Reloading cached agent mappings");
        currentMappings = readAgentMappings(fileManager.getWorkspaceRevision());
    }

    /**
     * Reading a list of {@link AgentMapping}s from a Yaml file.
     *
     * @param readAccess the accessor to use for reading the file
     *
     * @return List of {@link AgentMapping}s representing the content of the given file or an empty List in case of an error.
     */
    public List<AgentMapping> readAgentMappings(AbstractFileAccessor readAccess) {
        Optional<String> mappingsFile = readAccess.readAgentMappings();
        if (mappingsFile.isPresent()) {
            try {
                return ymlMapper.readValue(mappingsFile.get(), mappingsListType);
            } catch (Exception e) {
                log.error("Error decoding Agent Mappings", e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Writing the given list of {@link AgentMapping}s as a Yaml representation into the specified file.
     *
     * @param agentMappings the {@link AgentMapping}s to write to file
     *
     * @throws IOException if any error occurs, e.g. file cannot be written
     */
    public void writeAgentMappings(List<AgentMapping> agentMappings) throws IOException {
        currentMappings = agentMappings;
        AbstractWorkingDirectoryAccessor fileAccess = fileManager.getWorkingDirectory();
        fileAccess.writeAgentMappings(ymlMapper.writeValueAsString(agentMappings));
    }

    /**
     * Sets the source branch, from which the agent mappings file will be read
     *
     * @param sourceBranch new source branch
     *
     * @return the set source branch
     */
    public Branch setSourceBranch(Branch sourceBranch) {
        log.info("Setting source branch for {} to {}", AGENT_MAPPINGS_FILE_NAME, sourceBranch);
        Branch oldBranch = this.sourceBranch;
        this.sourceBranch = sourceBranch;

        RevisionAccess currentRevisionAccess = getRevisionAccess();
        if(currentRevisionAccess.agentMappingsExist()) {
            // Publish event to trigger configuration reload
            publisher.publishEvent(new AgentMappingsSourceBranchChangedEvent(this));
        }
        else {
            log.error("Source branch for {} cannot be set to {}, since no file was found", AGENT_MAPPINGS_FILE_NAME, sourceBranch);
            this.sourceBranch = oldBranch;
        }
        return this.sourceBranch;
    }

    /**
     * @return The branch, which is currently used to access the agent mappings
     */
    public RevisionAccess getRevisionAccess() {
        switch (getSourceBranch()) {
            case LIVE:
                return fileManager.getLiveRevision();
            case WORKSPACE:
                return fileManager.getWorkspaceRevision();
            default:
                throw new UnsupportedOperationException("Unhandled branch: " + getSourceBranch());
        }
    }
}
