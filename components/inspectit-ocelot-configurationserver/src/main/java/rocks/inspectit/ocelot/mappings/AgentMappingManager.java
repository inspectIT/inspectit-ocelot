package rocks.inspectit.ocelot.mappings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

@Component
@Slf4j
public class AgentMappingManager {

    private static final String AGENT_MAPPINGS_FILE = "agent_mappings.yaml";

    private static final Object mappingLock = new Object();

    private ObjectMapper ymlMapper;

    private CollectionType agentMappingListType;

    private File mappingsFile;

    private List<AgentMapping> agentMappings;

    @Value("${inspectit.workingDirectory}")
    @VisibleForTesting
    String workingDirectory;

    @PostConstruct
    public void postConstruct() {
        log.debug("Loading existing agent mappings.");

        mappingsFile = new File(workingDirectory, AGENT_MAPPINGS_FILE);

        initObjectMapper();
        readAgentMappingsFromFile();
    }

    private void initObjectMapper() {
        ymlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        ymlMapper.findAndRegisterModules();

        agentMappingListType = ymlMapper.getTypeFactory().constructCollectionType(List.class, AgentMapping.class);
    }

    private void readAgentMappingsFromFile() {
        if (mappingsFile.exists()) {
            try {
                agentMappings = ymlMapper.readValue(mappingsFile, agentMappingListType);
                log.debug("Successfully loaded agent mappings.");
            } catch (IOException e) {
                log.error("Could not load agent mappings from file.", e);
                agentMappings = Collections.emptyList();
            }
        } else {
            log.info("No agent mappings have been loaded - agent mappings file has not been found: {}", mappingsFile.getAbsolutePath());
            agentMappings = Collections.emptyList();
        }
    }

    private void writeAgentMappingsToFile() throws IOException {
        log.debug("Writing agent mappings to file: {}", mappingsFile);
        ymlMapper.writeValue(mappingsFile, agentMappings);
    }

    public List<AgentMapping> getAgentMappings() {
        return agentMappings;
    }

    public Optional<AgentMapping> getAgentMapping(String mappingName) {
        return agentMappings.stream()
                .filter(mapping -> mapping.getName().equals(mappingName))
                .findFirst();
    }

    public synchronized void setAgentMappings(List<AgentMapping> newAgentMappings) throws IOException {
        log.info("Overriding current agent mappings with {} new mappings.", newAgentMappings.size());
        synchronized (mappingLock) {
            agentMappings = newAgentMappings;
            writeAgentMappingsToFile();
        }
    }

    public boolean deleteAgentMapping(String mappingName) throws IOException {
        log.info("Deleting agent mapping '{}'.", mappingName);
        synchronized (mappingLock) {
            ArrayList<AgentMapping> currentMappings = new ArrayList<>(this.agentMappings);
            boolean removed = agentMappings.removeIf(mapping -> mapping.getName().equals(mappingName));
            if (removed) {
                try {
                    writeAgentMappingsToFile();
                } catch (IOException ex) {
                    agentMappings = currentMappings;
                    throw ex;
                }
            }
            return removed;
        }
    }

    public void addAgentMapping(AgentMapping agentMapping) throws IOException {
        log.info("Adding new agent mapping '{}'.", agentMapping.getName());
        synchronized (mappingLock) {
            addAgentMapping(agentMapping, 0);
        }
    }

    public void addAgentMappingBefore(AgentMapping agentMapping, String mappingName) throws IOException {
        log.info("Adding new agent mapping '{}' before existing mapping '{}'.", agentMapping.getName(), mappingName);
        synchronized (mappingLock) {
            OptionalInt indexOpt = getMappingIndex(mappingName);
            if (indexOpt.isPresent()) {
                addAgentMapping(agentMapping, indexOpt.getAsInt());
            } else {
                throw new IllegalArgumentException("The agent mapping has not been added because the mapping '" + mappingName + "' does not exists, thus, cannot be added before it.");
            }
        }
    }

    public void addAgentMappingAfter(AgentMapping agentMapping, String mappingName) throws IOException {
        log.info("Adding new agent mapping '{}' after existing mapping '{}'.", agentMapping.getName(), mappingName);
        synchronized (mappingLock) {
            OptionalInt indexOpt = getMappingIndex(mappingName);
            if (indexOpt.isPresent()) {
                addAgentMapping(agentMapping, indexOpt.getAsInt() + 1);
            } else {
                throw new IllegalArgumentException("The agent mapping has not been added because the mapping '" + mappingName + "' does not exists, thus, cannot be added after it.");
            }
        }
    }

    private OptionalInt getMappingIndex(String mappingName) {
        return IntStream.range(0, agentMappings.size())
                .filter(i -> mappingName.equals(agentMappings.get(i).getName()))
                .findFirst();
    }

    private void addAgentMapping(AgentMapping agentMapping, int index) throws IOException {
        synchronized (mappingLock) {
            ArrayList<AgentMapping> currentMappings = new ArrayList<>(this.agentMappings);

            OptionalInt currentIndexOpt = getMappingIndex(agentMapping.getName());

            if (currentIndexOpt.isPresent()) {
                int currentIndex = currentIndexOpt.getAsInt();
                if (index > currentIndex) {
                    index = index - 1;
                }
                agentMappings.remove(currentIndex);
            }

            if (index > agentMappings.size()) {
                agentMappings.add(agentMapping);
            } else {
                agentMappings.add(index, agentMapping);
            }
            try {
                writeAgentMappingsToFile();
            } catch (IOException ex) {
                agentMappings = currentMappings;
                throw ex;
            }
        }
    }
}
