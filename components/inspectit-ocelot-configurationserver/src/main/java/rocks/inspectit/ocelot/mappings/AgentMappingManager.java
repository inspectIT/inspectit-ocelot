package rocks.inspectit.ocelot.mappings;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The manager class to handle and manage the agent mappings.
 */
@Component
@Slf4j
public class AgentMappingManager {

    /**
     * The mapping which is used when no mappings file exists.
     */
    @VisibleForTesting
    static final AgentMapping DEFAULT_MAPPING = AgentMapping.builder()
            .name("Default Mapping")
            .source("/")
            .attribute("service", ".*")
            .build();

    /**
     * Object mapper utils.
     */
    @Autowired
    private AgentMappingSerializer serializer;

    /**
     * Object mapper utils.
     */
    @Autowired
    private FileManager fileManager;

    /**
     * Post construct. Initially reading the agent mappings if the mappings file exists.
     */
    @PostConstruct
    public void postConstruct() throws IOException {
        if (!fileManager.getWorkspaceRevision().agentMappingsExist()) {
            log.info("Generating default agent mappings");
            List<AgentMapping> defaultMappings = Collections.singletonList(DEFAULT_MAPPING);
            serializer.writeAgentMappings(defaultMappings, fileManager.getWorkingDirectory());
        }
    }

    /**
     * Returns a unmodifiable representation of the current agent mappings list.
     *
     * @return A list of {@link AgentMapping}
     */
    public List<AgentMapping> getAgentMappings() {
        return serializer.readAgentMappings(fileManager.getWorkspaceRevision());
    }

    /**
     * Returns the {@link AgentMapping} with the given name.
     *
     * @param mappingName the name of the mapping
     *
     * @return The mapping with the given name or an empty {@link Optional} in case no mapping exists with the given name
     */
    public Optional<AgentMapping> getAgentMapping(String mappingName) {
        checkArgument(!StringUtils.isEmpty(mappingName), "The mapping name should not be empty or null.");

        return getAgentMappings().stream()
                .filter(mapping -> mapping.getName().equals(mappingName))
                .findFirst();
    }

    /**
     * Sets the given list as new list of {@link AgentMapping}s and persists it into a file.
     *
     * @param newAgentMappings list of {@link AgentMapping}s
     *
     * @throws IOException In case of an error while persisting it into a file
     */
    public synchronized void setAgentMappings(List<AgentMapping> newAgentMappings) throws IOException {
        checkArgument(newAgentMappings != null, "The agent mappings should not be null.");

        log.info("Overriding current agent mappings with {} new mappings.", newAgentMappings.size());

        serializer.writeAgentMappings(newAgentMappings, fileManager.getWorkingDirectory());
    }

    /**
     * Deletes the {@link AgentMapping} with the given name and persists the changed list into a file.
     *
     * @param mappingName the name of the {@link AgentMapping} to delete
     *
     * @return Returns true if a mapping has been removed, otherwise false.
     *
     * @throws IOException In case of an error while persisting it into a file
     */
    public synchronized boolean deleteAgentMapping(String mappingName) throws IOException {
        checkArgument(!StringUtils.isEmpty(mappingName), "The mapping name should not be empty or null.");

        log.info("Deleting agent mapping '{}'.", mappingName);

        ArrayList<AgentMapping> newAgentMappings = new ArrayList<>(getAgentMappings());
        boolean removed = newAgentMappings.removeIf(mapping -> mapping.getName().equals(mappingName));
        if (removed) {
            setAgentMappings(newAgentMappings);
        }
        return removed;
    }

    /**
     * Adds a {@link AgentMapping} to the head of the current list of mappings. If an agent mapping exists which has the
     * same name as the given agent mapping, the existing one will be replaced.
     * The new list will be persisted into a file.
     *
     * @param agentMapping the {@link AgentMapping} to add
     *
     * @throws IOException In case of an error while persisting it into a file
     */
    public void addAgentMapping(AgentMapping agentMapping) throws IOException {
        checkArgument(agentMapping != null, "The agent mapping should not be null.");
        checkArgument(!StringUtils.isEmpty(agentMapping.getName()), "The agent mapping's name should not be null or empty.");

        log.info("Adding new agent mapping '{}'.", agentMapping.getName());

        addAgentMapping(getAgentMappings(), agentMapping, 0);
    }

    /**
     * Adds a {@link AgentMapping} before the agent mapping with the given name in the current list of mappings. If an agent mapping exists which has the
     * same name as the given agent mapping, the existing one will be removed. The new list will be persisted into a file.
     * Nothing happens if no agent mapping exists with the given name.
     *
     * @param agentMapping the {@link AgentMapping} to add
     * @param mappingName  the name of the mapping where the new mapping is added before
     *
     * @throws IOException      In case of an error while persisting it into a file
     * @throws RuntimeException If no mapping exists with the given name
     */
    public synchronized void addAgentMappingBefore(AgentMapping agentMapping, String mappingName) throws IOException {
        log.info("Adding new agent mapping '{}' before existing mapping '{}'.", agentMapping.getName(), mappingName);

        List<AgentMapping> currentMappings = getAgentMappings();
        OptionalInt indexOpt = getMappingIndex(mappingName, currentMappings);
        if (indexOpt.isPresent()) {
            addAgentMapping(currentMappings, agentMapping, indexOpt.getAsInt());
        } else {
            throw new IllegalArgumentException("The agent mapping has not been added because the mapping '" + mappingName + "' does not exists, thus, cannot be added before it.");
        }
    }

    /**
     * Adds a {@link AgentMapping} after the agent mapping with the given name in the current list of mappings. If an agent mapping exists which has the
     * same name as the given agent mapping, the existing one will be removed. The new list will be persisted into a file.
     * Nothing happens if no agent mapping exists with the given name.
     *
     * @param agentMapping the {@link AgentMapping} to add
     * @param mappingName  the name of the mapping where the new mapping is added after
     *
     * @throws IOException      In case of an error while persisting it into a file
     * @throws RuntimeException If no mapping exists with the given name
     */
    public synchronized void addAgentMappingAfter(AgentMapping agentMapping, String mappingName) throws IOException {
        log.info("Adding new agent mapping '{}' after existing mapping '{}'.", agentMapping.getName(), mappingName);

        List<AgentMapping> currentMappings = getAgentMappings();
        OptionalInt indexOpt = getMappingIndex(mappingName, currentMappings);
        if (indexOpt.isPresent()) {
            addAgentMapping(currentMappings, agentMapping, indexOpt.getAsInt() + 1);
        } else {
            throw new IllegalArgumentException("The agent mapping has not been added because the mapping '" + mappingName + "' does not exists, thus, cannot be added after it.");
        }
    }

    /**
     * Returns the index of the agent mapping with the given name.
     */
    private OptionalInt getMappingIndex(String mappingName, List<AgentMapping> mappings) {
        return IntStream.range(0, mappings.size())
                .filter(i -> mappingName.equals(mappings.get(i).getName()))
                .findFirst();
    }

    /**
     * Adds a agent mapping at the specified index. An existing mapping will be removed if it has the same name as the given one.
     */
    private void addAgentMapping(List<AgentMapping> currentMappings, AgentMapping agentMapping, int index) throws IOException {
        ArrayList<AgentMapping> newAgentMappings = new ArrayList<>(currentMappings);

        OptionalInt currentIndexOpt = getMappingIndex(agentMapping.getName(), newAgentMappings);

        if (currentIndexOpt.isPresent()) {
            int currentIndex = currentIndexOpt.getAsInt();
            if (index > currentIndex) {
                index = index - 1;
            }
            newAgentMappings.remove(currentIndex);
        }

        if (index > newAgentMappings.size()) {
            newAgentMappings.add(agentMapping);
        } else {
            newAgentMappings.add(index, agentMapping);
        }

        setAgentMappings(newAgentMappings);
    }
}
