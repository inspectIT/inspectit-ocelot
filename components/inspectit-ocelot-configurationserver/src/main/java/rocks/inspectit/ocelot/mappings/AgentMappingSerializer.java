package rocks.inspectit.ocelot.mappings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Utility for reading and writing the Agent Mappings.
 */
@Component
@Slf4j
public class AgentMappingSerializer {

    private ObjectMapper ymlMapper;

    private CollectionType mappingsListType;

    /**
     * Post construct for initializing the mapper objects.
     */
    @PostConstruct
    public void postConstruct() {
        ymlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        ymlMapper.findAndRegisterModules();

        mappingsListType = ymlMapper.getTypeFactory().constructCollectionType(List.class, AgentMapping.class);
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
}
