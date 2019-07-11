package rocks.inspectit.ocelot.agentconfiguration;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.mappings.AgentMappingManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manager responsible for serving the agent configuration based on the set of {@link AgentMapping}s.
 */
@Component
public class AgentConfigurationManager {

    /**
     * Predicate for detecting if a given file ends with .yml or .yaml.
     * Not case sensitive.
     */
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
            if (mapping.matchesAttributes(agentAttributes)) {
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
        LinkedHashSet<String> allYamlFiles = new LinkedHashSet<>();
        for (String path : mapping.getSources()) {
            allYamlFiles.addAll(getAllYamlFiles(path));
        }

        if (allYamlFiles.isEmpty()) {
            return "";
        } else {
            Object result = null;
            for (String path : allYamlFiles) {
                result = loadAndMergeYaml(result, path);
            }
            return new Yaml().dump(result);
        }
    }

    /**
     * If the given path is a yaml file, a list containing only it is returned.
     * If the path is a directory, the absolute path of all contained yaml files is returned in alphabetical order.
     * If it is neither, an empty list is returned.
     *
     * @param path the path to check for yaml files, can start with a slash which will be ignored
     * @return a list of absolute paths of contained YAML files
     */
    private List<String> getAllYamlFiles(String path) throws IOException {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (fileManager.exists(path)) {
            if (fileManager.isDirectory(path)) {
                return fileManager.getFilesInDirectory(path).stream()
                        .filter(f -> f.getType() == FileInfo.Type.FILE)
                        .map(FileInfo::getPath)
                        .filter(HAS_YAML_ENDING)
                        .sorted()
                        .collect(Collectors.toList());
            } else if (HAS_YAML_ENDING.test(path)) {
                return Collections.singletonList(path);
            }
        }
        return Collections.emptyList();
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
        Object loadedYaml = yaml.load(src);
        if (toMerge == null) {
            return loadedYaml;
        } else {
            return ObjectStructureMerger.merge(toMerge, loadedYaml);
        }
    }

}
