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

/**
 * The manager class to handle and manage the agent mappings.
 */
@Component
@Slf4j
public class AgentMappingManager {

    /**
     * The name of the agent mappings Yaml file.
     */
    private static final String AGENT_MAPPINGS_FILE = "agent_mappings.yaml";

    /**
     * Lock for manipulation of the agent mappings.
     */
    private static final Object mappingLock = new Object();

    /**
     * Object mapper utils.
     */
    @Autowired
    private ObjectMapperUtils objectMapperUtils;

    /**
     * The agent mappings Yaml file.
     */
    private File mappingsFile;

    /**
     * The currently used agent mappings. This should be in sync with the content of the {@link #mappingsFile}.
     */
    private List<AgentMapping> agentMappings;

    /**
     * The working directory which is used to store the {@link #AGENT_MAPPINGS_FILE}.
     */
    @Value("${inspectit.workingDirectory}")
    @VisibleForTesting
    String workingDirectory;

    /**
     * Post construct. Initially reading the agent mappings if the mappings file exists.
     */
    @PostConstruct
    public void postConstruct() {
        log.debug("Loading existing agent mappings.");

        mappingsFile = new File(workingDirectory, AGENT_MAPPINGS_FILE);

        readAgentMappingsFromFile();
    }

    /**
     * Reading existing agent mappings from the mappings file.
     */
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

    /**
     * Writes the given list of {@link AgentMapping}s into the {@link #mappingsFile}.
     *
     * @param mappings the mappings to write
     * @throws IOException In case of an error
     */
    private void writeAgentMappingsToFile(List<AgentMapping> mappings) throws IOException {
        log.debug("Writing agent mappings to file: {}", mappingsFile);
        objectMapperUtils.writeAgentMappings(mappings, mappingsFile);
    }

    /**
     * Returns a copy of the current agent mappings list.
     *
     * @return A list of {@link AgentMapping}
     */
    public List<AgentMapping> getAgentMappings() {
        return agentMappings.stream()
                .map(mapping -> mapping.toBuilder().build())
                .collect(Collectors.toList());
    }

    /**
     * Returns the {@link AgentMapping} with the given name.
     *
     * @param mappingName the name of the mapping
     * @return The mapping with the given name or an empty {@link Optional} in case no mapping exists with the given name
     */
    public Optional<AgentMapping> getAgentMapping(String mappingName) {
        checkArgument(!StringUtils.isEmpty(mappingName), "The mapping name should not be empty or null.");

        return agentMappings.stream()
                .filter(mapping -> mapping.getName().equals(mappingName))
                .map(mapping -> mapping.toBuilder().build())
                .findFirst();
    }

    /**
     * Sets the given list as new list of {@link AgentMapping}s and persists it into a file.
     *
     * @param newAgentMappings list of {@link AgentMapping}s
     * @throws IOException In case of an error while persisting it into a file
     */
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

    /**
     * Deletes the {@link AgentMapping} with the given name and persists the changed list into a file.
     *
     * @param mappingName the name of the {@link AgentMapping} to delete
     * @return Returns true if a mapping has been removed, otherwise false.
     * @throws IOException In case of an error while persisting it into a file
     */
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

    /**
     * Adds a {@link AgentMapping} to the head of the current list of mappings. If an agent mapping exists which has the
     * same name as the given agent mapping, the existing one will be removed.
     * The new list will be persisted into a file.
     *
     * @param agentMapping the {@link AgentMapping} to add
     * @throws IOException In case of an error while persisting it into a file
     */
    public void addAgentMapping(AgentMapping agentMapping) throws IOException {
        checkArgument(agentMapping != null, "The agent mapping should not be null.");
        checkArgument(!StringUtils.isEmpty(agentMapping.getName()), "The agent mapping's name should not be null or empty.");

        log.info("Adding new agent mapping '{}'.", agentMapping.getName());
        synchronized (mappingLock) {
            addAgentMapping(agentMapping, 0);
        }
    }

    /**
     * Adds a {@link AgentMapping} before the agent mapping with the given name in the current list of mappings. If an agent mapping exists which has the
     * same name as the given agent mapping, the existing one will be removed. The new list will be persisted into a file.
     * Nothing happens if no agent mapping exists with the given name.
     *
     * @param agentMapping the {@link AgentMapping} to add
     * @param mappingName  the name of the mapping where the new mapping is added before
     * @throws IOException      In case of an error while persisting it into a file
     * @throws RuntimeException If no mapping exists with the given name
     */
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

    /**
     * Adds a {@link AgentMapping} after the agent mapping with the given name in the current list of mappings. If an agent mapping exists which has the
     * same name as the given agent mapping, the existing one will be removed. The new list will be persisted into a file.
     * Nothing happens if no agent mapping exists with the given name.
     *
     * @param agentMapping the {@link AgentMapping} to add
     * @param mappingName  the name of the mapping where the new mapping is added after
     * @throws IOException      In case of an error while persisting it into a file
     * @throws RuntimeException If no mapping exists with the given name
     */
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

    /**
     * Returns the index of the agent mapping with the given name.
     */
    private OptionalInt getMappingIndex(String mappingName) {
        return IntStream.range(0, agentMappings.size())
                .filter(i -> mappingName.equals(agentMappings.get(i).getName()))
                .findFirst();
    }

    /**
     * Adds a agent mapping at the specified index. An existing mapping will be removed if it has the same name as the given one.
     */
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
