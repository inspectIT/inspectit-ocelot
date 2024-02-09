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
    public static AgentConfiguration NO_MATCHING_MAPPING = createDefault();

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
     * The set of suppliers for documentable objects. <br>
     */
    private static final Set<Supplier<AgentDocumentation>> documentationSuppliers = Collections.synchronizedSet(new HashSet<>());

    /**
     * The set of documentable objects. <br>
     * There is a set of defined documentable objects in this configuration for each file.
     */
    private Set<AgentDocumentation> documentations;

    /**
     * The merged YAML configuration for the given mapping.
     */
    private String configYaml;

    /**
     * Cryptographic hash for {@link #configYaml}.
     */
    private String hash;

    private AgentConfiguration(AgentMapping mapping, String configYaml, String hash) {
        this.mapping = mapping;
        this.documentations = new HashSet<>();
        this.configYaml = configYaml;
        this.hash = hash;
    }

    private AgentConfiguration(AgentMapping mapping, Set<AgentDocumentation> documentations, String configYaml, String hash) {
        this.mapping = mapping;
        this.documentations = documentations;
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
        return new AgentConfiguration(mapping, configYaml, hash);
    }

    /**
     * Factory method to create an AgentConfiguration.
     * Also creates a cryptographic hash.
     *
     * @param mapping The agent mapping for which this instance represents the loaded configuration
     * @param docs The set of agent documentation objects
     * @param configYaml The yaml string, which contains the configuration
     * @return Created AgentConfiguration
     */
    public static AgentConfiguration create(AgentMapping mapping, Set<AgentDocumentation> docs, String configYaml) {
        String hash = DigestUtils.md5DigestAsHex(configYaml.getBytes(Charset.defaultCharset()));
        return new AgentConfiguration(mapping, docs, configYaml, hash);
    }

    /**
     * Factory method to create a default AgentConfiguration.
     *
     * @return Created default AgentConfiguration
     */
    public static AgentConfiguration createDefault() {
        String configYaml = "";
        String hash = DigestUtils.md5DigestAsHex(configYaml.getBytes(Charset.defaultCharset()));
        return new AgentConfiguration(null, new HashSet<>(), configYaml, hash);
    }

    /**
     * Use the suppliers to create the agent documentations for this configuration.
     * The suppliers are cleared after they all have been used.
     */
    public synchronized void supplyDocumentations() {
        documentationSuppliers.forEach(supplier -> documentations.add(supplier.get()));
        documentationSuppliers.clear();
    }

    /**
     * Get the current agent documentations as a map. <br>
     * - Key: the file path <br>
     * - Value: the set of objects, like actions, scopes, rules & metrics
     *
     * @return The agent documentations as a map
     */
    public Map<String, Set<String>> getDocumentationsAsMap() {
        return documentations.stream()
                .collect(Collectors.toMap(
                        AgentDocumentation::filePath,
                        AgentDocumentation::objects
                ));
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

            Supplier<AgentDocumentation> documentationSupplier = () -> loadDocsObjects(path, src);
            documentationSuppliers.add(documentationSupplier);
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
    private static AgentDocumentation loadDocsObjects(String filePath, String src) {
        Set<String> objects = Collections.emptySet();
        try {
            objects = DocsObjectsLoader.loadObjects(src);
        } catch (Exception e) {
            log.warn("Could not parse configuration: {}", filePath, e);
        }
        return new AgentDocumentation(filePath, objects);
    }
}
