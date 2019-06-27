package rocks.inspectit.ocelot.mappings;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;
import rocks.inspectit.ocelot.utils.ObjectMapperUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;

@Component
@Slf4j
public class AgentMappingManager {

    private static final String AGENT_MAPPINGS_FILE = "agent_mappings.yaml";

    private static final Object mappingLock = new Object();

    @Autowired
    private ObjectMapperUtils objectMapperUtils;

    private File mappingsFile;

    private List<AgentMapping> agentMappings;

    @Value("${inspectit.workingDirectory}")
    @VisibleForTesting
    String workingDirectory;

    @PostConstruct
    public void postConstruct() {
        log.debug("Loading existing agent mappings.");

        mappingsFile = new File(workingDirectory, AGENT_MAPPINGS_FILE);

        readAgentMappingsFromFile();
    }

    private void readAgentMappingsFromFile() {
        if (mappingsFile.exists()) {
            try {
                agentMappings = objectMapperUtils.readAgentMappings(mappingsFile);
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

    private void writeAgentMappingsToFile(List<AgentMapping> mappings) throws IOException {
        log.debug("Writing agent mappings to file: {}", mappingsFile);
        objectMapperUtils.writeAgentMappings(mappings, mappingsFile);
    }

    public List<AgentMapping> getAgentMappings() {
        return agentMappings;
    }

    public Optional<AgentMapping> getAgentMapping(String mappingName) {
        checkArgument(!StringUtils.isEmpty(mappingName), "The mapping name should not be empty or null.");

        return agentMappings.stream()
                .filter(mapping -> mapping.getName().equals(mappingName))
                .findFirst();
    }

    public synchronized void setAgentMappings(List<AgentMapping> newAgentMappings) throws IOException {
        checkArgument(newAgentMappings != null, "The agent mappings should not be null.");

        List<AgentMapping> copiedMappings = newAgentMappings.stream()
                .map(mapping -> mapping.toBuilder().build())
                .collect(Collectors.toList());

        log.info("Overriding current agent mappings with {} new mappings.", copiedMappings.size());
        synchronized (mappingLock) {
            writeAgentMappingsToFile(copiedMappings);
            agentMappings = copiedMappings;
        }
    }

    public boolean deleteAgentMapping(String mappingName) throws IOException {
        checkArgument(!StringUtils.isEmpty(mappingName), "The mapping name should not be empty or null.");

        log.info("Deleting agent mapping '{}'.", mappingName);
        synchronized (mappingLock) {
            ArrayList<AgentMapping> newAgentMappings = new ArrayList<>(agentMappings);
            boolean removed = newAgentMappings.removeIf(mapping -> mapping.getName().equals(mappingName));
            if (removed) {
                writeAgentMappingsToFile(newAgentMappings);
                agentMappings = newAgentMappings;
            }
            return removed;
        }
    }

    public void addAgentMapping(AgentMapping agentMapping) throws IOException {
        checkArgument(agentMapping != null, "The agent mapping should not be null.");
        checkArgument(!StringUtils.isEmpty(agentMapping.getName()), "The agent mapping's name should not be null or empty.");

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
                throw new RuntimeException("The agent mapping has not been added because the mapping '" + mappingName + "' does not exists, thus, cannot be added before it.");
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
                throw new RuntimeException("The agent mapping has not been added because the mapping '" + mappingName + "' does not exists, thus, cannot be added after it.");
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
            ArrayList<AgentMapping> newAgentMappings = new ArrayList<>(agentMappings);

            AgentMapping copiedAgentMapping = agentMapping.toBuilder().build();

            OptionalInt currentIndexOpt = getMappingIndex(copiedAgentMapping.getName());

            if (currentIndexOpt.isPresent()) {
                int currentIndex = currentIndexOpt.getAsInt();
                if (index > currentIndex) {
                    index = index - 1;
                }
                newAgentMappings.remove(currentIndex);
            }

            if (index > newAgentMappings.size()) {
                newAgentMappings.add(copiedAgentMapping);
            } else {
                newAgentMappings.add(index, copiedAgentMapping);
            }

            writeAgentMappingsToFile(newAgentMappings);
            agentMappings = newAgentMappings;
        }
    }
}
