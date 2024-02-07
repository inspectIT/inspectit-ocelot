package rocks.inspectit.ocelot.agentconfiguration;

import com.google.common.annotations.VisibleForTesting;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An {@link AgentMapping} which has its configuration loaded in-memory.
 * In addition, a cryptographic hash is computed to detect changes of configurations.
 */
@Value
@Slf4j
public class AgentConfiguration {

    /**
     * Used as maker in {@link #attributesToConfigurationCache} to mark attribute-maps for which no mapping matches.
     */
    public static final AgentConfiguration NO_MATCHING_MAPPING = createDefaultConfiguration();

    /**
     * Predicate to check if a given file path ends with .yml or .yaml
     */
    private static final Predicate<String> HAS_YAML_ENDING = filePath -> filePath.toLowerCase()
            .endsWith(".yml") || filePath.toLowerCase().endsWith(".yaml");

    /**
     * The agent mapping for which this instance represents the loaded configuration.
     */
    private AgentMapping mapping;

    /**
     * The set of defined documentable objects in this configuration for each file. <br>
     * The map might be initialized after constructing the AgentConfiguration. <br>
     * - Key: the file path <br>
     * - Value: the set of objects, like actions, scopes, rules & metrics
     */
    private Map<String, Set<String>> docsObjectsByFile;

    /**
     * The set of suppliers for the defined documentable objects in this configuration for each file
     */
    private static final Map<String, Supplier<Set<String>>> docsObjectsByFileSuppliers = new HashMap<>();

    /**
     * The merged YAML configuration for the given mapping.
     */
    private String configYaml;

    /**
     * Cryptographic hash for {@link #configYaml}.
     */
    private String hash;

    private AgentConfiguration(AgentMapping mapping, Map<String, Set<String>> docsObjectsByFile, String configYaml, String hash) {
        this.mapping = mapping;
        this.docsObjectsByFile = docsObjectsByFile;
        this.configYaml = configYaml;
        this.hash = hash;
    }

    /**
     * Factory method to create AgentConfigurations.
     * Also creates a cryptographic hash.
     *
     * @param mapping The agent mapping for which this instance represents the loaded configuration
     * @return Created AgentConfiguration
     */
    public static AgentConfiguration create(AgentMapping mapping, AbstractFileAccessor fileAccessor) {
        String configYaml = loadConfigForMapping(mapping, fileAccessor);
        String hash = DigestUtils.md5DigestAsHex(configYaml.getBytes(Charset.defaultCharset()));
        return new AgentConfiguration(mapping, new HashMap<>(), configYaml, hash);
    }

    /**
     * Factory method to create AgentConfigurations.
     * Also creates a cryptographic hash.
     *
     * @param mapping The agent mapping for which this instance represents the loaded configuration
     * @param configYaml The yaml string, which contains the configuration
     * @param docsObjectsByFile The set of defined documentable objects in this configuration for each file
     * @return Created AgentConfiguration
     */
    public static AgentConfiguration create(AgentMapping mapping, Map<String, Set<String>> docsObjectsByFile, String configYaml) {
        String hash = DigestUtils.md5DigestAsHex(configYaml.getBytes(Charset.defaultCharset()));
        return new AgentConfiguration(mapping, docsObjectsByFile, configYaml, hash);
    }

    /**
     * Apply suppliers to get the documentable objects by file and store them
     */
    public void supplyDocsObjectsByFile() {
        for (Map.Entry<String, Supplier<Set<String>>> entry : docsObjectsByFileSuppliers.entrySet()) {
            this.docsObjectsByFile.put(entry.getKey(), entry.getValue().get());
        }
    }

    /**
     * Factory method to create default AgentConfiguration.
     *
     * @return Created default AgentConfiguration
     */
    private static AgentConfiguration createDefaultConfiguration() {
        String configYaml = "";
        String hash = DigestUtils.md5DigestAsHex(configYaml.getBytes(Charset.defaultCharset()));
        return new AgentConfiguration(null, new HashMap<>(), configYaml, hash);
    }

    /**
     * Loads the given mapping as yaml string.
     *
     * @param mapping the mapping to load
     *
     * @return the merged yaml for the given mapping or an empty string if the mapping does not contain any existing files
     * If this task has been canceled, null is returned.
     */
    @VisibleForTesting
    static String loadConfigForMapping(AgentMapping mapping, AbstractFileAccessor fileAccessor) {
        LinkedHashSet<String> allYamlFiles = getAllYamlFilesForMapping(fileAccessor, mapping);

        Object result = null;
        for (String path : allYamlFiles) {
            String src = fileAccessor.readConfigurationFile(path).orElse("");
            result = ObjectStructureMerger.loadAndMergeYaml(src, result, path);

            Supplier<Set<String>> docsObjectsSupplier = () -> loadDocsObjects(src, path);
            docsObjectsByFileSuppliers.put(path, docsObjectsSupplier);
        }
        return result == null ? "" : new Yaml().dump(result);
    }

    /**
     * Returns the set of yaml files, which is defined in the agent mapping sources.
     *
     * @param fileAccessor the accessor to use for reading the file
     * @param mapping the mapping, which contains a list of source file paths
     *
     * @return the set of yaml file paths for the provided mapping
     * If this task has been canceled, null is returned.
     */
    private static LinkedHashSet<String> getAllYamlFilesForMapping(AbstractFileAccessor fileAccessor, AgentMapping mapping) {
        LinkedHashSet<String> allYamlFiles = new LinkedHashSet<>();
        for (String path : mapping.sources()) {
            List<String> yamlFiles = getAllYamlFiles(fileAccessor, path);
            allYamlFiles.addAll(yamlFiles);
        }
        return allYamlFiles;
    }

    /**
     * If the given path is a yaml file, a list containing only it is returned.
     * If the path is a directory, the absolute path of all contained yaml files is returned in alphabetical order.
     * If it is neither, an empty list is returned.
     *
     * @param path the path to check for yaml files, can start with a slash which will be ignored
     *
     * @return a list of absolute paths of contained YAML files
     */
    private static List<String> getAllYamlFiles(AbstractFileAccessor fileAccessor, String path) {
        String cleanedPath;
        if (path.startsWith("/")) {
            cleanedPath = path.substring(1);
        } else {
            cleanedPath = path;
        }

        if (fileAccessor.configurationFileExists(cleanedPath)) {
            if (fileAccessor.configurationFileIsDirectory(cleanedPath)) {
                List<FileInfo> fileInfos = fileAccessor.listConfigurationFiles(cleanedPath);

                return fileInfos.stream()
                        .flatMap(file -> file.getAbsoluteFilePaths(cleanedPath))
                        .filter(HAS_YAML_ENDING)
                        .sorted()
                        .collect(Collectors.toList());
            } else if (HAS_YAML_ENDING.test(cleanedPath)) {
                return Collections.singletonList(cleanedPath);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Loads all documentable objects of the yaml source string.
     *
     * @param src the yaml string
     * @param filePath the path to the yaml file
     *
     * @return the set of documentable objects
     */
    private static Set<String> loadDocsObjects(String src, String filePath) {
        Set<String> objects = Collections.emptySet();
        try {
            objects = DocsObjectsLoader.loadObjects(src);
        } catch (Exception e) {
            log.warn("Could not parse configuration: {}", filePath, e);
        }
        return objects;
    }
}
