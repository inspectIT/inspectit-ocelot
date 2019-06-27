package rocks.inspectit.ocelot.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility for reading and writing objects as a Yaml represented string into files.
 */
@Component
public class ObjectMapperUtils {

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
     * @param mappingsFile the Yaml file to read
     * @return List of {@link AgentMapping}s representing the content of the given file.
     * @throws IOException if any error occurs, e.g. invalid Yaml
     */
    public List<AgentMapping> readAgentMappings(File mappingsFile) throws IOException {
        return ymlMapper.readValue(mappingsFile, mappingsListType);
    }

    /**
     * Writing the given list of {@link AgentMapping}s as a Yaml representation into the specified file.
     *
     * @param agentMappings the {@link AgentMapping}s to write to file
     * @param mappingsFile  the target file
     * @throws IOException if any error occurs, e.g. file cannot be written
     */
    public void writeAgentMappings(List<AgentMapping> agentMappings, File mappingsFile) throws IOException {
        ymlMapper.writeValue(mappingsFile, agentMappings);
    }
}
