package rocks.inspectit.ocelot.mappings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.events.AgentMappingsSourceBranchChangedEvent;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.versioning.Branch;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME;
import static rocks.inspectit.ocelot.file.versioning.Branch.LIVE;

/**
 * Utility for reading and writing the Agent Mappings.
 */
@Component
@Slf4j
public class AgentMappingSerializer {

    private ObjectMapper ymlMapper;

    private CollectionType mappingsListType;

    private FileManager fileManager;

    private ApplicationEventPublisher publisher;

    /**
     * SourceBranch for the agent mapping file itself. This does not affect the SourceBranch property inside the agent mappings!
     */
    @Getter
    private Branch sourceBranch;

    @VisibleForTesting
    @Autowired
    AgentMappingSerializer(FileManager fileManager, ApplicationEventPublisher publisher) {
        this.fileManager = fileManager;
        this.publisher = publisher;
    }

    /**
     * Post construct for initializing the mapper objects.
     */
    @PostConstruct
    public void postConstruct() {
        ymlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        ymlMapper.findAndRegisterModules();

        mappingsListType = ymlMapper.getTypeFactory().constructCollectionType(List.class, AgentMapping.class);

        sourceBranch = LIVE; // TODO: Soll der source Branch bei jedem Start initial pauschal auf LIVE gesetzt werden?
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
     * @param fileAccess    the accessor to which the mappings will be written
     *
     * @throws IOException if any error occurs, e.g. file cannot be written
     */
    public void writeAgentMappings(List<AgentMapping> agentMappings, AbstractWorkingDirectoryAccessor fileAccess) throws IOException {
        fileAccess.writeAgentMappings(ymlMapper.writeValueAsString(agentMappings));
    }

    /**
     * Sets the source branch, from which the agent mappings file will be read
     * @param sourceBranch new source branch
     */
    public void setSourceBranch(Branch sourceBranch) {
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
