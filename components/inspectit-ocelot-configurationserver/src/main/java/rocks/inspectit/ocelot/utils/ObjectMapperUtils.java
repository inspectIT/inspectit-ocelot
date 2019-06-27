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

@Component
public class ObjectMapperUtils {

    private ObjectMapper ymlMapper;

    private CollectionType mappingsListType;

    @PostConstruct
    public void postConstruct() {
        ymlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        ymlMapper.findAndRegisterModules();

        mappingsListType = ymlMapper.getTypeFactory().constructCollectionType(List.class, AgentMapping.class);
    }

    public List<AgentMapping> readAgentMappings(File mappingsFile) throws IOException {
        return ymlMapper.readValue(mappingsFile, mappingsListType);
    }

    public void writeAgentMappings(List<AgentMapping> agentMappings, File mappingsFile) throws IOException {
        ymlMapper.writeValue(mappingsFile, agentMappings);
    }
}
