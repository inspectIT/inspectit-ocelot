package rocks.inspectit.ocelot.agentconfig;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.mappings.AgentMappingManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manager responsible for serving the agent configuration based on the set of {@link AgentMapping}s.
 */
@Component
public class AgentConfigurationManager {

    private static final Predicate<String> HAS_YAML_ENDING = filePath -> filePath.toLowerCase().endsWith(".yml") || filePath.toLowerCase().endsWith(".yaml");

    @Autowired
    private AgentMappingManager mappingManager;

    @Autowired
    private FileManager fileManager;


    /**
     * Fetches the configuration as yaml string given a set of attributes describing the target agent.
     *
     * @param agentAttributes the attributes of the agent for which the configuration shall be queried
     * @return the YAML configuration as a string or null if the attributes match no mapping
     */
    public String getConfiguration(Map<String, String> agentAttributes) throws IOException {
        //TODO: add a (limited size) cache mapping the agentAttributes to the resulting configuration, as this avoids looping over all mappings
        for (AgentMapping mapping : mappingManager.getAgentMappings()) {
            if (doAttributesMatchMapping(agentAttributes, mapping)) {
                return loadConfigForMapping(mapping);
            }
        }
        return null;
    }

    /**
     * Loads the given mapping as yaml string.
     *
     * @param mapping the mapping to load
     * @return the merged yaml for the given mapping or an empty string if the mapping does not contain any existing files
     * @throws IOException in case a file could not be loaded
     */
    @VisibleForTesting
    String loadConfigForMapping(AgentMapping mapping) throws IOException {
        //TODO: instead of reloading the mapping when it is requested, reload it once the files or the mappings change and cache it
        Object result = null;
        for (String path : mapping.getSources()) {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (fileManager.doesPathExist(path)) {
                if (fileManager.isDirectory(path)) {
                    List<String> paths = fileManager.getFilesInDirectory(path).stream()
                            .filter(f -> f.getType() == FileInfo.Type.FILE)
                            .map(FileInfo::getPath)
                            .filter(HAS_YAML_ENDING)
                            .sorted()
                            .collect(Collectors.toList());
                    for (String file : paths) {
                        result = loadAndMergeYaml(result, file);
                    }
                } else if (HAS_YAML_ENDING.test(path)) {
                    result = loadAndMergeYaml(result, path);
                }
            }
        }
        if (result == null) {
            return "";
        } else {
            return new Yaml().dump(result);
        }
    }

    /**
     * Loads a yaml file as a Map/List strucutre and merges it with an existing map/list structure
     *
     * @param toMerge the existing structure of nested maps / lists with which the loaded yaml will be merged.
     * @param path    the path of the yaml file to load
     * @return the merged structure
     * @throws IOException in case an error occurs while loading the file
     */
    private Object loadAndMergeYaml(Object toMerge, String path) throws IOException {
        Yaml yaml = new Yaml();
        String src = fileManager.readFile(path);
        Object loaded = yaml.load(src);
        if (toMerge == null) {
            return loaded;
        } else {
            return ObjectStructureMerger.merge(toMerge, loaded);
        }
    }

    /**
     * Checks if an Agent with a given map of attributes and their values fulfills the requirements of a given mapping.
     *
     * @param agentAttributes the attributes to check
     * @param mapping         the mapping to check against
     * @return true, if the mapping matches
     */
    private boolean doAttributesMatchMapping(Map<String, String> agentAttributes, AgentMapping mapping) {
        for (Map.Entry<String, String> pair : mapping.getAttributes().entrySet()) {
            String value = agentAttributes.getOrDefault(pair.getKey(), "");
            if (!value.matches(pair.getValue())) {
                return false;
            }
        }
        return true;
    }
}
